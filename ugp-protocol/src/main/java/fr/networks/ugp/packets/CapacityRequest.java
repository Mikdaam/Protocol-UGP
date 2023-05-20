package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;

public record CapacityRequest(TaskId taskId) implements Packet{
    @Override
    public ByteBuffer encode() {
        return taskId.encode();
    }

    @Override
    public byte type() {
        return 1;
    }
}
