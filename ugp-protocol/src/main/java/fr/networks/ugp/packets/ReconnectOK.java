package fr.networks.ugp.packets;

import java.nio.ByteBuffer;

public record ReconnectOK() implements Packet{
	@Override
	public ByteBuffer encode() {
		return ByteBuffer.allocate(0);
	}

	@Override
	public byte type() {
		return 16;
	}
}