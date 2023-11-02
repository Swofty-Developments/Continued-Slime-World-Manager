package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.plugin.commands.CommandCooldown;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@CommandParameters(description = "Returns help page listing out commands", inGameOnly = false)
public class subCommand_help extends SWMCommand implements CommandCooldown {

    // Initialize hashmap with commands
    public static HashMap<String, String> commands;

    static {
        commands = new HashMap<>();
        commands.put("cswm import <path-to-world> <datasource> [new-world-name]", "Convert a world to the slime format and save it");
        commands.put("cswm clone <template-world> <world-name> [new-data-source]", "Clones a world");
        commands.put("cswm create <world> <data-source> [overworld/nether/end]", "Create an empty world");
        commands.put("cswm dslist <data-source> [page]", "List all worlds inside a data source");
        commands.put("cswm delete <world> [data-source]", "Delete a world");
        commands.put("cswm goto <world> [player]", "Teleport yourself (or someone else) to a world");
        commands.put("cswm help", "Shows this page");
        commands.put("cswm loadtemplate <template-world> <world-name>", "Creates a temporary world using another as a template. This world will never be stored.");
        commands.put("cswm load <world>", "Load a world");
        commands.put("cswm migrate <world> <new-data-source>", "Migrate a world from one data source to another");
        commands.put("cswm reload", "Reloads the config files");
        commands.put("cswm unload <world>", "Unload a world");
        commands.put("cswm version", "Shows the plugin version");
        commands.put("cswm list [slime] [page]", "List all worlds. To only list slime worlds, use the 'slime' argument");
    }

    @Override
    public void run(CommandSource sender, String[] args) {
        sender.send((sender.getSender() instanceof Player ? Logging.COMMAND_PREFIX : Logging.CONSOLE_PREFIX) + "Command list:");

        for (String cmd : commands.keySet()) {
            if (sender.getSender() instanceof Player)
                sender.send("  §7-§b/" + cmd + " §7- " + commands.get(cmd));
            else
                sender.send("  -" + cmd + " - " + commands.get(cmd));
        }
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return commands.keySet().stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
    }

    @Override
    public long cooldownSeconds() {
        return 1;
    }
}