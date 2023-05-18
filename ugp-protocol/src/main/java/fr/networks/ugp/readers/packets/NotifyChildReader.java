package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.NotifyChild;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class NotifyChildReader implements Reader<Packet> {
  @Override
  public ProcessStatus process(ByteBuffer bb) {
    return ProcessStatus.DONE;
  }

  @Override
  public Packet get() {
    return new NotifyChild();
  }

  @Override
  public void reset() {}
}
