package mtt.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandParser {

    public Command parse(String line) {
        List<String> parts = new ArrayList<>();
        for (String part : line.trim().split("\\s+")) {
            parts.add(part);
        }
        String name = parts.isEmpty() ? "" : parts.get(0).toLowerCase(Locale.ROOT);
        List<String> args = List.copyOf(parts.subList(1, parts.size()));
        return new Command(name, args);
    }
}
