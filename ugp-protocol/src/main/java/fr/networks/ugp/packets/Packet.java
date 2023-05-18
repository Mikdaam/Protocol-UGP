package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits Capacity, CapacityRequest, NewParent, PartialResult, Result, Task, TaskAccepted, TaskRefused {
    public ByteBuffer encode();
}
