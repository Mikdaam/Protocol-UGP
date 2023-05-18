package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Capacity;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;
import fr.networks.ugp.readers.base.IntReader;

import java.nio.ByteBuffer;

public class CapacityReader implements Reader<Packet> {
    private enum State { DONE, WAITING_TASK_ID, WAITING_CAPACITY, ERROR };
    private State state = State.WAITING_TASK_ID;
    private Packet capacity;
    private TaskId taskId;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private final IntReader intReader = new IntReader();

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
            state = State.WAITING_CAPACITY;
        }

        var status = intReader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }

        state = State.DONE;
        var intValue = intReader.get();
        if(intValue <= 0) { // TODO enable to have a capacity of 0 to refuse any tasks
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }

        capacity = new Capacity(taskId, intValue);
        return ProcessStatus.DONE;
    }

    @Override
    public Packet get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return capacity;
    }

    @Override
    public void reset() {
        taskIdReader.reset();
        intReader.reset();
        state = State.WAITING_TASK_ID;
    }
}
