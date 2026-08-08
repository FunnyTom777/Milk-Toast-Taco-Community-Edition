package mtt.commands;

import mtt.Game;
import mtt.io.Command;

@FunctionalInterface
public interface CommandHandler {

    String execute(Command command, Game game);
}
