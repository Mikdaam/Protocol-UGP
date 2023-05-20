package fr.networks.ugp.packets;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record Result(TaskId id, String result) implements Packet {
  @Override
  public ByteBuffer encode() {
    var taskIdBuffer = id.encode();
    taskIdBuffer.flip();

    var buffer = ByteBuffer.allocate(taskIdBuffer.remaining() + Integer.BYTES + result.length());
    buffer.put(taskIdBuffer);
    buffer.putInt(result.length());
    buffer.put(StandardCharsets.UTF_8.encode(result));
    return buffer;
  }

  @Override
  public byte type() {
    return 0;
  }
}
