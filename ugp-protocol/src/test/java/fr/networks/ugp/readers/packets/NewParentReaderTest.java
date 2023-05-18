package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.NewParent;
import fr.networks.ugp.readers.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

class NewParentReaderTest {

    private NewParentReader newParentReader;

    @BeforeEach
    void setUp() {
        newParentReader = new NewParentReader();
    }

    @Test
    void simple() {
        InetAddress address = InetAddress.getLoopbackAddress();
        int port = 8080;
        var expectedNewParent = new NewParent(new InetSocketAddress(address, port));

        Assertions.assertEquals(Reader.ProcessStatus.DONE, newParentReader.process(expectedNewParent.encode()));
        Assertions.assertEquals(expectedNewParent, newParentReader.get());
    }

    @Test
    void simpleIPv6() {
        int port = 8080;
        var expectedNewParent = new NewParent(new InetSocketAddress("::1", port));

        Assertions.assertEquals(Reader.ProcessStatus.DONE, newParentReader.process(expectedNewParent.encode()));
        Assertions.assertEquals(expectedNewParent, newParentReader.get());
    }
}