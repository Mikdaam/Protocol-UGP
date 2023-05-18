package fr.networks.ugp.readers;

import fr.networks.ugp.readers.base.StringReader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class URLReader implements Reader<URI> {
    private enum State {
        DONE, WAITING_CONTENT, ERROR
    }

    private State state = State.WAITING_CONTENT;
    private URI uri = null;
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
        if(!string.matches("^http://.*")) {
            return ProcessStatus.ERROR;
        }
        try {
            uri = new URI(string);
        } catch (URISyntaxException e) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
        System.out.println(uri.getScheme());
        System.out.println(uri.getHost());
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public URI get() {
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
