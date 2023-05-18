package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.CancelTask;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;

import java.nio.ByteBuffer;

public class CancelTaskReader implements Reader<CancelTask> {
  private enum State { DONE, REFILL, ERROR }
  private State state = State.REFILL;
  private final TaskIdReader taskIdReader = new TaskIdReader();
  private CancelTask cancelTask;
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    var status = taskIdReader.process(bb);
    if(status != ProcessStatus.DONE) {
      return status;
    }
    cancelTask = new CancelTask(taskIdReader.get());
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  public CancelTask get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return cancelTask;
  }

  public void reset() {
    taskIdReader.reset();
    state = State.REFILL;
  }
}
