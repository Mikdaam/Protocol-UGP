package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.StringReader;

import java.net.URL;
import java.nio.ByteBuffer;

public class URLReader implements Reader<URL> {
    private enum State {
        DONE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_CONTENT;
    private URL uri = null;
    private final StringReader stringReader = new StringReader();

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        var status = stringReader.process(bb);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        var string = stringReader.get();
        if(!string.matches("^http://.*")) {
            return ProcessStatus.ERROR;
        }

        try {
            uri = new URL(string);
            state = State.DONE;
            return ProcessStatus.DONE;
        } catch (java.net.MalformedURLException e) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
    }

    @Override
    public URL get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return uri;
    }

    @Override
    public void reset() {
        state = State.WAITING_CONTENT;
        stringReader.reset();
    }
}
