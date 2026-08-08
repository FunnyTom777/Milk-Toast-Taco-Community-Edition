package mtt.io;

import java.util.List;

public record Command(String name, List<String> args) {
}
