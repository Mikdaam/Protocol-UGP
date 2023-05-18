package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits AllSent, CancelTask, Capacity, CapacityRequest, LeavingNotification, NewParent, NotifyChild, PartialResult, Result, Task, TaskAccepted, TaskRefused {
    ByteBuffer encode();
}
