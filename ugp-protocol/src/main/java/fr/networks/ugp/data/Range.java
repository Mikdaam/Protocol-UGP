package fr.networks.ugp.data;

import java.nio.ByteBuffer;

public record Range(Long from, Long to) {
    public long diff() {
        return to - from;
    }

    public ByteBuffer encode() {
        var buffer  = ByteBuffer.allocate(Long.BYTES * 2);
        buffer.putLong(from);
        buffer.putLong(to);
        return buffer;
    }
}
