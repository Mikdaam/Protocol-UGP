package fr.networks.ugp.readers;

import fr.networks.ugp.packets.AllSent;
import fr.networks.ugp.readers.packets.AllSentReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AllSentReaderTest {
  private AllSentReader allSentReader;

  @BeforeEach
  void setUp() {
    allSentReader = new AllSentReader();
  }

  @Test
  void simple() {
    Assertions.assertEquals(Reader.ProcessStatus.DONE, allSentReader.process(null));
    Assertions.assertEquals(new AllSent(), allSentReader.get());
  }
}