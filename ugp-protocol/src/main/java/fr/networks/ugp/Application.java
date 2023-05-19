package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;
import fr.networks.ugp.readers.packets.PacketReader;
import fr.networks.ugp.utils.CommandParser;
import fr.networks.ugp.utils.Helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    static private class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Packet> queue = new ArrayDeque<>();
        private final Application server; // we could also have Context as an instance class, which would naturally
        // give access to ServerChat.this
        private final PacketReader packetReader = new PacketReader();
        private boolean closed = false;

        private Context(Application server, SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
            //packetReader = new PacketReader();
        }

        /**
         * Process the content of bufferIn
         * <p>
         * The convention is that bufferIn is in write-mode before the call to process and
         * after the call
         *
         */
        private void processIn() {
            while (true) {
                var status = packetReader.process(bufferIn);
                switch (status) {
                    case DONE -> {
                        var packet = packetReader.get();
                        System.out.println("Received: " + packet);
                        processPacket(packet);
                        packetReader.reset();
                    }
                    case REFILL -> {
                        return;
                    }
                    case ERROR -> {
                        silentlyClose();
                        return;
                    }
                }
            }
        }

        private void processPacket(Packet packet) {
            switch (packet) {
                case AllSent allSent -> {
                }
                case AllowDeconnection allowDeconnection -> {
                }
                case CancelTask cancelTask -> {
                }
                case Capacity capacity -> {

                }
                case CapacityRequest capacityRequest -> {
                    // Demande de capacité
                    System.out.println("Received capacity request");
                }
                case LeavingNotification leavingNotification -> {
                }
                case NewParent newParent -> {
                }
                case NotifyChild notifyChild -> {
                }
                case PartialResult partialResult -> {
                }
                case Result result -> {
                }
                case ResumeTask resumeTask -> {
                }
                case Task task -> {
                }
                case TaskAccepted taskAccepted -> {
                }
                case TaskRefused taskRefused -> {
                }
            }
        }
        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param packet packet
         */
        public void queueMessage(Packet packet) {
            queue.add(packet);
            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bufferOut from the message queue
         *
         */
        private void processOut() {
            while (!queue.isEmpty()) {
                var packet = queue.remove();
                var opCode = getOpCode(packet);
                var packetBuffer = packet.encode();
                packetBuffer.flip();

                var buffer = ByteBuffer.allocate(Byte.BYTES + packetBuffer.remaining());
                buffer.put(opCode);
                buffer.put(packetBuffer);
                buffer.flip();

                bufferOut.put(buffer);
            }
        }

        private byte getOpCode(Packet packet) {
            return 1;
        }
        /**
         * Update the interestOps of the key looking only at values of the boolean
         * closed and of both ByteBuffers.
         * <p>
         * The convention is that both buffers are in write-mode before the call to
         * updateInterestOps and after the call. Also, it is assumed that process has
         * been called just before updateInterestOps.
         */

        private void updateInterestOps() {
            var newInterestOps = 0;

            if (!closed && bufferIn.hasRemaining()) {
                newInterestOps |= SelectionKey.OP_READ;
            }

            if (bufferOut.position() != 0) {
                newInterestOps |= SelectionKey.OP_WRITE;
            }

            if (newInterestOps == 0) {
                silentlyClose();
                return;
            }
            key.interestOps(newInterestOps);
        }

        private void silentlyClose() {
            try {
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         * <p>
         * The convention is that both buffers are in write-mode before the call to
         * doRead and after the call
         *
         * @throws IOException exception
         */
        private void doRead() throws IOException {
            if (sc.read(bufferIn) == -1) {
                closed = true;
            }
            processIn();
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         * <p>
         * The convention is that both buffers are in write-mode before the call to
         * doWrite and after the call
         *
         * @throws IOException exception
         */

        private void doWrite() throws IOException {
            bufferOut.flip();
            sc.write(bufferOut);
            bufferOut.compact();
            updateInterestOps();
        }

        public void doConnect() throws IOException {
            if (!sc.finishConnect()) {
                logger.warning("The selector give a bad hint");
                return; // selector gave a bad hint
            }
            key.interestOps(SelectionKey.OP_WRITE);
        }

    }

    private static final int BUFFER_SIZE = 10_000;
    private static final Logger logger = Logger.getLogger(Application.class.getName());

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final SocketChannel sc;
    private final InetSocketAddress serverAddress;
    private Queue<String> queue = new ArrayDeque<>();
    private final Thread console;
    private final Object lock = new Object();
    private final ArrayList<Context> connectedClients = new ArrayList<>();
    private final HashMap<SocketAddress, Integer> capacityTable = new HashMap<>();
    private final HashMap<TaskId, Task> tasks = new HashMap<>();
    private long taskCounter = 0;
    private final int port;

    public Application(InetSocketAddress serverAddress, int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        sc = SocketChannel.open();
        sc.bind(new InetSocketAddress(port));
        this.serverAddress = serverAddress;
        this.console = Thread.ofPlatform().unstarted(this::consoleRun);
    }

    public Application(int port) throws IOException {
        this(null, port); // TODO: A modifier
    }


    private void consoleRun() {
        try {
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    var msg = scanner.nextLine();
                    sendCommand(msg);
                }
            }
            logger.info("Console thread stopping");
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        }
    }

    private void sendCommand(String msg) throws InterruptedException {
        synchronized (lock) {
            queue.add(msg);
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
                    TaskId taskId;
                    if(serverAddress == null) {
                         taskId = new TaskId(taskCounter, ((InetSocketAddress) serverSocketChannel.getLocalAddress()));
                    } else {
                         taskId = new TaskId(taskCounter, ((InetSocketAddress) sc.getLocalAddress()));
                    }
                    var url = new URL(start.urlJar());
                    var range = new Range(start.startRange(), start.endRange());

                    var task = new Task(taskId, url, start.fullyQualifiedName(), range);
                    tasks.put(taskId, task);
                    taskCounter++;

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
        sc.configureBlocking(false);
        if(serverAddress != null) {
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

    private void doAccept(SelectionKey key) throws IOException {
        var client = serverSocketChannel.accept();
        if (client == null) {
            logger.warning("The selector give a bad hint");
            return; // selector gave a bad hint
        }
        client.configureBlocking(false);
        var clientKey = client.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new Context(this, clientKey));
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
            new Application(Integer.parseInt(args[0])).launch();
        } else {
            new Application(new InetSocketAddress(args[1], Integer.parseInt(args[2])), Integer.parseInt(args[0])).launch();
        }
    }

    private static void usage() {
        System.out.println("Usage : Application port <dest_hostname> <dest_port>");
    }
}

// TEST LINE
// START http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar fr.uge.factors.Factorizer 0 150 ./res/text.txt