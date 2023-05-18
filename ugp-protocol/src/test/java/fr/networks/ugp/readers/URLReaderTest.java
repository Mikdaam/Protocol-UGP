package fr.networks.ugp.readers;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLReaderTest {
  @Test
  public void simple() throws java.net.MalformedURLException {
    var string = "http://docs.google.com/document/d/1AjTmTtZMehO35vIylUnCvPO2X7mu7iffD0P4Tbwwfwc/edit#";
    var bb = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    bb.putInt(bytes.remaining()).put(bytes);
    var sr = new URLReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
    assertEquals(new URL(string), sr.get());
    assertEquals(0, bb.position());
    assertEquals(bb.capacity(), bb.limit());
  }

  @Test
  public void reset() throws java.net.MalformedURLException {
    var string = "http://docs.google.com/document/d/1AjTmTtZMehO35vIylUnCvPO2X7mu7iffD0P4Tbwwfwc/edit#";
    var string2 = "http://google.com";
    var bb = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    var bytes2 = StandardCharsets.UTF_8.encode(string2);
    bb.putInt(bytes.remaining()).put(bytes).putInt(bytes2.remaining()).put(bytes2);
    var sr = new URLReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
    assertEquals(new URL(string), sr.get());
    assertEquals(bb.capacity(), bb.limit());
    sr.reset();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
    assertEquals(new URL(string2), sr.get());
    assertEquals(0, bb.position());
    assertEquals(bb.capacity(), bb.limit());
  }

  @Test
  public void reffil() throws java.net.MalformedURLException {
    var string = "http://docs.google.com/document/d/1AjTmTtZMehO35vIylUnCvPO2X7mu7iffD0P4Tbwwfwc/edit#";
    var bb = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    bb.putInt(bytes.remaining()).put(bytes);
    var ur = new URLReader();
    bb.flip();
    var smallBuffer = ByteBuffer.allocate(4);
    var status = Reader.ProcessStatus.REFILL;
    while(bb.hasRemaining()) {
      var oldLimit = bb.limit();
      bb.limit(bb.position() + smallBuffer.capacity());
      smallBuffer.put(bb);
      bb.limit(oldLimit);

      status = ur.process(smallBuffer);
      if(bb.hasRemaining()) {
        assertEquals(Reader.ProcessStatus.REFILL, status);
      }
      smallBuffer.clear();
    }
    assertEquals(Reader.ProcessStatus.DONE, status);
    assertEquals(new URL(string), ur.get());
    assertEquals(0, smallBuffer.position());
    assertEquals(smallBuffer.capacity(), smallBuffer.limit());
  }

  @Test
  public void wrongFormat() {
    var string = "http:/google.com";
    var bb = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    bb.putInt(bytes.remaining()).put(bytes);
    var sr = new URLReader();
    assertEquals(Reader.ProcessStatus.ERROR, sr.process(bb));
  }
}
