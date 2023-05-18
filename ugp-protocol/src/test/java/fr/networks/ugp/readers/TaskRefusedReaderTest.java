package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.TaskRefused;
import fr.networks.ugp.readers.packets.TaskRefusedReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class TaskRefusedReaderTest {
  private TaskRefusedReader taskRefusedReader;

  @BeforeEach
  void setUp() {
    taskRefusedReader = new TaskRefusedReader();
  }

  @Test
  void simple() {
    long id = 123456789L;
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
    TaskId taskId = new TaskId(id, address);

    var range = new Range(0L, 48523L);
    var expectedTaskRefused = new TaskRefused(taskId, range);

    Assertions.assertEquals(Reader.ProcessStatus.DONE, taskRefusedReader.process(expectedTaskRefused.encode()));
    Assertions.assertEquals(expectedTaskRefused, taskRefusedReader.get());
  }
}
