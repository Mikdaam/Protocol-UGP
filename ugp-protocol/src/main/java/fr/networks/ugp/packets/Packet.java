package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits Capacity, CapacityRequest, PartialResult, Task, TaskAccepted, TaskRefused {
    public ByteBuffer encode();
}
