package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.TaskAccepted;
import fr.networks.ugp.readers.Reader;
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
    TaskAccepted expectedTask = new TaskAccepted(new TaskId(id, address));

    Assertions.assertEquals(Reader.ProcessStatus.DONE, taskAcceptedReader.process(expectedTask.encode()));
    Assertions.assertEquals(expectedTask, taskAcceptedReader.get());
  }
}
