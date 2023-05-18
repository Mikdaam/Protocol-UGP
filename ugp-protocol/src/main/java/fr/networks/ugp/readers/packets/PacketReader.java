package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.ByteReader;
import fr.networks.ugp.readers.packets.CapacityReader;
import fr.networks.ugp.readers.packets.TaskReader;

import java.nio.ByteBuffer;
import java.util.Map;

public class PacketReader implements Reader<Packet> {
    private enum State {
        DONE, WAITING_TYPE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_TYPE;
    private final ByteReader byteReader = new ByteReader();
    private byte type;

    private final Map<Byte, Reader<Packet>> readers = Map
            .of(
                    (byte)1, new CapacityReader(),
                    (byte)2, new CapacityReader(),
                    (byte)3, new TaskReader()
            );
    private Packet packet;
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
