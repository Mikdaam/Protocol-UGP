package fr.networks.ugp.readers;

import fr.networks.ugp.packets.LeavingNotification;
import fr.networks.ugp.readers.packets.LeavingNotificationReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LeavingNotificationReaderTest {
  private LeavingNotificationReader leavingNotificationReader;

  @BeforeEach
  void setUp() {
    leavingNotificationReader = new LeavingNotificationReader();
  }

  @Test
  void simple() {
    Assertions.assertEquals(Reader.ProcessStatus.DONE, leavingNotificationReader.process(null));
    Assertions.assertEquals(new LeavingNotification(), leavingNotificationReader.get());
  }
}
