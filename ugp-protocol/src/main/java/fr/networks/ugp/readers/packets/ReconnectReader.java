package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.NotifyChild;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.Reconnect;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;
import fr.networks.ugp.readers.base.IntReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ReconnectReader implements Reader<Packet> {
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        return ProcessStatus.DONE;
    }

    @Override
    public Packet get() {
        return new Reconnect();
    }

    @Override
    public void reset() {}
}
