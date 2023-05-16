package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.ByteReader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;

public class InetAddressReader implements Reader<InetAddress>{
    private enum State {
        DONE, WAITING_VERSION, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_VERSION;
    private final Map<Integer, Integer> nbBytes = Map.of(4, 4, 6, 16);
    private final ByteReader byteReader = new ByteReader();
    private ByteBuffer addressBytes = null;
    private InetAddress inetAddress;


    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_VERSION) {
            var status = byteReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_CONTENT;
                int version = byteReader.get();
                if (version != 4 && version != 6) {
                    return ProcessStatus.ERROR;
                }
                int bytes = nbBytes.get(version);
                addressBytes = ByteBuffer.allocate(bytes); // write-mode
                byteReader.reset();
            } else {
                return status;
            }
        }

        while(state == State.WAITING_CONTENT) {
            var status = byteReader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                addressBytes.put(byteReader.get());
                byteReader.reset();
                if (addressBytes.position() == addressBytes.limit()) {
                    addressBytes.flip();
                    try {
                        inetAddress = InetAddress.getByAddress(addressBytes.array());
                    } catch (UnknownHostException e) {
                        throw new AssertionError(e);
                    }
                    state = State.DONE;
                    return ProcessStatus.DONE;
                }
            }
        }

        throw new AssertionError();
    }

    @Override
    public InetAddress get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return inetAddress;
    }

    @Override
    public void reset() {
        state = State.WAITING_VERSION;
        byteReader.reset();
    }
}
