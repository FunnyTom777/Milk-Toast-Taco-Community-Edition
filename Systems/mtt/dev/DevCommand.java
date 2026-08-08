package mtt.dev;

public interface DevCommand {

    String name();

    String description();

    String usage();

    void execute(String[] args, DevConsole console);
}
