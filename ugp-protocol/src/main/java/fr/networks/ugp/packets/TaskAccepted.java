package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public record TaskAccepted() implements Packet {
    @Override
    public ByteBuffer encode() {
        return null;
    }
}