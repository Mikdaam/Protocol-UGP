package fr.networks.ugp.readers.base;

import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.base.IntReader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private enum State {
        DONE, WAITING_SIZE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_SIZE;
    private final IntReader sizeReader = new IntReader();
    private final ByteBuffer stringBuffer = ByteBuffer.allocate(1_024 - Integer.BYTES); // write-mode
    private String value;

    private void fillBuffer(ByteBuffer buffer, ByteBuffer internalBuffer) {
        buffer.flip();
        try {
            if (buffer.remaining() <= internalBuffer.remaining()) {
                internalBuffer.put(buffer);
            } else {
                var oldLimit = buffer.limit();
                buffer.limit(internalBuffer.remaining());
                internalBuffer.put(buffer);
                buffer.limit(oldLimit);
            }
        } finally {
            buffer.compact();
        }
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_SIZE) {
            var status = sizeReader.process(buffer);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_CONTENT;
                int size = sizeReader.get();
                if (size < 0 || size > 1020) {
                    return ProcessStatus.ERROR;
                }
                stringBuffer.limit(size);
            } else {
                return status;
            }
        }

        if (state == State.WAITING_CONTENT) {
            fillBuffer(buffer, stringBuffer);
            if (stringBuffer.hasRemaining()) {
                return ProcessStatus.REFILL;
            }
            state = State.DONE;
            stringBuffer.flip();
            value = StandardCharsets.UTF_8.decode(stringBuffer).toString();
            return ProcessStatus.DONE;
        }
        throw new AssertionError();
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        sizeReader.reset();
        stringBuffer.clear();
    }
}