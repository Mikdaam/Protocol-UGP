package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Capacity;
import fr.networks.ugp.packets.CapacityRequest;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.Task;
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

    public void sendCapacityRequest(CapacityRequest packet, Context emitter) {
        var capacityHandler = new CapacityHandler(emitter, neighborsNumber() - 1);
        capacityTable.put(packet.taskId(), capacityHandler);
        sendToNeighborsExceptOne(packet, emitter);
    }

    public boolean receiveCapacity(Capacity capacity, Context context) {
        var capacityHandler = capacityTable.get(capacity.id());
        var state = capacityHandler.handleCapacity(capacity, context);

        return state == CapacityHandler.State.RECEIVED_SUM;
    }

    public void receiveTask(Task task) {
        var id = task.id();
        currentTasks.put(id, task);

        var capacityHandler = capacityTable.get(id);
        var taskHandle = taskTable.get(id);
        if(capacityHandler != null) { // if accepted
            distributeTask(id);
            taskHandle.sendTaskAccepted();
        } else {
            taskHandle.sendTaskRefused(task.range());
        }
    }

    public void distributeTask(TaskId id) {
        var task = currentTasks.get(id);
        long range = task.range().diff();

        var capacityHandler = capacityTable.get(task.id());
        var totalCapacity = capacityHandler.capacitySum() + 1;

        long unit = range / totalCapacity;

        var from = task.range().from();
        long start = from;
        long limit = start + unit;

        var subTask = new Task(id, task.url(), task.className(), new Range(start, limit));
        System.out.println("Add task : " + subTask);
        //TODO launch the sub task ourself

        // Send the rest to neighbors
        distributeTaskToNeighbors(task, from, limit, unit, capacityHandler);
    }

    private void distributeTaskToNeighbors(Task task, long from, long offset, long unit, CapacityHandler capacityHandler) {
        long start = 0;
        long limit = 0;
        var capacityTable = capacityHandler.getCapacityTable();

        var taskHandler = new TaskHandler(task.id(), null, capacityTable.size() + 1);

        for (Map.Entry<Context, Integer> entry : capacityTable.entrySet()) {
            start = from + offset;
            var capacity = entry.getValue();
            limit = start + unit * capacity;
            offset = limit;

            var subTask = new Task(task.id(), task.url(), task.className(), new Range(start, limit));
            System.out.println("Add task : " + subTask);
            var context = entry.getKey();
            context.queueMessage(subTask);
            taskHandler.addDestination(context);
        }
    }

    public void sendToNeighborsExceptOne(Packet packet, Context exception) {
        selector.keys().stream()
                .filter(selectionKey -> !(selectionKey.channel() instanceof ServerSocketChannel))
                .map(selectionKey -> (Context) selectionKey.attachment())
                .filter(context -> context != exception)
                .forEach(context -> context.queueMessage(packet));
    }

    public int neighborsNumber() {
        int keyCount = selector.keys().size();
        return keyCount - 1;
    }

    public boolean hasNeighborsExceptEmitter() {
        return neighborsNumber() - 1 >= 1;
    }

    public InetSocketAddress serverAddress() {
        try {
            return (InetSocketAddress) serverSocketChannel.getLocalAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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