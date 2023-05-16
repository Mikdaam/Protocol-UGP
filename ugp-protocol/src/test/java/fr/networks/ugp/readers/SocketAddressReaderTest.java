package fr.networks.ugp.readers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

class SocketAddressReaderTest {

    private SocketAddressReader socketAddressReader;

    @BeforeEach
    void setUp() {
        socketAddressReader = new SocketAddressReader();
    }

    @Test
    void simpleSocketAddress() {
        InetAddress address = InetAddress.getLoopbackAddress();
        int port = 8080;
        ByteBuffer bb = ByteBuffer.allocate(21);
        bb.put((byte) 4); // IPv4 version
        bb.put(address.getAddress()); // IPv4 address
        bb.putInt(port); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, socketAddressReader.process(bb));
        Assertions.assertEquals(new InetSocketAddress(address, port), socketAddressReader.get());
    }


    @Test
    void invalid() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put((byte) 7); // Invalid IP version

        Assertions.assertEquals(Reader.ProcessStatus.ERROR, socketAddressReader.process(bb));
    }

    @Test
    void error() {
        Assertions.assertThrows(IllegalStateException.class, socketAddressReader::get);
    }

    @Test
    void processAndReset() throws UnknownHostException {
        InetAddress address1 = InetAddress.getLoopbackAddress();
        InetAddress address2 = InetAddress.getByName("192.168.0.1");
        int port1 = 8080;
        int port2 = 8888;

        ByteBuffer bb = ByteBuffer.allocate(40);

        // First socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address1.getAddress());
        bb.putInt(port1);

        // Second socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address2.getAddress());
        bb.putInt(port2);

        Assertions.assertEquals(Reader.ProcessStatus.DONE, socketAddressReader.process(bb));
        Assertions.assertEquals(new InetSocketAddress(address1, port1), socketAddressReader.get());
        socketAddressReader.reset();

        Assertions.assertEquals(Reader.ProcessStatus.DONE, socketAddressReader.process(bb));
        Assertions.assertEquals(new InetSocketAddress(address2, port2), socketAddressReader.get());
    }
}