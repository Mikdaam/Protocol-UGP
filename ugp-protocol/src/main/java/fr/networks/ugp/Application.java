package fr.networks.ugp;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.packets.PacketReader;
import fr.networks.ugp.utils.Helpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayDeque;
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
        private final PacketReader packetReader;
        private boolean closed = false;

        private Context(Application server, SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
            packetReader = new PacketReader();
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
                        var value = packetReader.get();
                        server.broadcast(value);
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

        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param msg message
         */
        public void queueMessage(Packet msg) {
            queue.add(msg);
            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bufferOut from the message queue
         *
         */
        private void processOut() {
            while (!queue.isEmpty()) {
                var msg = queue.remove().encode().flip();
                if (bufferOut.remaining() < msg.remaining()) {
                    return;
                }
                bufferOut.put(msg);
            }
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

    public Application(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            Helpers.printKeys(selector); // for debug
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
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
     * @param msg message
     */
    private void broadcast(Packet msg) {
        selector.keys().forEach(selectionKey -> {
            if (selectionKey.channel() instanceof ServerSocketChannel) {
                return;
            }
            ((Context) selectionKey.attachment()).queueMessage(msg);
        });
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        new Application(Integer.parseInt(args[0])).launch();
    }

    private static void usage() {
        System.out.println("Usage : ServerSumBetter port");
    }
}
