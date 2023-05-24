package fr.networks.ugp;

import fr.networks.ugp.data.TaskId;
import fr.networks.ugp.packets.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DisconnectionHandler {
  //private enum State {RUNNING, WAITING_FOR_NOTIFY, WAITING_TO_DISCONNECT}
  private final HashMap<TaskId, CapacityManager> capacityTable;
  private final HashMap<TaskId, TaskHandler> taskTable;
  //private State state = State.RUNNING;
  private final Context parent;
  private final Selector selector;
  private int disconnectingChildren = 0;

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
    //state = State.WAITING_FOR_NOTIFY;
  }

  public void wantToDisconnect(Context wantingToDisconnect) {
    wantingToDisconnect.queueMessage(new NotifyChild());
  }

  /*taskTable.forEach((taskId, taskHandler) -> {
      taskHandler.stopTask(parent);
    });
    parent.queueMessage(new AllSent());
    state = State.WAITING_TO_DISCONNECT;*/
  public void receivedNotifyChild(InetSocketAddress parentAddress, List<Context> children) {
    for (var child : children) {
      child.queueMessage(new NewParent(parentAddress));
      disconnectingChildren++;
    }
  }

  // return true if all children reconnecteed
  public boolean receivedNewParentOk() {
    disconnectingChildren--;
    return disconnectingChildren == 0;
  }

  // TODO when we receive allow deconnexion, w<e have to send notify child to all childs waiting
}
