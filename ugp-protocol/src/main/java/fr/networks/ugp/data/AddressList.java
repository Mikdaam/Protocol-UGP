package fr.networks.ugp.data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public record AddressList(List<InetSocketAddress> address) {
	private static final Map<Integer, Integer> bytesToVersion = Map.of(4, 4, 16, 6);

	public ByteBuffer encode() {

		var size = Integer.BYTES;
		for (var element : address) {
			size += Byte.BYTES + element.getAddress().getAddress().length + Integer.BYTES;
		}
		var buffer  = ByteBuffer.allocate(size);

		buffer.putInt(address.size());
		for (var element : address) {
			var addressBuffer = element.getAddress().getAddress();
			var bytes = addressBuffer.length;
			var port = element.getPort();
			int version = bytesToVersion.get(bytes);

			buffer.put((byte) version);
			buffer.put(addressBuffer);
			buffer.putInt(port);
		}
		return buffer;
	}
}
