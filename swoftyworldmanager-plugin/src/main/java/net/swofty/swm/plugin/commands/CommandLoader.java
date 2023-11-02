package net.swofty.swm.plugin.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandLoader {
    public static List<SWMCommand> commands;

    public CommandLoader() {
        commands = new ArrayList<>();
    }

    public void register(SWMCommand command) {
        commands.add(command);
    }

    public int getCommandAmount() {
        return commands.size();
    }
}