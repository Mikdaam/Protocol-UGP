package fr.networks.ugp;

import fr.networks.ugp.packets.Task;

import java.nio.file.Path;

public record LaunchedTask(Task task, Path file) { }
