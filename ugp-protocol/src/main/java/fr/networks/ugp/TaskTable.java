package fr.networks.ugp;

import fr.networks.ugp.data.TaskId;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

public class TaskTable {
	private final HashMap<TaskId, TaskHandler> taskTable;

	public TaskTable() {
		taskTable = new HashMap<>();
	}

	public void addTask(TaskId id, TaskHandler taskHandler) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(taskHandler);
		taskTable.put(id, taskHandler);
	}

	public TaskHandler getTaskHandler(TaskId id) {
		Objects.requireNonNull(id);
		return taskTable.get(id);
	}

	public void removeTask(TaskId id) {
		Objects.requireNonNull(id);
		taskTable.remove(id);
	}

	public void forEach(BiConsumer<TaskId, TaskHandler> consumer) {
		Objects.requireNonNull(consumer);
		taskTable.forEach(consumer);
	}
}
