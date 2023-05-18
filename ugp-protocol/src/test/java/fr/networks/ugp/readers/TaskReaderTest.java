package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Task;
import fr.networks.ugp.readers.packets.TaskReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class TaskReaderTest {
  private TaskReader taskReader;

  @BeforeEach
  void setUp() {
    taskReader = new TaskReader();
  }

  @Test
  public void simple() throws MalformedURLException {
    long id = 123456789L;
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
    TaskId taskId = new TaskId(id, address);

    var url = new URL("http://google.com");
    var className = "lambdaClass";
    var range = new Range(0L, 4523L);

    var task = new Task(taskId, url, className, range);

    Assertions.assertEquals(Reader.ProcessStatus.DONE, taskReader.process(task.encode()));
    Assertions.assertEquals(task, taskReader.get());
  }
}
