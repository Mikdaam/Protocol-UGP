package fr.networks.ugp;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;

import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;

public class DisconnectionHandler {
  private enum State {RUNNING, WAITING_FOR_NOTIFY, WAITING_TO_DISCONNECT}
  private final HashMap<TaskId, CapacityManager> capacityTable;
  private final HashMap<TaskId, TaskHandler> taskTable;
  private final ArrayList<Context> waitingToDisconnect = new ArrayList<>(); // List of children wanting to disconnect
  private State state = State.RUNNING;
  private final Context parent;
  private final Selector selector;

  public DisconnectionHandler(Context parent, Selector selector, HashMap<TaskId, CapacityManager> capacityTable, HashMap<TaskId, TaskHandler> taskTable) {
    this.parent = parent;
    this.selector = selector;
    this.capacityTable = capacityTable;
    this.taskTable = taskTable;
  }

  public void startingDisconnection() {
    if(parent == null) {
      throw new IllegalStateException("Root can't disconnect");
    }
    parent.queueMessage(new LeavingNotification());
    state = State.WAITING_FOR_NOTIFY;
  }

  public void wantToDisconnect(Context wantingToDisconnect) {
    if(state != State.RUNNING) {
      waitingToDisconnect.add(wantingToDisconnect);
      return;
    }
    wantingToDisconnect.queueMessage(new NotifyChild());
  }

  /*public void receivedNotifyChild() {
    taskTable.forEach((taskId, taskHandler) -> {
      taskHandler.stopTask(parent);
    });
    parent.queueMessage(new AllSent());
    state = State.WAITING_TO_DISCONNECT;
  }*/

  // TODO when we receive allow deconnexion, w<e have to send notify child to all childs waiting
}
