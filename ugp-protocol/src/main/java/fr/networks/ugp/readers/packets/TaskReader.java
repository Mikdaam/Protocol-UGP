package fr.networks.ugp.readers.packets;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.packets.Task;
import fr.networks.ugp.readers.RangeReader;
import fr.networks.ugp.readers.Reader;
import fr.networks.ugp.readers.TaskIdReader;
import fr.networks.ugp.readers.URLReader;
import fr.networks.ugp.readers.base.StringReader;

import java.net.URL;
import java.nio.ByteBuffer;

public class TaskReader implements Reader<Packet> {
    public enum State { DONE, WAITING_TASK_ID, WAITING_URL, WAITING_CLASSNAME, WAITING_RANGE, ERROR }

    private State state = State.WAITING_TASK_ID;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private final URLReader urlReader = new URLReader();
    private final StringReader stringReader = new StringReader();
    private final RangeReader rangeReader = new RangeReader();
    private Task task;
    private TaskId taskId;
    private URL url;
    private String className;
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
            state = State.WAITING_URL;
        }

        if(state == State.WAITING_URL) {
            var status = urlReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            url = urlReader.get();
            state = State.WAITING_CLASSNAME;
        }

        if(state == State.WAITING_CLASSNAME) {
            var status = stringReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            className = stringReader.get();

            if(className.length() == 0) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }

            state = State.WAITING_RANGE;
        }

        var status = rangeReader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }

        state = State.DONE;
        var range = rangeReader.get();
        task = new Task(taskId, url, className, range);

        return ProcessStatus.DONE;
    }

    public Task get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return task;
    }

    public void reset() {
        taskIdReader.reset();
        urlReader.reset();
        stringReader.reset();
        rangeReader.reset();
        state = State.WAITING_TASK_ID;
    }
}
