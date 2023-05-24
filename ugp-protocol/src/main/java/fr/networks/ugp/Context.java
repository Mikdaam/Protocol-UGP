package fr.networks.ugp;

import fr.networks.ugp.packets.*;
import fr.networks.ugp.readers.packets.PacketReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
            case AllSent allSent -> application.handleAllSent(this, allSent);
            case AllowDeconnection allowDeconnection -> application.handleAllowDeconnection(this, allowDeconnection);
            case CancelTask cancelTask -> application.handleCancelTask(this, cancelTask);
            case Capacity capacity -> application.handleCapacity(this, capacity);
            case CapacityRequest capacityRequest -> application.handleCapacityRequest(this, capacityRequest);
            case LeavingNotification leavingNotification -> application.handleLeavingNotification(this, leavingNotification);
            case NewParent newParent -> application.handleNewParent(this, newParent);
            case NotifyChild notifyChild -> application.handleNotifyChild(this, notifyChild);
            case PartialResult partialResult -> application.handlePartialResult(this, partialResult);
            case Result result -> application.handleResult(this, result);
            case ResumeTask resumeTask -> application.handleResumeTask(this, resumeTask);
            case Reconnect reconnect -> application.handleReconnect(this, reconnect);
            case ReconnectOK reconnectOK -> application.handleReconnectOK(this, reconnectOK);
            case Task task -> application.handleTask(this, task);
            case TaskAccepted taskAccepted -> application.handleTaskAccepted(this, taskAccepted);
            case TaskRefused taskRefused -> application.handleTaskRefused(this, taskRefused);
            case NewParentOK newParentOK -> application.handleNewParentOK(this, newParentOK);
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

    public void silentlyClose() {
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

    public InetSocketAddress getRemoteAddress() throws IOException {
        return (InetSocketAddress) sc.getRemoteAddress();
    }
}