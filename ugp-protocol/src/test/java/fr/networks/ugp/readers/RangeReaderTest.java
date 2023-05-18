package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class RangeReaderTest {
    private RangeReader rangeReader;

    @BeforeEach
    void setUp() {
        rangeReader = new RangeReader();
    }

    @Test
    public void simple() {
        var expectedRange = new Range(0L, 48523L);

        Assertions.assertEquals(Reader.ProcessStatus.DONE, rangeReader.process(expectedRange.encode()));
        Assertions.assertEquals(expectedRange, rangeReader.get());
    }

    @Test
    public void negativeLong() {
        var expectedRange = new Range(-1L, 48523L);

        Assertions.assertEquals(Reader.ProcessStatus.ERROR, rangeReader.process(expectedRange.encode()));
    }

    @Test
    public void toInferiorFrom() {
        var expectedRange = new Range(500L, 15L);

        Assertions.assertEquals(Reader.ProcessStatus.ERROR, rangeReader.process(expectedRange.encode()));
    }

    @Test
    public void smallBuffer() {
        var expectedRange = new Range(0L, 48523L);
        var rangeBuffer = expectedRange.encode();
        var smallBuffer = ByteBuffer.allocate(Long.BYTES);

        rangeBuffer.flip();
        smallBuffer.putLong(rangeBuffer.getLong());
        Assertions.assertEquals(Reader.ProcessStatus.REFILL, rangeReader.process(smallBuffer));

        smallBuffer.clear();
        smallBuffer.putLong(rangeBuffer.getLong());
        Assertions.assertEquals(Reader.ProcessStatus.DONE, rangeReader.process(smallBuffer));

        Assertions.assertEquals(expectedRange, rangeReader.get());
    }
}
