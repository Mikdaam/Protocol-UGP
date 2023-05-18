package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Capacity;
import fr.networks.ugp.packets.Task;
import fr.networks.ugp.readers.packets.PacketReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketReaderTest {

    private PacketReader packetReader;

    @BeforeEach
    void setUp() {
        packetReader = new PacketReader();
    }

    @Test
    void simple() {
        long id = 123456789L;
        var address = new InetSocketAddress("127.0.0.1", 8080);
        var expectedPacket = new Capacity(new TaskId(id, address), 5);
        var capBytes = expectedPacket.encode().flip();
        var bb = ByteBuffer.allocate(1 + capBytes.remaining());

        byte type = 1;
        bb.put(type);
        bb.put(capBytes);

        assertEquals(Reader.ProcessStatus.DONE, packetReader.process(bb));
        assertEquals(expectedPacket, packetReader.get());
    }

    @Test
    void sample() throws MalformedURLException {
        long id = 123456789L;
        var address = new InetSocketAddress("127.0.0.1", 8080);
        var expectedPacket = new Task(new TaskId(id, address), new URL("http://docs.google.com/document/d/1AjTmTtZMehO35vIylUnCvPO2X7mu7iffD0P4Tbwwfwc/edit#"), "Checker", new Range(1234L, 5678L));
        var capBytes = expectedPacket.encode().flip();
        var bb = ByteBuffer.allocate(1 + capBytes.remaining());

        byte type = 3;
        bb.put(type);
        bb.put(capBytes);

        assertEquals(Reader.ProcessStatus.DONE, packetReader.process(bb));
        assertEquals(expectedPacket, packetReader.get());
    }

    @Test
    void smallBuffer() {
        long id = 123456789L;
        var address = new InetSocketAddress("127.0.0.1", 8080);
        var expectedPacket = new Capacity(new TaskId(id, address), 5);
        var capBytes = expectedPacket.encode().flip();
        var bb = ByteBuffer.allocate(Byte.BYTES + capBytes.remaining());
        bb.put((byte) 1).put(capBytes).flip();

        var bbSmall = ByteBuffer.allocate(2);
        while (bb.hasRemaining()) {
            while (bb.hasRemaining() && bbSmall.hasRemaining()) {
                bbSmall.put(bb.get());
            }
            if (bb.hasRemaining()) {
                assertEquals(Reader.ProcessStatus.REFILL, packetReader.process(bbSmall));
            } else {
                assertEquals(Reader.ProcessStatus.DONE, packetReader.process(bbSmall));
            }
        }
        assertEquals(expectedPacket, packetReader.get());
    }

}