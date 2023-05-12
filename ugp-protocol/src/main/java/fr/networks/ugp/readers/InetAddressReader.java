package fr.networks.ugp.readers;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class InetAddressReader implements Reader<InetAddress>{
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        return null;
    }

    @Override
    public InetAddress get() {
        return null;
    }

    @Override
    public void reset() {

    }
}
