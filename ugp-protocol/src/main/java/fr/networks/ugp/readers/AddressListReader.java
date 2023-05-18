package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.IntReader;
import fr.networks.ugp.readers.base.address.SocketAddressReader;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AddressListReader implements Reader<ArrayList<SocketAddress>> {
    private enum State {
        DONE, WAITING_NUMBER, WAITING_ADDRESSES, ERROR
    }

    private State state = State.WAITING_NUMBER;
    private final SocketAddressReader socketAddressReader = new SocketAddressReader();
    private final IntReader intReader = new IntReader();
    private ArrayList<SocketAddress> addressList;
    private int nbOfAddress;
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_NUMBER) {
            var status = intReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_ADDRESSES;
                nbOfAddress = intReader.get();
                if (nbOfAddress <= 0) {
                    return ProcessStatus.ERROR;
                }
                addressList = new ArrayList<>(nbOfAddress);
                intReader.reset();
            } else {
                return status;
            }
        }

        while(state == State.WAITING_ADDRESSES) {
            var status = socketAddressReader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                addressList.add(socketAddressReader.get());
                socketAddressReader.reset();
                if (addressList.size() == nbOfAddress) {
                    state = State.DONE;
                    return ProcessStatus.DONE;
                }
            }
        }

        throw new AssertionError();
    }

    @Override
    public ArrayList<SocketAddress> get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return addressList;
    }

    @Override
    public void reset() {
        state = State.WAITING_NUMBER;
        intReader.reset();
        socketAddressReader.reset();
    }
}
