package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.readers.packets.CancelTaskReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class CancelTaskReaderTest {
  private CancelTaskReader cancelTaskReader;

  @BeforeEach
  void setUp() {
    cancelTaskReader = new CancelTaskReader();
  }

  @Test
  void simple() {
    long id = 123456789L;
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
    TaskId expectedTaskId = new TaskId(id, address);

    Assertions.assertEquals(Reader.ProcessStatus.DONE, cancelTaskReader.process(expectedTaskId.encode()));
    Assertions.assertEquals(expectedTaskId, cancelTaskReader.get());
  }
}
