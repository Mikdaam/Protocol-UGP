package fr.networks.ugp.packets;

import fr.networks.ugp.data.AddressList;
import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record PartialResult(TaskId id, AddressList destination, Range range, Long stoppedAt, Result result) implements Packet {
    private static final Map<Integer, Integer> bytesToVersion = Map.of(4, 4, 16, 6);

    @Override
    public ByteBuffer encode() {
        var taskIdBuffer = id.encode();
        var destinationBuffer = destination.encode();
        var rangeBuffer = range.encode();
        var resultBuffer = result.encode();

        taskIdBuffer.flip();
        destinationBuffer.flip();
        rangeBuffer.flip();
        resultBuffer.flip();


        var buffer = ByteBuffer.allocate(taskIdBuffer.remaining()
                + destinationBuffer.remaining()
                + rangeBuffer.remaining()
                + Long.BYTES
                + resultBuffer.remaining()
        );

        buffer.put(taskIdBuffer);
        buffer.put(destinationBuffer);
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
