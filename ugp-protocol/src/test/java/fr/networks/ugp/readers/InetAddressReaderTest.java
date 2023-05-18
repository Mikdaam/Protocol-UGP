package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.address.InetAddressReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InetAddressReaderTest {

    private InetAddressReader inetAddressReader;

    @BeforeEach
    void setUp() {
        inetAddressReader = new InetAddressReader();
    }

    @Test
    void simpleIPv4() throws UnknownHostException {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put((byte) 4); // IPv4 version
        bb.put(new byte[]{127, 0, 0, 1}); // IPv4 address

        Assertions.assertEquals(Reader.ProcessStatus.DONE, inetAddressReader.process(bb));
        Assertions.assertEquals(InetAddress.getByName("127.0.0.1"), inetAddressReader.get());
    }

    @Test
    void simpleIPv6() throws UnknownHostException {
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.put((byte) 6); // IPv6 version
        bb.put(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 255, (byte) 255, (byte) 192, 0, 2, 1}); // IPv6 address

        Assertions.assertEquals(Reader.ProcessStatus.DONE, inetAddressReader.process(bb));
        Assertions.assertEquals(InetAddress.getByName("::ffff:c000:201"), inetAddressReader.get());
    }

    @Test
    void invalidIPVersion() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put((byte) 7); // Invalid IP version

        assertEquals(Reader.ProcessStatus.ERROR, inetAddressReader.process(bb));
    }

    @Test
    public void errorGet() {
        assertThrows(IllegalStateException.class, () -> inetAddressReader.get());
    }

    @Test
    void processAndReset() throws UnknownHostException {
        // IPv4 address: 192.168.0.1
        ByteBuffer ipv4Bytes = ByteBuffer.allocate(5);
        ipv4Bytes.put((byte) 4).put((byte) 192).put((byte) 168).put((byte) 0).put((byte) 1); // IPv4 version

        Assertions.assertEquals(Reader.ProcessStatus.DONE, inetAddressReader.process(ipv4Bytes));
        Assertions.assertEquals(InetAddress.getByName("192.168.0.1"), inetAddressReader.get());
        inetAddressReader.reset();

        // IPv6 address: 2001:0db8:85a3:0000:0000:8a2e:0370:7334
        ByteBuffer ipv6Bytes = ByteBuffer.allocate(17); // IPv6 version
        ipv6Bytes.put((byte) 6);
        ipv6Bytes.putShort((short) 0x2001).putShort((short) 0x0db8)
                .putShort((short) 0x85a3).putShort((short) 0x0000)
                .putShort((short) 0x0000).putShort((short) 0x8a2e)
                .putShort((short) 0x0370).putShort((short) 0x7334);

        Assertions.assertEquals(Reader.ProcessStatus.DONE, inetAddressReader.process(ipv6Bytes));
        Assertions.assertEquals(InetAddress.getByName("2001:db8:85a3::8a2e:370:7334"), inetAddressReader.get());
    }
}