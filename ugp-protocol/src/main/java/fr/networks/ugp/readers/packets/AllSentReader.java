package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.AllSent;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class AllSentReader implements Reader<Packet> {
  @Override
  public ProcessStatus process(ByteBuffer bb) {
    return ProcessStatus.DONE;
  }

  @Override
  public AllSent get() {
    return new AllSent();
  }

  @Override
  public void reset() {}
}
