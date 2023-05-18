package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits CapacityRequest, Capacity, Task, TaskRefused {
    public ByteBuffer encode();
}
