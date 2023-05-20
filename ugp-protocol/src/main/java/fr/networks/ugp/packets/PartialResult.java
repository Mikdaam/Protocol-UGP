package fr.networks.ugp.packets;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record PartialResult(TaskId id, InetSocketAddress socketAddress, Range range, Long stoppedAt, Result result) implements Packet {
    private static final Map<Integer, Integer> bytesToVersion = Map.of(4, 4, 16, 6);
    @Override
    public ByteBuffer encode() {
        var taskIdBuffer = id.encode();
        var addressBuffer = socketAddress.getAddress().getAddress();
        var port = socketAddress.getPort();
        var rangeBuffer = range.encode();
        var resultBuffer = result.encode();

        taskIdBuffer.flip();
        rangeBuffer.flip();
        resultBuffer.flip();

        int bytes = addressBuffer.length;
        int version = bytesToVersion.get(bytes);

        var buffer = ByteBuffer.allocate(taskIdBuffer.remaining()
                + Byte.BYTES + bytes
                + Integer.BYTES
                + rangeBuffer.remaining()
                + Long.BYTES
                + resultBuffer.remaining()
        );

        buffer.put(taskIdBuffer);
        buffer.put((byte) version);
        buffer.put(addressBuffer);
        buffer.putInt(port);
        buffer.put(rangeBuffer);
        buffer.putLong(stoppedAt);
        buffer.put(resultBuffer);
        return buffer;
    }

    @Override
    public byte type() {
        return 10;
    }
}
