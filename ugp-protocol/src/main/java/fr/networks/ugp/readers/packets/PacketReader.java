package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.TaskAccepted;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.ByteReader;
import fr.networks.ugp.readers.packets.CapacityReader;
import fr.networks.ugp.readers.packets.TaskReader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PacketReader implements Reader<Packet> {
    private enum State {
        DONE, WAITING_TYPE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_TYPE;
    private final ByteReader byteReader = new ByteReader();
    private byte type;

    private final Map<Byte, Reader<Packet>> readers;
    private Packet packet;

    public PacketReader() {
        readers = new HashMap<>();
        readers.put((byte)1, new CapacityReader());
        readers.put((byte)2, new CapacityReader());
        readers.put((byte)3, new TaskReader());
        readers.put((byte)4, new TaskAcceptedReader());
        readers.put((byte)5, new TaskReader());
        readers.put((byte)6, new TaskReader());
        readers.put((byte)7, new TaskReader());
        readers.put((byte)8, new TaskReader());
        readers.put((byte)9, new TaskReader());
        readers.put((byte)10, new TaskReader());
        readers.put((byte)11, new TaskReader());
        readers.put((byte)12, new TaskReader());
        readers.put((byte)13, new TaskReader());
        readers.put((byte)14, new TaskReader());
        readers.put((byte)15, new TaskReader());
    }

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_TYPE) {
            var status = byteReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_CONTENT;
                type = byteReader.get();
                byteReader.reset();
            } else {
                return status;
            }
        }

        if(state == State.WAITING_CONTENT) {
            var reader = readers.get(type);

            var status = reader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                state = State.DONE;
                packet = reader.get();
                return ProcessStatus.DONE;
            }
        }

        throw new AssertionError();
    }

    @Override
    public Packet get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return packet;
    }

    @Override
    public void reset() {
        state = State.WAITING_TYPE;
        byteReader.reset();
        readers.get(type).reset();
    }
}
