package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.Result;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;
import fr.networks.ugp.readers.base.StringReader;

import java.nio.ByteBuffer;

public class ResultReader implements Reader<Packet> {
  private enum State { DONE, WAITING_TASK_ID, WAITING_STRING, ERROR }
  private State state = State.WAITING_TASK_ID;
  private final TaskIdReader taskIdReader = new TaskIdReader();
  private final StringReader stringReader = new StringReader();
  private TaskId taskId;
  private Result result;

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
      state = State.WAITING_STRING;
    }

    var status = stringReader.process(bb);
    if(status != ProcessStatus.DONE) {
      return status;
    }
    var string = stringReader.get();
    result = new Result(taskId, string);
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  @Override
  public Result get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return result;
  }

  @Override
  public void reset() {
    taskIdReader.reset();
    stringReader.reset();
    state = State.WAITING_TASK_ID;
  }
}
