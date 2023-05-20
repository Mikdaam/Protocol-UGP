package fr.networks.ugp.packets;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;

import java.nio.ByteBuffer;

public record TaskRefused(TaskId id, Range range) implements Packet {
  @Override
  public ByteBuffer encode() {
    var taskIdBuffer = id.encode();
    var rangeBuffer = range.encode();
    taskIdBuffer.flip();
    rangeBuffer.flip();

    var buffer = ByteBuffer.allocate(taskIdBuffer.remaining() + rangeBuffer.remaining());
    buffer.put(taskIdBuffer);
    buffer.put(rangeBuffer);
    return buffer;
  }

  @Override
  public byte type() {
    return 0;
  }
}
