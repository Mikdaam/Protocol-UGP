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
    private final Object lock = new Object();


    private final BlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<Task> tasksQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<Result> resultsQueue = new ArrayBlockingQueue<>(10);
    private final ArrayList<Context> children = new ArrayList<>();
    private final HashMap<TaskId, CapacityHandler> capacityTable = new HashMap<>();
    private final HashMap<TaskId, TaskHandler> taskTable = new HashMap<>();
    private final HashMap<TaskId, Task> currentTasks = new HashMap<>();
    private boolean cantSend;
    private Context parentContext;
    private long taskCounter = 0;

    public Application(InetSocketAddress serverAddress, int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();

        if (serverAddress != null) {
            sc = SocketChannel.open();
            this.serverAddress = serverAddress;
        } else {
            sc = null;
            this.serverAddress = null;
        }
        this.console = Thread.ofPlatform().unstarted(this::consoleRun);
    }


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
            var resultString = new StringBuilder();

            System.out.println("Launch the task");
            for (long i = curTask.range().from(); i < curTask.range().to(); i++) {
                try {
                    resultString.append(checker.check(i));
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
                System.out.println("Result of task : " + result);
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
                    var taskId = new TaskId(taskCounter, ((InetSocketAddress) serverSocketChannel.getLocalAddress()));
                    var url = new URL(start.urlJar());
                    var range = new Range(start.startRange(), start.endRange());

                    currentTasks.put(taskId, new Task(taskId, url, start.fullyQualifiedName(), range));
                    taskCounter++;

                    capacityTable.put(taskId, new CapacityHandler(null, neighborsNumber()));
                    broadcast(new CapacityRequest(taskId));
                }
                case CommandParser.Disconnect disconnect -> {
                    //disconnectionHandler.startingDisconnection();
                }
                default -> throw new IllegalStateException("Unexpected value: " + command);
            }
        }
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        if(serverAddress != null) {
            connectToParent(serverAddress);
        }
        //disconnectionHandler = new DisconnectionHandler(parentContext, selector, capacityTable, taskTable); // TODO Change place this line
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

    private void connectToParent(InetSocketAddress address) throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        parentContext = new Context(this, key);
        key.attach(parentContext);
        sc.connect(address);
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

    private void distributeTask(Task task, HashMap<Context, Integer> taskCapacityTable, int totalCapacity) {
        // TODO: Should send to destinataires instead of capacity table context.
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
            /*if(emitter != null && emitter == context) {
                continue;
            }*/
            start = limit;
            var capacity = entry.getValue();
            limit = start + unit * capacity;

            var neighborTask = new Task(task.id(), task.url(), task.className(), new Range(start, limit));
            System.out.println("Task sent to neighbor : " + neighborTask);
            var destination = entry.getKey();
            destination.queueMessage(neighborTask);
        }
    }

    public void handleCapacityRequest(Context receiveFrom, CapacityRequest capacityRequest) {
        System.out.println("Received capacity request");
        System.out.println("Received: " + capacityRequest);
        if (hasNeighborsExceptEmitter()) {
            var capacityHandler = new CapacityHandler(receiveFrom, neighborsNumber() - 1);
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

        if (state == CapacityHandler.State.RECEIVED_ALL) {
            handleReceivedAllCapacity(id, capacityHandler);
        }
    }

    private void handleReceivedAllCapacity(TaskId id, CapacityHandler capacityHandler) {
        System.out.println("Received all capacity.");
        System.out.println("Launch the task");

        var task = currentTasks.get(id);
        var taskCapacityTable = capacityHandler.getTaskCapacityTable();
        int totalCapacity = capacityHandler.capacitySum() + 1;

        // TODO: [2] Mettre le bon compteur
        var taskHandler = new TaskHandler(task, totalCapacity, null);
        taskTable.put(id, taskHandler);

        distributeTask(task, taskCapacityTable, totalCapacity);
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

    private boolean isTaskAccepted() {
        // TODO: Implement the logic to determine if the task is accepted
        return true; // Placeholder value, replace with actual implementation
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

    private void handleWithNeighbors(Task task, TaskId id, Context receivedFrom, CapacityHandler capacityHandler) {
        var taskCapacityTable = capacityHandler.getTaskCapacityTable();
        int totalCapacity = capacityHandler.capacitySum();
        // TODO: [2] here to
        TaskHandler taskHandler = new TaskHandler(task, totalCapacity, receivedFrom);
        taskTable.put(id, taskHandler);

        currentTasks.put(id, task);
        distributeTask(task, taskCapacityTable, totalCapacity);
    }

    private void handleRefusedTask(Context receivedFrom, Task task, TaskId id) {
        receivedFrom.queueMessage(new TaskRefused(id, task.range())); // Send task refused
    }


    public void handleTaskAccepted(Context receiveFrom, TaskAccepted taskAccepted) {
        System.out.println(receiveFrom + " accepted the task " + taskAccepted.id());
        var taskHandle = taskTable.get(taskAccepted.id());
        taskHandle.addTaskDestination(receiveFrom);
    }

    public void handleTaskRefused(Context receiveFrom, TaskRefused taskRefused) {
        System.out.println(receiveFrom + " refused the task " + taskRefused.id() + " with range : " + taskRefused.range());
        var refusedTask = currentTasks.get(taskRefused.id());
        // TODO launch the sub task ourself
    }

    public void handleResult(Context receiveFrom, Result result) {
        System.out.println("Received a task result");
        System.out.println("Received: " + result);

        var taskHandler = taskTable.get(result.id());

        /*if (cantSend) {
            taskHandler.storeResult(result);
            return;
        }*/

        if (taskHandler.emitter() == null) {
            var res = taskHandler.taskResult();
            // TODO write the res in the file
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
        currentTasks.remove(result.id());
    }

    public void handleLeavingNotification(Context receiveFrom, LeavingNotification leavingNotification) {
        // disconnectionHandler.wantToDisconnect(receiveFrom);
        receiveFrom.queueMessage(new NotifyChild());
    }

    public void handleNotifyChild(Context receiveFrom, NotifyChild notifyChild) {
        // disconnectionHandler.receivedNotifyChild();
        cancelMyTasks();

        // TODO: Stop all the current task
        // TODO: get the partial result of task and send it to parent [PARTIAL_RESULT & ALL_SENT]

        // Then send "NEW_PARENT" to children
        for (var child : children) {
            try {
                var parentAddress = parentContext.getRemoteAddress();
                child.queueMessage(new NewParent(parentAddress));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    private void cancelMyTasks() {
        taskTable.forEach((taskId, taskHandler) -> {
            if (taskHandler.emitter() == null) {
                for (var dest : taskHandler.destinations()) {
                    dest.queueMessage(new CancelTask(taskId));
                }
            }
        });
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

    }

    public void handleNewParent(Context receiveFrom, NewParent newParent) {
        System.out.println("Received a newParent : " + newParent);
        System.out.println("Should close the connection and reconnect!!");
        // Close the current connection and open a new one to parent
        receiveFrom.silentlyClose();


        try {
            connectToParent(newParent.newParent());
            //parentContext.queueMessage(new Reconnect());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void handleResumeTask(Context receiveFrom, ResumeTask resumeTask) {

    }

    public void handleAllowDeconnection(Context receiveFrom, AllowDeconnection allowDeconnection) {

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
        if (args.length < 1) {
            usage();
            return;
        } else if(args.length == 1) {
            new Application(null, Integer.parseInt(args[0])).launch();
        } else {
            new Application(new InetSocketAddress(args[1], Integer.parseInt(args[2])), Integer.parseInt(args[0])).launch();
        }
    }

    private static void usage() {
        System.out.println("Usage : Application port <dest_hostname> <dest_port>");
    }
}

// TEST LINE
// START http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar fr.uge.factors.Factorizer 1 200 ./res/text.txt