package com.grinderwolf.swm.plugin.command.subtypes;

import com.grinderwolf.swm.plugin.command.CommandCooldown;
import com.grinderwolf.swm.plugin.command.CommandParameters;
import com.grinderwolf.swm.plugin.command.CommandSource;
import com.grinderwolf.swm.plugin.command.SWMCommand;
import com.grinderwolf.swm.plugin.log.Logging;
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
        commands.put("swm import <path-to-world> <datasource> [new-world-name]", "Convert a world to the slime format and save it.");
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

    @Override
    public long getCooldown() {
        return 1;
    }
}