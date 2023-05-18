package fr.networks.ugp.data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record TaskId(long id, InetSocketAddress socketAddress) {
    private static final Map<Integer, Integer> bytesToVersion = Map.of(4, 4, 16, 6);
    public ByteBuffer encode() {
        var address = socketAddress.getAddress().getAddress();
        int bytes = address.length;
        int version = bytesToVersion.get(bytes);

        var buffer = ByteBuffer.allocate(Long.BYTES + 1 + bytes + Integer.BYTES);
        buffer.putLong(id);
        buffer.put((byte) version);
        buffer.put(address);
        buffer.putInt(socketAddress.getPort());
        return buffer;
    }
}
