package fr.networks.ugp.readers.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HTTPReader {

    private final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
    private final int BUF_SIZE = 1024;
    private final SocketChannel sc;
    private final ByteBuffer buffer;

    public HTTPReader(SocketChannel sc, ByteBuffer buffer) {
        this.sc = sc;
        this.buffer = buffer;
    }

    /**
     * @return The ASCII string terminated by CRLF without the CRLF
     *         <p>
     *         The method assume that buffer is in write mode and leaves it in
     *         write mode The method process the data from the buffer and if necessary
     *         will read more data from the socket.
     * @throws IOException HTTPException if the connection is closed before a line
     *                     could be read
     */
    public String readLineCRLF() throws IOException {
        var builder = new StringBuilder();
        boolean hasCR = false;

        buffer.flip();
        while (true) {
            if (!buffer.hasRemaining()) {
                buffer.clear();
                if (sc.read(buffer) == -1) {
                    throw new HTTPException("Connection is closed before read.");
                }
                buffer.flip();
            }

            char c = (char) buffer.get();
            builder.append(c);

            if (c == '\n' && hasCR) {
                buffer.compact();
                break;
            }

            hasCR = c == '\r';
        }

        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    /**
     * @return The HTTPHeader object corresponding to the header read
     * @throws IOException HTTPException if the connection is closed before a header
     *                     could be read or if the header is ill-formed
     */
    public HTTPHeader readHeader() throws IOException {
        var responseStatus = readLineCRLF();
        var emptyLine = "";
        var fields = new HashMap<String, String>();
        String line;
        while (!(line = readLineCRLF()).equals(emptyLine)) {
            var cols = line.split(": ");
            fields.put(cols[0], cols[1]);
        }
        return HTTPHeader.create(responseStatus, fields);
    }

    /**
     * @param size 
     * @return a ByteBuffer in write mode containing size bytes read on the socket
     *         <p>
     *         The method assume that buffer is in write mode and leaves it in
     *         write mode The method process the data from the buffer and if necessary
     *         will read more data from the socket.
     * @throws IOException HTTPException is the connection is closed before all
     *                     bytes could be read
     */
    public ByteBuffer readBytes(int size) throws IOException {
        var readBuffer = ByteBuffer.allocate(size);

        buffer.flip();
        if (buffer.hasRemaining()) {
            while (buffer.hasRemaining() && readBuffer.hasRemaining()) {
                readBuffer.put(buffer.get());
            }
            buffer.compact();
        }

        while (readBuffer.hasRemaining()) {
            if (sc.read(readBuffer) == -1) {
                throw new HTTPException("Connection is closed before read.");
            }
        }

        return readBuffer;
    }

    private ByteBuffer grow(ByteBuffer buffer, int size) {
        buffer.flip();
        var newBuffer = ByteBuffer.allocate(buffer.capacity() + size);
        newBuffer.put(buffer);
        return newBuffer;
    }

    /**
     * @return a ByteBuffer in write-mode containing a content read in chunks mode
     * @throws IOException HTTPException if the connection is closed before the end
     *                     of the chunks if chunks are ill-formed
     */

    public ByteBuffer readChunks() throws IOException {
        var chunksBuffer = ByteBuffer.allocate(BUF_SIZE);

        // read the bytes size
        while (true) {
            int size = Integer.parseInt(readLineCRLF(), 16);
            if (size == 0) {
                break;
            }
            // read a chunk
            var chunk = readBytes(size).flip();
            // grow the main buffer
            int difference = chunk.remaining() - chunksBuffer.remaining();
            chunksBuffer = grow(chunksBuffer, difference);
            chunksBuffer.put(chunk);
            readLineCRLF(); // consume /r/n
        }
        return chunksBuffer;
    }
}