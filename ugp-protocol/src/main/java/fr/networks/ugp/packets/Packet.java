package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits CancelTask, Capacity, CapacityRequest, NewParent, PartialResult, Result, Task, TaskAccepted, TaskRefused {
    ByteBuffer encode();
}
