package fr.networks.ugp.readers;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.readers.base.LongReader;

import java.nio.ByteBuffer;

public class RangeReader implements Reader<Range>{
    public enum State { DONE, WAITING_FROM, WAITING_TO, ERROR };

    private State state = State.WAITING_FROM;
    private Long from;
    private Range range;
    private final LongReader longReader = new LongReader();

    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_FROM) {
            var status = longReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            from = longReader.get();

            if(from < 0) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }

            longReader.reset();
            state = State.WAITING_TO;
        }

        var status = longReader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }

        var to = longReader.get();
        if(to < from) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
        state = State.DONE;
        range = new Range(from, to);
        return ProcessStatus.DONE;
    }

    public Range get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return range;
    }

    public void reset() {
        longReader.reset();
        state = State.WAITING_FROM;
    }
}
