package mtt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mtt.commands.CommandHandler;
import mtt.io.Command;
import mtt.io.CommandParser;
import mtt.io.Console;
import mtt.player.Player;

public class Game {

    private static final List<String> HELP = List.of(
            "help          show this list of commands",
            "look          describe your surroundings",
            "inventory     list what you are carrying",
            "balance       show how much money you have",
            "stats         show your level, xp, and stats",
            "quit          leave the game"
    );

    private final Console console = new Console();
    private final CommandParser parser = new CommandParser();
    private final Map<String, CommandHandler> commands = new LinkedHashMap<>();

    private Player player;
    private String location = "your run-down farmhouse driveway";
    private boolean running = true;

    public Game() {
        register("help", (c, g) -> String.join(System.lineSeparator(), HELP));
        register("look", (c, g) -> describeLocation());
        register("inventory", (c, g) -> describeInventory());
        register("inv", (c, g) -> describeInventory());
        register("balance", (c, g) -> describeBalance());
        register("money", (c, g) -> describeBalance());
        register("stats", (c, g) -> describeStats());
        register("quit", (c, g) -> {
            running = false;
            return null;
        });
        register("exit", (c, g) -> {
            running = false;
            return null;
        });
    }

    public void run() {
        console.println("==============================");
        console.println("  Milk Toast Taco");
        console.println("  Community Edition");
        console.println("==============================");
        console.println();

        if (!introducePlayer()) {
            return;
        }

        console.println("Type 'help' to get started.");
        while (running) {
            console.print("> ");
            String line = console.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Command command = parser.parse(line);
            CommandHandler handler = commands.get(command.name());
            if (handler == null) {
                console.println("Unknown command '" + command.name() + "'. Type 'help'.");
                continue;
            }
            String response = handler.execute(command, this);
            if (response != null) {
                console.println(response);
            }
        }
        console.println("Goodbye!");
    }

    private boolean introducePlayer() {
        console.print("What is your name? ");
        String name = console.readLine();
        if (name == null) {
            return false;
        }
        name = name.trim();
        if (name.isEmpty()) {
            name = "Bystander";
        }
        player = new Player(name, 200.0, List.of("a well-worn spanner"));
        console.println("Welcome, " + player.name() + "!");
        return true;
    }

    private void register(String name, CommandHandler handler) {
        commands.put(name, handler);
    }

    private String describeLocation() {
        return "You are standing on " + location + ". An old pickup truck sits in the yard, mud still caked on its tires.";
    }

    private String describeInventory() {
        if (player.inventory().isEmpty()) {
            return "You are carrying nothing.";
        }
        return "You are carrying: " + String.join(", ", player.inventory());
    }

    private String describeBalance() {
        return String.format("You have $%.2f in your wallet.", player.money());
    }

    private String describeStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Level ").append(player.level()).append(" - ").append(player.xp())
                .append("/").append(player.xpForNextLevel()).append(" XP");
        for (Map.Entry<String, Integer> e : player.stats().entrySet()) {
            sb.append(System.lineSeparator()).append("  ").append(e.getKey()).append(": ").append(e.getValue());
        }
        return sb.toString();
    }
}
