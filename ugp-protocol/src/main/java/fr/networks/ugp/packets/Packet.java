package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits AllSent, AllowDeconnection, CancelTask, Capacity, CapacityRequest, LeavingNotification, NewParent, NewParentOK, NotifyChild, PartialResult, Reconnect, ReconnectOK, Result, ResumeTask, Task, TaskAccepted, TaskRefused
{
    ByteBuffer encode();
    byte type();
}
