package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.TaskRefused;
import fr.networks.ugp.readers.RangeReader;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;

import java.nio.ByteBuffer;

public class TaskRefusedReader implements Reader<Packet> {
  private enum State { DONE, WAITING_TASK_ID, WAITING_RANGE, ERROR }
  private State state = State.WAITING_TASK_ID;
  private TaskRefused taskRefused;
  private final TaskIdReader taskIdReader = new TaskIdReader();
  private final RangeReader rangeReader = new RangeReader();
  private TaskId taskId;
  @Override
  public ProcessStatus process(ByteBuffer bb) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    if(state == State.WAITING_TASK_ID) {
      var status = taskIdReader.process(bb);
      if(status != ProcessStatus.DONE) {
        return status;
      }
      taskId = taskIdReader.get();
      state = State.WAITING_RANGE;
    }

    var status = rangeReader.process(bb);
    if(status != ProcessStatus.DONE) {
      return status;
    }
    var range = rangeReader.get();
    taskRefused = new TaskRefused(taskId, range);
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  @Override
  public TaskRefused get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return taskRefused;
  }

  @Override
  public void reset() {
    taskIdReader.reset();
    rangeReader.reset();
    state = State.WAITING_TASK_ID;
  }
}
