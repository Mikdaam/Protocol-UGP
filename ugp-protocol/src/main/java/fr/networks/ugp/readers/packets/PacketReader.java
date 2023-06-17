package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.ByteReader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PacketReader implements Reader<Packet> {
    private enum State {
        DONE, WAITING_TYPE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_TYPE;
    private final ByteReader byteReader = new ByteReader();
    private int type;

    private final Map<Integer, Reader<Packet>> readers;
    private Packet packet;

    public PacketReader() {
        readers = new HashMap<>();
        readers.put(1, new CapacityRequestReader());
        readers.put(2, new CapacityReader());
        readers.put(3, new TaskReader());
        readers.put(4, new TaskAcceptedReader());
        readers.put(5, new TaskRefusedReader());
        readers.put(6, new ResultReader());
        readers.put(7, new LeavingNotificationReader());
        readers.put(8, new NotifyChildReader());
        readers.put(9, new ChildNotifiedReader());
        readers.put(10, new CancelTaskReader());
        readers.put(11, new PartialResultReader());
        readers.put(12, new NewParentReader());
        readers.put(13, new NewParentOKReader());
        readers.put(14, new ReconnectReader());
        readers.put(15, new ReconnectOKReader());
        readers.put(16, new ResumeTaskReader());
        readers.put(17, new AllSentReader());
        readers.put(18, new AllowDeconnectionReader());
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
