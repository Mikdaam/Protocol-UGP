package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.StringReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

public class URLReader implements Reader<URL> {
    private enum State {
        DONE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_CONTENT;
    private URL url = null;
    private final StringReader stringReader = new StringReader();

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        var status = stringReader.process(bb);
        if(status == ProcessStatus.ERROR) {
            state = State.ERROR;
            return status;
        } else if (status == ProcessStatus.REFILL) {
            return status;
        }
        var string = stringReader.get();
        /*if(!string.matches("^https:\\/\\/.*")) {
            return ProcessStatus.ERROR;
        }*/
        try {
            url = new URL(string);
        } catch (MalformedURLException e) {
            return ProcessStatus.ERROR;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public URL get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return url;
    }

    @Override
    public void reset() {
        state = State.WAITING_CONTENT;
        stringReader.reset();
    }
}
