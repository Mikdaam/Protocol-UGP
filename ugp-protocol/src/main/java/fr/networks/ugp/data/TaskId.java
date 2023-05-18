package fr.networks.ugp.data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public record TaskId(long id, InetSocketAddress socketAddress) {
    private static final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 16);
    public ByteBuffer encode() {
        var address = socketAddress.getAddress().getAddress();
        int version = address.length;
        buffer.putLong(id);
        buffer.put((byte) version);
        buffer.put(address);
        buffer.putInt(socketAddress.getPort());
        return buffer;
    }
}
