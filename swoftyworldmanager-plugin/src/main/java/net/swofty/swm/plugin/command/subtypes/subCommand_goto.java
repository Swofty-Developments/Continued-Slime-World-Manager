package net.swofty.swm.plugin.command.subtypes;

import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Teleports a player to a world", inGameOnly = false, permission = "swm.goto")
public class subCommand_goto extends SWMCommand {
    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send("§cUsage: /swm goto <world> <player>");
            return;
        }

        World world = Bukkit.getWorld(args[0]);

        if (world == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " does not exist!");
            return;
        }

        Player target;

        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
        } else {
            if (!(sender.getSender() instanceof Player)) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "The console cannot be teleported to a world! Please specify a player.");
                return;
            }

            target = sender.getPlayer();
        }

        if (target == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + args[1] + " is offline.");
            return;
        }

        sender.send(Logging.COMMAND_PREFIX + "Teleporting " + (target.getName().equals(sender.getSender().getName())
                ? "yourself" : ChatColor.YELLOW + target.getName() + ChatColor.GRAY) + " to " + ChatColor.AQUA + world.getName() + ChatColor.GRAY + "...");

        Location spawnLocation = world.getSpawnLocation();

        // Safe Spawn Location
        while (spawnLocation.getBlock().getType() != Material.AIR || spawnLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
            spawnLocation.add(0, 1, 0);
        }

        target.teleport(spawnLocation);

    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        List<String> toReturn = null;

        if (sender instanceof ConsoleCommandSender) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            final String typed = args[1].toLowerCase();

            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (worldName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(worldName);
                }
            }
        }

        if (args.length == 3) {
            final String typed = args[2].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                final String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(playerName);
                }
            }
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}
