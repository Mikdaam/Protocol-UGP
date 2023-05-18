package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.LeavingNotification;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class LeavingNotificationReader implements Reader<Packet> {
  @Override
  public ProcessStatus process(ByteBuffer bb) {
    return ProcessStatus.DONE;
  }

  @Override
  public Packet get() {
    return new LeavingNotification();
  }

  @Override
  public void reset() {}
}
