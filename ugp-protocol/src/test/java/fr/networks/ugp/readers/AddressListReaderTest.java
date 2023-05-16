package fr.networks.ugp.readers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

class AddressListReaderTest {

    private AddressListReader addressListReader;

    @BeforeEach
    void setUp() {
        addressListReader = new AddressListReader();
    }

    @Test
    void processIPv4Only() {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8080);
        InetSocketAddress address2 = new InetSocketAddress("192.168.0.1", 8888);

        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.putInt(2); // Number of addresses
        bb.put((byte) 4); // IPv4 version
        bb.put(address1.getAddress().getAddress()); // IPv4 address
        bb.putInt(address1.getPort()); // Port number
        bb.put((byte) 4); // IPv4 version
        bb.put(address2.getAddress().getAddress()); // IPv4 address
        bb.putInt(address2.getPort()); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, addressListReader.process(bb));
        Assertions.assertEquals(Arrays.asList(address1, address2), addressListReader.get());
    }

    @Test
    void processIPv4AndIPv6() {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8080);
        InetSocketAddress address2 = new InetSocketAddress("::1", 8888);

        ByteBuffer bb = ByteBuffer.allocate(42);
        bb.putInt(2); // Number of addresses
        bb.put((byte) 4); // IPv4 version
        bb.put(address1.getAddress().getAddress()); // IPv4 address
        bb.putInt(address1.getPort()); // Port number
        bb.put((byte) 6); // IPv6 version
        bb.put(address2.getAddress().getAddress()); // IPv6 address
        bb.putInt(address2.getPort()); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, addressListReader.process(bb));
        Assertions.assertEquals(Arrays.asList(address1, address2), addressListReader.get());
    }

    @Test
    void invalidState_ShouldThrowIllegalStateException() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);

        ByteBuffer bb = ByteBuffer.allocate(13);
        bb.putInt(1); // Number of addresses
        bb.put((byte) 4); // IPv4 version
        bb.put(address.getAddress().getAddress()); // IPv4 address
        bb.putInt(address.getPort()); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, addressListReader.process(bb));

        Assertions.assertThrows(IllegalStateException.class, () -> addressListReader.process(bb));
    }

    @Test
    void invalidNumberOfAddresses_ShouldReturnError() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(-1); // Negative number of addresses

        Assertions.assertEquals(Reader.ProcessStatus.ERROR, addressListReader.process(bb));
    }

    @Test
    void errorGet_ShouldThrowIllegalStateException() {
        Assertions.assertThrows(IllegalStateException.class, addressListReader::get);
    }

    @Test
    void processAndReset_ShouldResetStateAndAllowReprocessing() {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8080);
        InetSocketAddress address2 = new InetSocketAddress("192.168.0.1", 8888);

        ByteBuffer bb = ByteBuffer.allocate(60);
        bb.putInt(2); // Number of addresses

        // First socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address1.getAddress().getAddress());
        bb.putInt(address1.getPort());

        // Second socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address2.getAddress().getAddress());
        bb.putInt(address2.getPort());

        Assertions.assertEquals(Reader.ProcessStatus.DONE, addressListReader.process(bb));
        Assertions.assertEquals(Arrays.asList(address1, address2), addressListReader.get());
        addressListReader.reset();

        bb.clear();
        bb.putInt(2); // Number of addresses

        // First socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address1.getAddress().getAddress());
        bb.putInt(address1.getPort());

        // Second socket address: IPv4 address + port
        bb.put((byte) 4);
        bb.put(address2.getAddress().getAddress());
        bb.putInt(address2.getPort());

        Assertions.assertEquals(Reader.ProcessStatus.DONE, addressListReader.process(bb));
        Assertions.assertEquals(Arrays.asList(address1, address2), addressListReader.get());
    }
}