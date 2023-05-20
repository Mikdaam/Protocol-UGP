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
        var urlBuffer = StandardCharsets.UTF_8.encode(url.toString());
        var classNameBuffer = StandardCharsets.UTF_8.encode(className);

        taskIdBuffer.flip();
        rangeBuffer.flip();
        urlBuffer.flip();
        classNameBuffer.flip();

        var buffer = ByteBuffer.allocate(taskIdBuffer.remaining()
                + Integer.BYTES + urlBuffer.remaining()
                + Integer.BYTES + classNameBuffer.remaining()
                + rangeBuffer.remaining());

        buffer.put(taskIdBuffer);
        buffer.putInt(urlBuffer.remaining());
        buffer.put(urlBuffer);
        buffer.putInt(classNameBuffer.remaining());
        buffer.put(classNameBuffer);
        buffer.put(rangeBuffer);

        return buffer;
    }

    @Override
    public byte type() {
        return 0;
    }
}
