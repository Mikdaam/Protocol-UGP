package fr.networks.ugp.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record NewParent(InetSocketAddress newParent) implements Packet {
  private static final Map<Integer, Integer> versionToBytes = Map.of(4, 4, 16, 6);
  @Override
  public ByteBuffer encode() {
    var address = newParent.getAddress().getAddress();
    int bytes = address.length;
    int version = versionToBytes.get(bytes);

    var buffer = ByteBuffer.allocate(Long.BYTES + 1 + bytes + Integer.BYTES);
    buffer.put((byte) version);
    buffer.put(address);
    buffer.putInt(newParent.getPort());

    return buffer;
  }

  @Override
  public byte type() {
    return 11;
  }
}
