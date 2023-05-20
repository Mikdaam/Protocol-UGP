package fr.networks.ugp;

import fr.networks.ugp.data.Range;
import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.TaskAccepted;
import fr.networks.ugp.packets.TaskRefused;

import java.util.ArrayList;

public class TaskHandler {
    private final Context emitter;
    private int responseToWait;
    private final ArrayList<Context> destinations = new ArrayList<>();
    private final TaskId taskId;

    public TaskHandler(TaskId taskId, Context emitter, int responseToWait) {
        this.emitter = emitter;
        this.responseToWait = responseToWait;
        this.taskId = taskId;
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
