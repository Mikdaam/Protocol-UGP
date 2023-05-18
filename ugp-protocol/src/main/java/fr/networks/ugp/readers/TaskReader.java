package fr.networks.ugp.readers;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Packet;
import fr.networks.ugp.readers.base.LongReader;
import fr.networks.ugp.readers.base.StringReader;

import java.net.URL;
import java.nio.ByteBuffer;

public class TaskReader implements Reader<Packet> {
    public enum State { DONE, WAITING_TASK_ID, WAITING_URL, WAITING_CLASSNAME, WAITING_FROM, WAITING_TO, ERROR };

    private State state = State.WAITING_TASK_ID;
    private final TaskIdReader taskIdReader = new TaskIdReader();
    private final URLReader urlReader = new URLReader();
    private final StringReader stringReader = new StringReader();
    private final LongReader longReader = new LongReader();
    private Packet task;
    private TaskId taskId;
    private URL url;
    private String className;
    private Long from;
    private Long to;

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
            state = State.WAITING_FROM;
        }

        /*if(state == State.WAITING_FROM) {
            var status = longReader.process(bb);
            if(status != ProcessStatus.DONE) {
                return status;
            }
            from = longReader.get();
            state = State.WAITING_TO;
            longReader.reset();
        }*/
        // TODO RangeReader
        return ProcessStatus.ERROR;
    }

    public Packet get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return task;
    }

    public void reset() {
        taskIdReader.reset();
        urlReader.reset();
        stringReader.reset();
        longReader.reset();
        state = State.WAITING_TASK_ID;
    }
}
