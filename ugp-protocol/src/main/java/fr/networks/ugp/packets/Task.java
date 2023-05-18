package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.net.URL;
import java.nio.ByteBuffer;

public record Task(TaskId id, URL url, String className, Long from, Long to) implements Packet {
    @Override
    public ByteBuffer encode() {
        return null;
    }
}
