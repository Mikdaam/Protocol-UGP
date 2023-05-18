package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.PartialResult;
import fr.networks.ugp.readers.RangeReader;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;
import fr.networks.ugp.readers.base.LongReader;
import fr.networks.ugp.readers.base.address.SocketAddressReader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PartialResultReader implements Reader<Packet> {
    public enum State { DONE, WAITING_TASK_ID, WAITING_SOURCE, WAITING_RANGE, WAITING_STOPPED, WAITING_RESULT, ERROR }

    private State state = State.WAITING_TASK_ID;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private final SocketAddressReader socketAddressReader = new SocketAddressReader();
    private final RangeReader rangeReader = new RangeReader();
    private final LongReader longReader = new LongReader();
    private final ResultReader resultReader = new ResultReader();
    private PartialResult partialResult;
    private TaskId taskId;
    private InetSocketAddress taskSourceAddress;
    private Range range;
    private Long stoppedAt;

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
            state = State.WAITING_SOURCE;
        }

        if(state == State.WAITING_SOURCE) {
            var status = socketAddressReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            taskSourceAddress = socketAddressReader.get();
            state = State.WAITING_RANGE;
        }

        if(state == State.WAITING_RANGE) {
            var status = rangeReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            range = rangeReader.get();
            state = State.WAITING_STOPPED;
        }

        if(state == State.WAITING_STOPPED) {
            var status = longReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            stoppedAt = longReader.get();
            state = State.WAITING_RESULT;
        }

        var status = resultReader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }
        state = State.DONE;
        var result = resultReader.get();
        partialResult = new PartialResult(taskId, taskSourceAddress, range, stoppedAt, result);
        return ProcessStatus.DONE;
    }

    @Override
    public PartialResult get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return partialResult;
    }

    @Override
    public void reset() {
        state = State.WAITING_TASK_ID;
        taskIdReader.reset();
        socketAddressReader.reset();
        rangeReader.reset();
        longReader.reset();
        resultReader.reset();
    }
}
