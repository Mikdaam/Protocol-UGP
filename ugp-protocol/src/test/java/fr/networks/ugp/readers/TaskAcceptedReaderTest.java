package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.readers.packets.TaskAcceptedReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class TaskAcceptedReaderTest {
  private TaskAcceptedReader taskAcceptedReader;

  @BeforeEach
  void setUp() {
    taskAcceptedReader = new TaskAcceptedReader();
  }

  @Test
  void simple() {
    long id = 123456789L;
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
    TaskId expectedTaskId = new TaskId(id, address);

    Assertions.assertEquals(Reader.ProcessStatus.DONE, taskAcceptedReader.process(expectedTaskId.encode()));
    Assertions.assertEquals(expectedTaskId, taskAcceptedReader.get());
  }
}
