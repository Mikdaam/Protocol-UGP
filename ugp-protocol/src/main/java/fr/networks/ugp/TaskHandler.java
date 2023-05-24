package fr.networks.ugp;

import fr.networks.ugp.packets.Result;
import fr.networks.ugp.packets.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskHandler {
    private Context emitter;
    private int resultToWait;
    private final ArrayList<Context> destinations = new ArrayList<>();
    private final Task task;
    private Result partialResults;
    private Result waitingResults;

    public TaskHandler(Task task, int resultToWait, Context emitter) {
        this.emitter = emitter;
        this.task = task;
        this.resultToWait = resultToWait + 1;
        this.waitingResults = null;
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

    public Task task() {
        return task;
    }

    public void storeResult(Context receiveFrom, Result result) {
        destinations.remove(receiveFrom);
        resultToWait--;
        if(waitingResults == null) {
            waitingResults = result;
        } else {
            waitingResults = new Result(result.id(), waitingResults.result() + result.result());
        }
    }

    public void receivedPartialResult(Context resultEmitter) {
        destinations.remove(resultEmitter);
    }
    public Context emitter() {
        return emitter;
    }

    public void updateEmitter(Context newEmitter) {
        emitter = newEmitter;
    }

    public Optional<Result> waitingResult() {
        var res = Optional.ofNullable(waitingResults);
        waitingResults = null;
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
