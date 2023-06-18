package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.ChildNotified;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;

import java.nio.ByteBuffer;

public record ChildNotifiedReader() implements Reader<Packet> {
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return ProcessStatus.DONE;
	}

	@Override
	public Packet get() {
		return new ChildNotified();
	}

	@Override
	public void reset() {}
}
