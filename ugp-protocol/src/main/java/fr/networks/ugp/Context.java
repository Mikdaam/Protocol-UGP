package fr.networks.ugp;

import fr.networks.ugp.packets.*;
import fr.networks.ugp.readers.packets.PacketReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.logging.Logger;

public class Context {
    public static final int BUFFER_SIZE = 10_000;
    private static final Logger logger = Logger.getLogger(Context.class.getName());

    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final Application application; // we could also have Context as an instance class, which would naturally
    // give access to ServerChat.this
    private boolean closed = false;
    private final PacketReader packetReader = new PacketReader();
    private final ArrayDeque<Packet> queue = new ArrayDeque<>();

    public Context(Application application, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.application = application;
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
            System.out.println("Process In buff");
            System.out.println("status : " + status);
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

                boolean isOrigin = capacity.id().socketAddress().equals(application.serverAddress());
                boolean allReceived = application.addNeighborCapacity(this, capacity);

                System.out.println("isOrigin : " + isOrigin);
                System.out.println("allReceived : " + allReceived);

                if (!isOrigin && allReceived) {
                    System.out.println("Send to emitter the sum of neighborsExceptMe and my capacity");
                    var capacitySum = application.getNeighborsCapacities(capacity.id());
                    application.sendToEmitter(capacity, capacitySum);
                }

                if(allReceived) {
                    System.out.println("All response are received");
                    System.out.println("Divide the tasks and assign tasks");
                    return;
                }

            }
            case CapacityRequest capacityRequest -> {
                System.out.println("Received capacity request");
                System.out.println("Received: " + capacityRequest);

                System.out.println("Nb of neighborsExceptMe : " + application.neighborsNumber());
                if (application.hasNeighborsExceptEmitter()) {
                    System.out.println("Send to neighborsExceptMe");
                    application.setEmitter(this);
                    application.sendToNeighborsExceptOne(packet, this);
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
            var packetBuffer = packet.encode();
            packetBuffer.flip();

            var buffer = ByteBuffer.allocate(Byte.BYTES + packetBuffer.remaining());
            buffer.put(packet.type());
            buffer.put(packetBuffer);
            buffer.flip();

            bufferOut.put(buffer);
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