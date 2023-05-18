package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.StringReader;

import java.net.URL;
import java.nio.ByteBuffer;

public class URLReader implements Reader<URL> {
    private enum State {
        DONE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_CONTENT;
    private URL url = null;
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        return null;
    }

    @Override
    public URL get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return null;
    }

    @Override
    public void reset() {

    }
}
