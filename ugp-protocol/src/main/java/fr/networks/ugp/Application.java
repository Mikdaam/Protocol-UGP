package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;
import fr.networks.ugp.utils.CommandParser;
import fr.networks.ugp.utils.Helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.*;
import java.util.*;
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


    private final Queue<String> queue = new ArrayDeque<>();
    private final ArrayList<Context> children = new ArrayList<>();
    private final HashMap<TaskId, CapacityHandler> capacityTable = new HashMap<>();
    private final HashMap<TaskId, TaskHandler> taskTable = new HashMap<>();
    private final HashMap<TaskId, Task> currentTasks = new HashMap<>();
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

    private void sendCommand(String command) throws InterruptedException {
        synchronized (lock) {
            queue.add(command);
            selector.wakeup();
        }
    }

    private void processCommands() throws IOException {
        synchronized (lock) {
            var msg = queue.poll();
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
                    System.out.println();
                }
                default -> throw new IllegalStateException("Unexpected value: " + command);
            }
        }
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        if(serverAddress != null) {
            sc.configureBlocking(false);
            var key = sc.register(selector, SelectionKey.OP_CONNECT);
            key.attach(new Context(this, key));
            sc.connect(serverAddress);
        }

        console.start();

        while (!Thread.interrupted()) {
            Helpers.printKeys(selector); // for debug
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
                processCommands();
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

        var capacityHandler = capacityTable.get(capacity.id());
        var state = capacityHandler.handleCapacity(capacity, receiveFrom);

        if (state == CapacityHandler.State.RECEIVED_SUM) {
            var taskId = capacity.id();
            var taskHandler = new TaskHandler(currentTasks.get(taskId), capacityHandler, null);
            var subTask = taskHandler.distributeTask();
            //TODO launch the sub task ourself
        }
    }

    public void handleTask(Context receivedFrom, Task task) {
        System.out.println("Received task request");
        System.out.println("Received: " + task);

        var id = task.id();

        var capacityHandler = capacityTable.get(id);
        var taskHandle = new TaskHandler(task, capacityHandler, receivedFrom);

        if(capacityHandler != null) { // if accepted
            taskTable.put(id, taskHandle);
            currentTasks.put(id, task);
            var subTask = taskHandle.distributeTask();
            //TODO launch the sub task ourself
            taskHandle.sendTaskAccepted();
        } else {
            taskHandle.sendTaskRefused(task.range());
        }
    }

    public void handleTaskAccepted(Context receiveFrom, TaskAccepted taskAccepted) {

    }

    public void handleTaskRefused(Context receiveFrom, TaskRefused taskRefused) {

    }

    public void handleResult(Context receiveFrom, Result result) {

    }

    public void handleLeavingNotification(Context receiveFrom, LeavingNotification leavingNotification) {

    }

    public void handleNotifyChild(Context receiveFrom, NotifyChild notifyChild) {

    }

    public void handleCancelTask(Context receiveFrom, CancelTask cancelTask) {

    }

    public void handlePartialResult(Context receiveFrom, PartialResult partialResult) {

    }

    public void handleAllSent(Context receiveFrom, AllSent allSent) {

    }

    public void handleNewParent(Context receiveFrom, NewParent newParent) {

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
// START http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar fr.uge.factors.Factorizer 0 200 ./res/text.txt