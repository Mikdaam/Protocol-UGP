package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.Task;
import fr.networks.ugp.packets.TaskAccepted;
import fr.networks.ugp.packets.TaskRefused;

import java.util.ArrayList;
import java.util.Map;

public class TaskHandler {
    private final Context emitter;
    private int responseToWait;
    private final ArrayList<Context> destinations = new ArrayList<>();
    private final Task task;
    private final TaskId taskId;
    private final CapacityHandler capacityHandler;

    public TaskHandler(Task task, CapacityHandler capacityHandler, Context emitter) {
        this.emitter = emitter;
        this.task = task;
        this.taskId = task.id();
        this.capacityHandler = capacityHandler;
        if(capacityHandler == null) {
            this.responseToWait = 1;

        } else {
            this.responseToWait = capacityHandler.getCapacityTable().size() + 1;
        }
    }

    public Task distributeTask() {
        if(capacityHandler == null) {
            return task;
        }
        long range = task.range().diff();
        var totalCapacity = capacityHandler.capacitySum() + 1;

        long unit = range / totalCapacity;

        var from = task.range().from();
        long start = from;
        long limit = start + unit;

        var subTask = new Task(taskId, task.url(), task.className(), new Range(start, limit));
        System.out.println("Add task : " + subTask);

        // Send the rest to neighbors
        var capacityTable = capacityHandler.getCapacityTable();

        for (Map.Entry<Context, Integer> entry : capacityTable.entrySet()) {
            var context = entry.getKey();
            if(emitter != null && emitter == context) {
                continue;
            }

            start = limit;
            var capacity = entry.getValue();
            limit = start + unit * capacity;

            var neighborTask = new Task(task.id(), task.url(), task.className(), new Range(start, limit));
            System.out.println("Add task : " + neighborTask);
            context.queueMessage(neighborTask);
            addDestination(context);

        }

        return subTask;
    }

    public void addDestination(Context destination) {
        System.out.println("Destination : " + destination);
        destinations.add(destination);
    }

    public void sendTaskAccepted() {
        emitter.queueMessage(new TaskAccepted(taskId));
    }

    public void sendTaskRefused(Range range) {
        emitter.queueMessage(new TaskRefused(taskId, range));
    }
}
