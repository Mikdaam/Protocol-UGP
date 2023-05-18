package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.ResumeTask;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class ResumeTaskReader implements Reader<Packet> {
  @Override
  public ProcessStatus process(ByteBuffer bb) {
    return ProcessStatus.DONE;
  }

  @Override
  public Packet get() {
    return new ResumeTask();
  }

  @Override
  public void reset() {}
}
