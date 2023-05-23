package fr.networks.ugp;

import fr.networks.ugp.packets.Result;
import fr.networks.ugp.packets.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskHandler {
    private final Context emitter;
    private int resultToWait;
    private final ArrayList<Context> destinations = new ArrayList<>();
    public final Task task;
    private Result waitingResults;

    public TaskHandler(Task task, int resultToWait, Context emitter) {
        this.emitter = emitter;
        this.task = task;
        this.resultToWait = resultToWait + 1;
        this.waitingResults = new Result(task.id(), "");
    }

    public void addTaskDestination(Context destination) {
        System.out.println("Add a new destination : " + destination);
        destinations.add(destination);
    }
    // return true if all the results have been received
    public boolean receivedTaskResult(Context resultEmitter) {
        resultToWait--;
        if(resultEmitter != null) {
            destinations.remove(resultEmitter);
        }
        return resultToWait == 0;
    }

    public void storeResult(Result result) {
        waitingResults = new Result(result.id(), waitingResults.result() + result.result());
    }

    public Context emitter() {
        return emitter;
    }

    public Result taskResult() {
        var res = waitingResults;
        waitingResults = new Result(task.id(), "");
        return res;
    }

    public List<Context> destinations() {
        return destinations;
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
