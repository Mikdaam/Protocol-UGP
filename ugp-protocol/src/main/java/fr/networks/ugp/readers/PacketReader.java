package fr.networks.ugp.readers;

import fr.networks.ugp.packets.Packet;

import java.nio.ByteBuffer;

public class PacketReader implements Reader<Packet>{
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        return null;
    }

    @Override
    public Packet get() {
        return null;
    }

    @Override
    public void reset() {

    }
}
