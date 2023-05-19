package fr.networks.ugp;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.packets.PacketReader;
import fr.networks.ugp.packets.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.logging.Logger;

import static fr.networks.ugp.Application.BUFFER_SIZE;

public class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Packet> queue = new ArrayDeque<>();
        private static final Logger logger = Logger.getLogger(Context.class.getName());
        private final Application application; // we could also have Context as an instance class, which would naturally
        // give access to ServerChat.this
        private final PacketReader packetReader = new PacketReader();
        private boolean closed = false;

        public Context(Application application, SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.application = application;
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
                    System.out.println("Received capacity response");
                    System.out.println("Received " + capacity);
                    if (application.hasNeighbors(1)) {
                        System.out.println("Send to emitters");
                        application.broadcastExceptOne(packet, this);
                    } else {
                        System.out.println("Divide task [get a part] and send the rest to neighbors");

                    }
                }
                case CapacityRequest capacityRequest -> {
                    System.out.println("Received capacity request");
                    System.out.println("Received: " + capacityRequest);
                    if (application.hasNeighbors(1)) {
                        System.out.println("Send to neighbors");
                        application.broadcastExceptOne(packet, this);
                    } else {
                        System.out.println("Respond");
                        queueMessage(new Capacity(capacityRequest.taskId(), 1));
                    }
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
            return switch (packet) {
                case AllSent ignored -> {
                    yield 0;
                }
                case AllowDeconnection allowDeconnection -> {
                    yield 0;
                }
                case CancelTask cancelTask -> {
                    yield 0;
                }
                case Capacity capacity -> {
                    yield 2;
                }
                case CapacityRequest capacityRequest -> {
                    yield 1;
                }
                case LeavingNotification leavingNotification -> {
                    yield 0;
                }
                case NewParent newParent -> {
                    yield 0;
                }
                case NotifyChild notifyChild -> {
                    yield 0;
                }
                case PartialResult partialResult -> {
                    yield 0;
                }
                case Result result -> {
                    yield 0;
                }
                case ResumeTask resumeTask -> {
                    yield 0;
                }
                case Task task -> {
                    yield 0;
                }
                case TaskAccepted taskAccepted -> {
                    yield 0;
                }
                case TaskRefused taskRefused -> {
                    yield 0;
                }
            };
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
        public void doRead() throws IOException {
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

        public void doWrite() throws IOException {
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