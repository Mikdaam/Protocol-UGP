package fr.networks.ugp.readers.packets;

import fr.networks.ugp.packets.CapacityRequest;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;

import java.nio.ByteBuffer;

public class CapacityRequestReader implements Reader<Packet> {
    private enum State { DONE, WAITING_TASK_ID, ERROR }

    private State state = State.WAITING_TASK_ID;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private CapacityRequest capacityRequest;
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        var status = taskIdReader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }
        capacityRequest = new CapacityRequest(taskIdReader.get());
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public CapacityRequest get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return capacityRequest;
    }

    @Override
    public void reset() {
        state = State.WAITING_TASK_ID;
        taskIdReader.reset();
    }
}
