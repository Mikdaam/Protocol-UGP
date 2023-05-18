package fr.networks.ugp.packets;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record Task(TaskId id, URL url, String className, Range range) implements Packet {
    @Override
    public ByteBuffer encode() {
        var taskIdBuffer = id.encode();
        var rangeBuffer = range.encode();
        var urlString = url.toString();

        taskIdBuffer.flip();
        rangeBuffer.flip();

        var buffer = ByteBuffer.allocate(taskIdBuffer.remaining()
                + Integer.BYTES + urlString.length()
                + Integer.BYTES + className.length()
                + rangeBuffer.remaining());

        buffer.put(taskIdBuffer);
        buffer.putInt(urlString.length());
        buffer.put(StandardCharsets.UTF_8.encode(urlString));
        buffer.putInt(className.length());
        buffer.put(StandardCharsets.UTF_8.encode(className));
        buffer.put(rangeBuffer);

        return buffer;
    }
}
