package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits
        CancelTask,
        Capacity,
        CapacityRequest,
        LeavingNotification,
        NewParent,
        NewParentOK,
        NotifyChild,
        PartialResult,
        Reconnect,
        ReconnectOK,
        Result,
        Task,
        TaskAccepted,
        TaskRefused,
        ChildNotified
{
    ByteBuffer encode();
    byte type();
}
