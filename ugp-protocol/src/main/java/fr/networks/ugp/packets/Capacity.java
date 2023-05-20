package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;

// TODO replace Integer with something else (to encode with X number of Bytes)
public record Capacity(TaskId id, Integer capacity) implements Packet { // Capacity isd the number of neighbors an application has recursively
    @Override
    public ByteBuffer encode() {
        var taskIdBuffer = id.encode();
        taskIdBuffer.flip();

        var buffer  = ByteBuffer.allocate(taskIdBuffer.remaining() + Integer.BYTES);
        buffer.put(taskIdBuffer);
        buffer.putInt(capacity);

        return buffer;
    }

    @Override
    public byte type() {
        return 2;
    }
}
