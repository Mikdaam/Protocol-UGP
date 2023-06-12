package fr.networks.ugp.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record NewParent(InetSocketAddress address) implements Packet {
  private static final Map<Integer, Integer> versionToBytes = Map.of(4, 4, 16, 6);
  @Override
  public ByteBuffer encode() {
    var ipAddress = address.getAddress().getAddress();
    int bytes = ipAddress.length;
    int version = versionToBytes.get(bytes);

    var buffer = ByteBuffer.allocate(Long.BYTES + 1 + bytes + Integer.BYTES);
    buffer.put((byte) version);
    buffer.put(ipAddress);
    buffer.putInt(address.getPort());

    return buffer;
  }

  @Override
  public byte type() {
    return 11;
  }
}
