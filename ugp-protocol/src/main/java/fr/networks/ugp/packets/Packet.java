package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public sealed interface Packet permits
        AllSent,
        AllowDeconnection,
        CancelTask,
        Capacity,
        CapacityRequest,
        LeavingNotification,
        NewParent,
        NotifyChild,
        PartialResult,
        Result,
        ResumeTask,
        Task,
        TaskAccepted,
        TaskRefused {
    ByteBuffer encode();
}
