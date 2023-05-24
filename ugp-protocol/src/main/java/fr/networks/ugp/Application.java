package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;
import fr.networks.ugp.utils.Client;
import fr.networks.ugp.utils.CommandParser;
import fr.networks.ugp.utils.Helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    private static final Logger logger = Logger.getLogger(Application.class.getName());

    // For the sever part
    private final ServerSocketChannel serverSocketChannel;

    // For the client part
    private final InetSocketAddress serverAddress;
    private final Selector selector;
    private final SocketChannel sc;
    private final Thread console;
    private final Path resultDirectory;
    private final Object lock = new Object();

    private final BlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<Task> tasksQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<Result> resultsQueue = new ArrayBlockingQueue<>(10);
    private final ArrayList<Context> children = new ArrayList<>();
    private final HashMap<TaskId, CapacityManager> capacityTable = new HashMap<>();
    private final HashMap<TaskId, TaskHandler> taskTable = new HashMap<>();
    private final HashMap<TaskId, LaunchedTask> launchedTasks = new HashMap<>();
    private HashMap<InetSocketAddress, Context> reconnected = new HashMap<>();
    private Context parentContext;
    private Context disconnectingContext;
    private boolean isAvailable = true;
    private long localTaskId = 0;

    private final ArrayList<Context> waitingToDisconnect = new ArrayList<>(); // List of children wanting to disconnect
    private DisconnectionHandler disconnectionHandler;

    public Application(InetSocketAddress serverAddress, int port, Path directory) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        resultDirectory = directory;

        if (serverAddress != null) {
            sc = SocketChannel.open();
            this.serverAddress = serverAddress;
        } else {
            sc = null;
            this.serverAddress = null;
        }
        this.console = Thread.ofPlatform().unstarted(this::consoleRun);
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        if(serverAddress != null) {
            connectToParent(serverAddress);
        }
        disconnectionHandler = new DisconnectionHandler(parentContext, selector, capacityTable, taskTable); // TODO Change place this line
        console.start();

        while (!Thread.interrupted()) {
            Helpers.printKeys(selector); // for debug
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
                processCommands();
                processResult();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
            System.out.println("Select finished");
        }
    }

    private void treatKey(SelectionKey key) {
        Helpers.printSelectedKey(key); // for debug
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
        try {
            if (key.isValid() && key.isConnectable()) {
                ((Context) key.attachment()).doConnect(); // Replaced UniqueContext
            }
            if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", e);
            silentlyClose(key);
        }
    }

    public void handleCapacityRequest(Context receiveFrom, CapacityRequest capacityRequest) {
        System.out.println("Received capacity request");
        System.out.println("Received: " + capacityRequest);
        if (hasNeighborsExceptEmitter()) {
            var capacityHandler = new CapacityManager(receiveFrom, neighborsNumber() - 1);
            capacityTable.put(capacityRequest.taskId(), capacityHandler);
            sendToNeighborsExceptOne(receiveFrom, capacityRequest);
        } else {
            System.out.println("Respond");
            receiveFrom.queueMessage(new Capacity(capacityRequest.taskId(), 1));
        }
    }

    public void handleCapacity(Context receiveFrom, Capacity capacity) {
        System.out.println("Received capacity response");
        System.out.println("Received: " + capacity);

        var id = capacity.id();
        var capacityHandler = capacityTable.get(id);
        var state = capacityHandler.handleCapacity(capacity, receiveFrom);

        if (state == CapacityManager.State.RECEIVED_ALL) {
            handleReceivedAllCapacity(id, capacityHandler);
        }
    }

    public void handleTask(Context receivedFrom, Task task) {
        System.out.println("Received task request");
        System.out.println("Received: " + task);
        var id = task.id();

        if (isTaskAccepted()) {
            handleAcceptedTask(receivedFrom, task, id);
        } else {
            handleRefusedTask(receivedFrom, task, id);
        }
    }

    public void handleTaskAccepted(Context receiveFrom, TaskAccepted taskAccepted) {
        System.out.println(receiveFrom + " accepted the task " + taskAccepted.id());
        var taskHandle = taskTable.get(taskAccepted.id());
        taskHandle.addTaskDestination(receiveFrom);
    }

    public void handleTaskRefused(Context receiveFrom, TaskRefused taskRefused) {
        System.out.println(receiveFrom + " refused the task " + taskRefused.id() + " with range : " + taskRefused.range());
        var refusedTask = launchedTasks.get(taskRefused.id());
        // TODO launch the sub task ourself
    }

    public void handleResult(Context receiveFrom, Result result) {
        System.out.println("Received a task result");
        System.out.println("Received: " + result);

        var taskHandler = taskTable.get(result.id());

        if (!isAvailable) {
            taskHandler.storeResult(result);
            return;
        }

        if (isTaskOrigin(taskHandler)) {
            // TODO write the res in the file [a private method]
            writeResultToFile(result);
        } else {
            taskHandler.emitter().queueMessage(result);
        }

        if(!taskHandler.receivedTaskResult(receiveFrom)) {
            // Attend 2 result et il en recoit 3
            return;
        };

        // Free resource
        capacityTable.remove(result.id());
        taskTable.remove(result.id());
        launchedTasks.remove(result.id());
    }

    public void handleLeavingNotification(Context receiveFrom, LeavingNotification leavingNotification) {
        if(isAvailable) {
            isAvailable = false;
            disconnectionHandler.wantToDisconnect(receiveFrom);
        } else {
            waitingToDisconnect.add(receiveFrom);
        }
    }

    public void handleNotifyChild(Context receiveFrom, NotifyChild notifyChild) {
        // disconnectionHandler.receivedNotifyChild();
        cancelMyTasks();

        // Then send "NEW_PARENT" to children
        var parentAddress = parentContext.getRemoteAddress();
        disconnectionHandler.receivedNotifyChild(parentAddress, children);
    }

    public void handleCancelTask(Context receiveFrom, CancelTask cancelTask) {
        // Iterate over the tasks in the task table
        for (TaskId taskId : taskTable.keySet()) {
            var destinations = taskTable.get(taskId).destinations();
            // Check if the application is in the task applications list
            if (destinations.contains(receiveFrom)) {
                // Stop the task and remove it from the task table
                // TODO: stop the task
                taskTable.remove(taskId);
            }
        }
    }

    public void handlePartialResult(Context receiveFrom, PartialResult partialResult) {

    }

    public void handleAllSent(Context receiveFrom, AllSent allSent) {
        receiveFrom.silentlyClose();
        isAvailable = true;
        if(!waitingToDisconnect.isEmpty()) {
            // TODO launch new disconnection
        }
    }

    public void handleNewParent(Context receiveFrom, NewParent newParent) {
        System.out.println("Received a newParent : " + newParent);
        System.out.println("Not available from now on");
        isAvailable = false;
        System.out.println("Then reconnect to parent!!");

        try {
            connectToParent(newParent.newParent());
            parentContext.queueMessage(new Reconnect());
            disconnectingContext = receiveFrom;
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        // Close the current connection and open a new one to parent
        // receiveFrom.silentlyClose();
    }

    public void handleNewParentOK(Context receiveFrom, NewParentOK newParentOK) {
        // Start to send Partial Result to Parent

    }

    public void handleResumeTask(Context receiveFrom, ResumeTask resumeTask) {

    }

    public void handleAllowDeconnection(Context receiveFrom, AllowDeconnection allowDeconnection) {

    }

    public void handleReconnect(Context receiveFrom, Reconnect reconnect) {
        reconnected.put(receiveFrom.getRemoteAddress(), receiveFrom);
        receiveFrom.queueMessage(new ReconnectOK());
    }

    public void handleReconnectOK(Context receiveFrom, ReconnectOK reconnectOK) {
        for (var taskHandler : taskTable.values()) {
            if (taskHandler.emitter().equals(disconnectingContext)) {
                taskHandler.updateEmitter(disconnectingContext);
            }
        }

        disconnectingContext.queueMessage(new NewParentOK());
    }


    // ===========[THREADS]

    private void consoleRun() {
        try {
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    var command = scanner.nextLine(); // <== Parser la commande ici
                    sendCommand(command);
                }
            }
            logger.info("Console thread stopping");
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        }
    }

    private void launchTaskInBackground() {
        synchronized (lock) {
            System.out.println("Beginning launch the task");
            var curTask = tasksQueue.poll();
            System.out.println("Start downloading...");
            var checker = Client.checkerFromHTTP(curTask.url().toString(), curTask.className()).orElseThrow();
            System.out.println("Checker downloaded");

            var resultString = new StringJoiner("\n");
            System.out.println("Launch the task");
            for (long i = curTask.range().from(); i < curTask.range().to(); i++) {
                try {
                    resultString.add(checker.check(i));
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            System.out.println("Finish the task");

            var result = new Result(curTask.id(), resultString.toString());
            resultsQueue.add(result);
            System.out.println("Wakeup selector");
            selector.wakeup();
        }
    }

    // ==========[COMMANDS]

    private void sendCommand(String command) throws InterruptedException {
        synchronized (lock) {
            commandQueue.add(command);
            selector.wakeup();
        }
    }

    private void processCommands() throws IOException {
        synchronized (lock) {
            var msg = commandQueue.poll();
            if(msg == null) {
                return;
            }
            var command = CommandParser.parseCommand(msg);
            if(command.isEmpty()) {
                return;
            }
            switch (command.get()) {
                case CommandParser.Start start -> {
                    var taskId = new TaskId(localTaskId, ((InetSocketAddress) serverSocketChannel.getLocalAddress()));
                    var url = new URL(start.urlJar());
                    var range = new Range(start.startRange(), start.endRange());
                    var filePath = resultDirectory.resolve(start.filename());

                    //Files.createFile(filePath);
                    var launchedTask = new LaunchedTask(new Task(taskId, url, start.fullyQualifiedName(), range), filePath);
                    launchedTasks.put(taskId, launchedTask);
                    localTaskId++;

                    capacityTable.put(taskId, new CapacityManager(null, neighborsNumber()));
                    broadcast(new CapacityRequest(taskId));
                }
                case CommandParser.Disconnect disconnect -> {
                    if(isAvailable) {
                        isAvailable = false;
                        disconnectionHandler.startingDisconnection();
                    } else {
                        System.out.println("A deconnection is already in progress please retry after");
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + command);
            }
        }
    }

    // ===========[TASK & RESULTS]
    private boolean isTaskOrigin(TaskHandler taskHandler) {
        return taskHandler.emitter() == null;
    }

    private boolean isTaskAccepted() {
        // TODO: Implement the logic to determine if the task is accepted
        return true; // Placeholder value, replace with actual implementation
    }

    private void handleReceivedAllCapacity(TaskId id, CapacityManager capacityManager) {
        System.out.println("Received all capacity.");
        System.out.println("Launch the task");

        var taskLaunched = launchedTasks.get(id);
        var task = taskLaunched.task();
        var taskCapacityTable = capacityManager.getTaskCapacityTable();
        int totalCapacity = capacityManager.capacitySum() + 1;

        // TODO: [2] Mettre le bon compteur
        var taskHandler = new TaskHandler(task, totalCapacity, null);
        taskTable.put(id, taskHandler);

        distributeTask(task, taskCapacityTable, totalCapacity);
    }

    private void handleAcceptedTask(Context receivedFrom, Task task, TaskId id) {
        System.out.println("Let the parent know that we accepted");
        receivedFrom.queueMessage(new TaskAccepted(id)); // Send task accepted

        var capacityHandler = capacityTable.get(id);

        if (capacityHandler == null) {
            handleNoNeighbors(task, id, receivedFrom);
        } else {
            handleWithNeighbors(task, id, receivedFrom, capacityHandler);
        }
    }

    private void handleNoNeighbors(Task task, TaskId id, Context receivedFrom) {
        var taskHandler = new TaskHandler(task, 0, receivedFrom);
        taskTable.put(id, taskHandler);
        System.out.println("No neighbors, launch the task ourselves");
        tasksQueue.add(task);
        Thread.ofPlatform().start(this::launchTaskInBackground);
    }

    private void handleWithNeighbors(Task task, TaskId id, Context receivedFrom, CapacityManager capacityManager) {
        var taskCapacityTable = capacityManager.getTaskCapacityTable();
        int totalCapacity = capacityManager.capacitySum() + 1;
        // TODO: [2] here to
        TaskHandler taskHandler = new TaskHandler(task, totalCapacity, receivedFrom);
        taskTable.put(id, taskHandler);
        distributeTask(task, taskCapacityTable, totalCapacity);
    }

    private void handleRefusedTask(Context receivedFrom, Task task, TaskId id) {
        receivedFrom.queueMessage(new TaskRefused(id, task.range())); // Send task refused
    }

    private void distributeTask(Task task, HashMap<Context, Integer> taskCapacityTable, int totalCapacity) {
        long range = task.range().diff();
        long unit = range / totalCapacity;
        long start = task.range().from();
        long limit = start + unit;

        var subTask = new Task(task.id(), task.url(), task.className(), new Range(start, limit));
        System.out.println("Add task[our part task] : " + subTask);
        // TODO: Launch our task
        tasksQueue.add(subTask);
        Thread.ofPlatform().start(this::launchTaskInBackground);

        // Send the rest to destinations
        for (var entry : taskCapacityTable.entrySet()) {
            start = limit;
            var capacity = entry.getValue();
            limit = start + unit * capacity;

            var neighborTask = new Task(task.id(), task.url(), task.className(), new Range(start, limit));
            System.out.println("Task sent to neighbor : " + neighborTask);
            var destination = entry.getKey();
            destination.queueMessage(neighborTask);
        }
    }

    private void processResult() {
        synchronized (lock) {
            var result = resultsQueue.poll();
            if (result == null) {
                return;
            }

            var taskId = result.id();
            var taskHandler = taskTable.get(taskId);
            var emitter = taskHandler.emitter();

            // TODO: Write to file
            if (emitter == null) {
                writeResultToFile(result);
            } else {
                System.out.println("Send to emitter");
                emitter.queueMessage(result);
            }

            if (taskHandler.receivedTaskResult(null)) {
                System.out.println("Free resources");
                capacityTable.remove(taskId);
                taskTable.remove(taskId);
            }
        }
    }

    private void cancelMyTasks() {
        taskTable.forEach((taskId, taskHandler) -> {
            if (isTaskOrigin(taskHandler)) {
                for (var dest : taskHandler.destinations()) {
                    dest.queueMessage(new CancelTask(taskId));
                }
            }
        });
    }

    private void writeResultToFile(Result result) {
        var taskId = result.id();
        var taskLaunched = launchedTasks.get(taskId);
        try {
            Files.write(taskLaunched.file(), Collections.singleton(result.result()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    // =============[PACKETS]

    private void connectToParent(InetSocketAddress address) throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        parentContext = new Context(this, key);
        key.attach(parentContext);
        sc.connect(address);
    }

    private void sendToNeighborsExceptOne(Context sendFrom, Packet packet) {
        selector.keys().stream()
                .filter(selectionKey -> !(selectionKey.channel() instanceof ServerSocketChannel))
                .map(selectionKey -> (Context) selectionKey.attachment())
                .filter(context -> context != sendFrom)
                .forEach(context -> context.queueMessage(packet));
    }

    private int neighborsNumber() {
        int keyCount = selector.keys().size();
        return keyCount - 1;
    }

    private boolean hasNeighborsExceptEmitter() {
        return neighborsNumber() - 1 >= 1;
    }

    private void doAccept(SelectionKey key) throws IOException {
        var client = serverSocketChannel.accept();
        if (client == null) {
            logger.warning("The selector give a bad hint");
            return; // selector gave a bad hint
        }
        client.configureBlocking(false);
        var clientKey = client.register(selector, SelectionKey.OP_READ);
        var child = new Context(this, clientKey);
        clientKey.attach(child);
        children.add(child);
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    /**
     * Add a message to all connected clients queue
     *
     * @param packet packet to broadcast
     */
    private void broadcast(Packet packet) {
        selector.keys().forEach(selectionKey -> {
            if (selectionKey.channel() instanceof ServerSocketChannel) {
                return;
            }
            ((Context) selectionKey.attachment()).queueMessage(packet);
        });
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 2 && args.length != 4) {
            usage();
            return;
        }

        if(args.length == 2) {
            int port = Integer.parseInt(args[0]);
            var resultsDirectory = Paths.get(args[1]);
            new Application(null, port, resultsDirectory).launch();
        } else {
            int port = Integer.parseInt(args[0]);
            var resultsDirectory = Paths.get(args[1]);
            var parentHost = args[2];
            int parentPort = Integer.parseInt(args[3]);
            new Application(new InetSocketAddress(parentHost, parentPort), port, resultsDirectory).launch();
        }
    }

    private static void usage() {
        System.out.println("Usage: Application port [results_directory] [parent_host] [parent_port]");
        System.out.println("  port: The port number for listening");
        System.out.println("  results_directory: The directory path for storing result files");
        System.out.println("  parent_host: The host address of the parent application (optional)");
        System.out.println("  parent_port: The port number of the parent application (optional)");
    }
}

// TEST LINE
// START http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar fr.uge.factors.Factorizer 1 200 ./res/text.txt