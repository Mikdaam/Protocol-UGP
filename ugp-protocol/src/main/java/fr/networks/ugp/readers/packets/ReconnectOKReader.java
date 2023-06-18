package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.ReconnectOK;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public class ReconnectOKReader implements Reader<Packet> {
	@Override
	public Reader.ProcessStatus process(ByteBuffer bb) {
	return ProcessStatus.DONE;
	}

	@Override
	public Packet get() {
	return new ReconnectOK();
	}

	@Override
	public void reset() {}
}