package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;

import java.nio.ByteBuffer;

public class TaskAcceptedReader implements Reader<TaskId> {
  private enum State { DONE, REFILL, ERROR }
  private State state = State.REFILL;
  private final TaskIdReader taskIdReader = new TaskIdReader();
  private TaskId taskId;
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    var status = taskIdReader.process(bb);
    if(status != ProcessStatus.DONE) {
      return status;
    }
    taskId = taskIdReader.get();
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  public TaskId get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return taskId;
  }

  public void reset() {
    taskIdReader.reset();
    state = State.REFILL;
  }
}
