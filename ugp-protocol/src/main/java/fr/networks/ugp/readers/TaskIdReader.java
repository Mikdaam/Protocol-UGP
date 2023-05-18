package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.readers.base.LongReader;

import java.nio.ByteBuffer;

public class TaskIdReader implements Reader<TaskId>{
    private enum State {
        DONE, WAITING_NUMBER, WAITING_ADDRESS, ERROR
    }

    private State state = State.WAITING_NUMBER;
    private final SocketAddressReader socketAddressReader = new SocketAddressReader();
    private final LongReader longReader = new LongReader();
    private long id;
    private TaskId taskId;
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_NUMBER) {
            var status = longReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_ADDRESS;
                id = longReader.get();
                if (id < 0) {
                    return ProcessStatus.ERROR;
                }
                longReader.reset();
            } else {
                return status;
            }
        }

        if(state == State.WAITING_ADDRESS) {
            var status = socketAddressReader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                taskId = new TaskId(id, socketAddressReader.get());
                socketAddressReader.reset();
                state = State.DONE;
                return ProcessStatus.DONE;
            }
        }

        throw new AssertionError();
    }

    @Override
    public TaskId get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return taskId;
    }

    @Override
    public void reset() {
        state = State.WAITING_ADDRESS;
        longReader.reset();
        socketAddressReader.reset();
    }
}