package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits Capacity, CapacityRequest, Result, Task, TaskRefused {
    public ByteBuffer encode();
}
