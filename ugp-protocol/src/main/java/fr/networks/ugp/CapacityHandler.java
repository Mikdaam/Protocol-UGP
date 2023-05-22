package fr.networks.ugp;

import fr.networks.ugp.packets.Capacity;

import java.util.HashMap;

public class CapacityHandler {
	public enum State { WAITING_RESPONSE, SENT_TO_EMITTER, RECEIVED_ALL};

	private final HashMap<Context, Integer> taskCapacityTable = new HashMap<>();
	private final Context emitter;
	private int responseToWait;
	private State state = State.WAITING_RESPONSE;

	public CapacityHandler(Context emitter, int responseToWait) {
		this.emitter = emitter;
		this.responseToWait = responseToWait;
	}

	/**
	 *
	 * @param capacity
	 * @param receivedFrom the context from wich the capacity was received
	 * @return the sum of the capacity table if there is no emitter to send the capacity, -1 else
	 */
	public State handleCapacity(Capacity capacity, Context receivedFrom) {
		responseToWait -= 1;
		taskCapacityTable.put(receivedFrom, capacity.capacity());

		if(responseToWait > 0) {
			state = State.WAITING_RESPONSE;
			return state;
		}

		if(emitter != null) {
			state = State.SENT_TO_EMITTER;
			System.out.println("Sending capacity to emitter");
			sendToEmitter(capacity, capacitySum());
			return state;
		}

		System.out.println("Received capacity with no emitter");
		state = State.RECEIVED_ALL;
		return state;
	}

	public HashMap<Context, Integer> getTaskCapacityTable() {
		if(state == State.WAITING_RESPONSE) {
			throw new IllegalStateException("Shouldn't access to taskCapacityTable if not received all capacity");
		}
		return taskCapacityTable;
	}
	private void sendToEmitter(Capacity capacity, int sum) {
		emitter.queueMessage(new Capacity(capacity.id(), sum + 1));
	}

	public int capacitySum() {
		return taskCapacityTable.values().stream()
				.mapToInt(Integer::intValue)
				.sum();
	}
}
