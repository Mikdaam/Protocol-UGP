package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;
import java.util.List;

public record Reconnect() implements Packet {
	@Override
	public ByteBuffer encode() { return ByteBuffer.allocate(0); }

	@Override
	public byte type() {
		return 14;
	}
}
