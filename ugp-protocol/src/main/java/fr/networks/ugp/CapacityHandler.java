package fr.networks.ugp;

import fr.networks.ugp.packets.Capacity;

import java.util.HashMap;

public class CapacityHandler {
  private final HashMap<Context, Integer> capacityTable = new HashMap<>();
  private int responseToWait;
  private final Context emitter;

  public CapacityHandler(Context emitter, int responseToWait) {
    this.emitter = emitter;
    this.responseToWait = responseToWait;
  }

  public void handleCapacity(Capacity capacity, Context receivedFrom) {
    responseToWait -= 1;
    capacityTable.put(receivedFrom, capacity.capacity());

    if(responseToWait > 0) {
      return;
    }

    var sum = capacitySum();

    System.out.println("Everything was received");
    System.out.println("sum = " + sum);

    if(emitter != null) {
      System.out.println("Sending capacity to emitter");
      sendToEmitter(capacity, sum);
      return;
    }

    System.out.println("No emitter to send");
  }

  private void sendToEmitter(Capacity capacity, int sum) {
    emitter.queueMessage(new Capacity(capacity.id(), sum + 1));
  }

  private int capacitySum() {
    return capacityTable.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
  }
}
