package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.ResumeTask;
import fr.networks.ugp.readers.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResumeTaskReaderTest {
  private ResumeTaskReader resumeTaskReader;

  @BeforeEach
  void setUp() {
    resumeTaskReader = new ResumeTaskReader();
  }

  @Test
  void simple() {
    Assertions.assertEquals(Reader.ProcessStatus.DONE, resumeTaskReader.process(null));
    Assertions.assertEquals(new ResumeTask(), resumeTaskReader.get());
  }
}
