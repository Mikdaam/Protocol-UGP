package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.NewParent;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.address.SocketAddressReader;

import java.nio.ByteBuffer;

public class NewParentReader implements Reader<Packet> {
  private enum State { DONE, REFILL, ERROR }
  private State state = State.REFILL;
  private final SocketAddressReader socketAddressReader = new SocketAddressReader();
  private NewParent newParent;
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    var status = socketAddressReader.process(bb);
    if(status != ProcessStatus.DONE) {
      return status;
    }
    newParent = new NewParent(socketAddressReader.get());
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  public NewParent get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return newParent;
  }

  public void reset() {
    state = State.REFILL;
    socketAddressReader.reset();
  }
}
