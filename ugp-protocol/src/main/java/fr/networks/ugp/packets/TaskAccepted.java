package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;

public record TaskAccepted(TaskId id) implements Packet {
    @Override
    public ByteBuffer encode() {
        return id.encode();
    }

    @Override
    public byte type() {
        return 0;
    }
}
