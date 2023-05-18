package fr.networks.ugp.readers;

import fr.networks.ugp.packets.NotifyChild;
import fr.networks.ugp.readers.packets.NotifyChildReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NotifyChildReaderTest {
  private NotifyChildReader notifyChildReader;

  @BeforeEach
  void setUp() {
    notifyChildReader = new NotifyChildReader();
  }

  @Test
  void simple() {
    Assertions.assertEquals(Reader.ProcessStatus.DONE, notifyChildReader.process(null));
    Assertions.assertEquals(new NotifyChild(), notifyChildReader.get());
  }
}
