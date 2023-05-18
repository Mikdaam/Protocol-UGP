package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits RequestCapacity {
    public ByteBuffer encode();
}
