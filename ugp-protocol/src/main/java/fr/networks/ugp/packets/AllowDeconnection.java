package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public record AllowDeconnection() implements Packet {
  @Override
  public ByteBuffer encode() {
    return ByteBuffer.allocate(0);
  }
}
