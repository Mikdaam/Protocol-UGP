package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
