package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Capacity;
import fr.networks.ugp.readers.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class CapacityReaderTest {
    private CapacityReader capacityReader;

    @BeforeEach
    void setUp() {
        capacityReader = new CapacityReader();
    }

    @Test
    public void simple() {
        long id = 123456789L;
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        Capacity expectedCapacity = new Capacity(new TaskId(id, address), 4250);

        Assertions.assertEquals(Reader.ProcessStatus.DONE, capacityReader.process(expectedCapacity.encode()));
        Assertions.assertEquals(expectedCapacity, capacityReader.get());
    }

    @Test
    public void badCapacity() {
        long id = 123456789L;
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        Capacity expectedCapacity = new Capacity(new TaskId(id, address), -5);

        Assertions.assertEquals(Reader.ProcessStatus.ERROR, capacityReader.process(expectedCapacity.encode()));
    }
}
