package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskHandler {
    public enum State {WAITING_RESULT, SENT_TO_EMITTER, RECEIVED_ALL_RESULT};
    private final Context emitter;
    private int resultToWait;
    private final ArrayList<Context> destinations = new ArrayList<>();
    public final Task task;
    private final TaskId taskId;
    private Result taskResult;
    private State state = State.WAITING_RESULT;

    public TaskHandler(Task task, int resultToWait, Context emitter) {
        this.emitter = emitter;
        this.task = task;
        this.taskId = task.id();
        this.resultToWait = resultToWait + 1;
    }

    public void addTaskDestination(Context destination) {
        System.out.println("Add a new destination : " + destination);
        destinations.add(destination);
    }

    public State receivedTaskResult(Context resultEmitter, Result result) {
        resultToWait--;
        if(resultEmitter != null) {
            destinations.remove(resultEmitter);
        }

        var oldResult = taskResult.result();
        taskResult = new Result(taskId, oldResult + result.result());

        if(resultToWait == 0) {
            if(emitter != null) {
                state = State.SENT_TO_EMITTER;
            } else {
                state = State.RECEIVED_ALL_RESULT;
            }
        }
        return state;
    }

    public Context emitter() {
        return emitter;
    }

    public Result taskResult() {
        if(state != State.RECEIVED_ALL_RESULT) {
            throw new IllegalStateException("Can't access to res");
        }
        return taskResult;
    }

    /*public void startTask(Task subTask) {
        //TODO start the subTask
    }

    public void stopTask(Context parent) {
        if(emitter == null) { // if we started the conjecture
            destinations.forEach(context -> context.queueMessage(new CancelTask(taskId)));
        } else {
            //TODO stop the task from the other thread and get the values of 'Stopped_At' and 'Résultat'
            try {
                parent.queueMessage(new PartialResult(taskId, emitter.getRemoteAddress(), task.range(), task.range().to(), finalResult));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }*/
}
