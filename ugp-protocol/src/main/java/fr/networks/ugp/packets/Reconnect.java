package fr.networks.ugp.packets;

import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;
import java.util.List;

public record Reconnect(List<TaskId> taskIdList) implements Packet {
	@Override
	public ByteBuffer encode() {
		int idsBytes = 0;
		for (var id : taskIdList) {
			idsBytes += id.encode().flip().remaining();
		}
		var buffer = ByteBuffer.allocate(Integer.BYTES + idsBytes);

		buffer.putInt(taskIdList.size());
		for (var taskId : taskIdList) {
			var taskIdBuffer = taskId.encode().flip();
			buffer.put(taskIdBuffer);
		}
		return buffer;
	}

	@Override
	public byte type() {
		return 12;
	}
}
