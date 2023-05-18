package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.AllowDeconnection;
import fr.networks.ugp.readers.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AllowDeconnectionReaderTest {
  private AllowDeconnectionReader allowDeconnectionReader;

  @BeforeEach
  void setUp() {
    allowDeconnectionReader = new AllowDeconnectionReader();
  }

  @Test
  void simple() {
    Assertions.assertEquals(Reader.ProcessStatus.DONE, allowDeconnectionReader.process(null));
    Assertions.assertEquals(new AllowDeconnection(), allowDeconnectionReader.get());
  }
}
