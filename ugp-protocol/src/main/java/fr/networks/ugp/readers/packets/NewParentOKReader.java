package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.NewParentOK;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class NewParentOKReader implements Reader<Packet> {
	@Override
	public ProcessStatus process(ByteBuffer bb) {
	return ProcessStatus.DONE;
	}

	@Override
	public Packet get() {
	return new NewParentOK();
	}

	@Override
	public void reset() {}
}