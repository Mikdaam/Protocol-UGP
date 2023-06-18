package fr.networks.ugp.data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public record TaskEmitter(TaskId taskId, InetSocketAddress emitter) {
	private static final Map<Integer, Integer> bytesToVersion = Map.of(4, 4, 16, 6);
	public ByteBuffer encode() {
		ByteBuffer taskIdBuffer = taskId.encode();
		var emitterAddress = emitter.getAddress().getAddress();
		int emitterBytes = emitterAddress.length;
		int version = bytesToVersion.get(emitterBytes);

		var buffer = ByteBuffer.allocate(taskIdBuffer.remaining() + 1 + emitterBytes + Integer.BYTES);
		buffer.put(taskIdBuffer);
		buffer.put((byte) version);
		buffer.put(emitterAddress);
		buffer.putInt(emitter.getPort());
		buffer.flip(); // Prepare buffer for reading
		return buffer;
	}
}
