package fr.networks.ugp.readers.base.address;

import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.IntReader;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SocketAddressReader implements Reader<InetSocketAddress> {
    private enum State {
        DONE, WAITING_ADDRESS, WAITING_PORT, ERROR
    }

    private State state = State.WAITING_ADDRESS;
    private final InetAddressReader addressReader = new InetAddressReader();
    private final IntReader intReader = new IntReader();
    private InetSocketAddress socketAddress;
    private InetAddress address;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_ADDRESS) {
            var status = addressReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_PORT;
                address = addressReader.get();
                addressReader.reset();
            } else {
                return status;
            }
        }

        if(state == State.WAITING_PORT) {
            var status = intReader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                state = State.DONE;
                int port = intReader.get();
                if (port < 0 || port > 65_535) {
                    return ProcessStatus.ERROR;
                }
                socketAddress = new InetSocketAddress(address, port);
                return ProcessStatus.DONE;
            }
        }

        throw new AssertionError();
    }

    @Override
    public InetSocketAddress get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return socketAddress;
    }

    @Override
    public void reset() {
        state = State.WAITING_ADDRESS;
        intReader.reset();
        addressReader.reset();
    }
}
