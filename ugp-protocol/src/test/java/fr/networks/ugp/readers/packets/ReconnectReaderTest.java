package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Reconnect;
import fr.networks.ugp.readers.AddressListReader;
import fr.networks.ugp.readers.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class ReconnectReaderTest {
	private ReconnectReader reconnectReader;

	@BeforeEach
	void setUp() {
		reconnectReader = new ReconnectReader();
	}

	@Test
	void process() {
		var task1 = new TaskId(1, new InetSocketAddress("127.0.0.1", 8080));
		var task2 = new TaskId(2, new InetSocketAddress("192.168.0.1", 8888));
		var rec = new Reconnect();

		var bb = rec.encode();

		Assertions.assertEquals(Reader.ProcessStatus.DONE, reconnectReader.process(bb));
		Assertions.assertEquals(rec, reconnectReader.get());
	}
}
