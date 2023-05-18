package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class TaskIdReaderTest {
    private TaskIdReader taskIdReader;

    @BeforeEach
    void setUp() {
        taskIdReader = new TaskIdReader();
    }

    @Test
    void simple() {
        long id = 123456789L;
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        TaskId expectedTaskId = new TaskId(id, address);

        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putLong(id); // Task ID
        bb.put((byte) 4); // IPv4 version
        bb.put(address.getAddress().getAddress()); // IPv4 address
        bb.putInt(address.getPort()); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, taskIdReader.process(bb));
        Assertions.assertEquals(expectedTaskId, taskIdReader.get());
    }

    @Test
    void simpleIPv6() {
        long id = 987654321L;
        InetSocketAddress address = new InetSocketAddress("::1", 8888);
        TaskId expectedTaskId = new TaskId(id, address);

        ByteBuffer bb = ByteBuffer.allocate(29);
        bb.putLong(id); // Task ID
        bb.put((byte) 6); // IPv6 version
        bb.put(address.getAddress().getAddress()); // IPv6 address
        bb.putInt(address.getPort()); // Port number

        Assertions.assertEquals(Reader.ProcessStatus.DONE, taskIdReader.process(bb));
        Assertions.assertEquals(expectedTaskId, taskIdReader.get());
    }
}