package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.readers.base.IntReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TaskIdListReader implements Reader<ArrayList<TaskId>> {
    private enum State {
        DONE, WAITING_NUMBER, WAITING_IDS, ERROR
    }

    private State state = State.WAITING_NUMBER;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private final IntReader intReader = new IntReader();
    private ArrayList<TaskId> taskIdList;
    private int nbOfIds;
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_NUMBER) {
            var status = intReader.process(bb);
            if (status == ProcessStatus.DONE) {
                state = State.WAITING_IDS;
                nbOfIds = intReader.get();
                if (nbOfIds <= 0) {
                    return ProcessStatus.ERROR;
                }
                taskIdList = new ArrayList<>(nbOfIds);
                intReader.reset();
            } else {
                return status;
            }
        }

        while(state == State.WAITING_IDS) {
            var status = taskIdReader.process(bb);
            if (status != ProcessStatus.DONE) {
                return status;
            } else {
                taskIdList.add(taskIdReader.get());
                taskIdReader.reset();
                if (taskIdList.size() == nbOfIds) {
                    state = State.DONE;
                    return ProcessStatus.DONE;
                }
            }
        }

        throw new AssertionError();
    }

    @Override
    public ArrayList<TaskId> get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return taskIdList;
    }

    @Override
    public void reset() {
        state = State.WAITING_NUMBER;
        intReader.reset();
        taskIdReader.reset();
    }
}
