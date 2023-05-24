package fr.networks.ugp.utils;

import java.util.Locale;
import java.util.Optional;

public class CommandParser {
    public interface Command{}

    public record Disconnect() implements Command { }
    public record Start(String urlJar, String fullyQualifiedName, long startRange, long endRange, String filename) implements Command {}
    public static Optional<Command> parseCommand(String command) {
        var parts = command.split(" ");
        var keyword = parts[0];

        return switch (keyword.toUpperCase(Locale.ROOT)) {
            case "START" -> {
                if (parts.length != 6) {
                    System.out.println("Invalid START command.");
                    yield Optional.empty();
                }
                yield Optional.of(parseStartCommand(parts));
            }
            case "DISCONNECT" -> Optional.of(new Disconnect());
            default -> {
                System.out.println("Invalid Command");
                yield Optional.empty();
            }
        };
    }

    private static Start parseStartCommand(String[] parts) {
        String urlJar = parts[1];
        String fullyQualifiedName = parts[2];
        int startRange = Integer.parseInt(parts[3]);
        int endRange = Integer.parseInt(parts[4]);
        String filename = parts[5];

        return new Start(urlJar, fullyQualifiedName, startRange, endRange, filename);
    }
}
