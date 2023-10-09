package net.swofty.swm.plugin.command.subtypes;

import net.swofty.swm.plugin.command.CommandCooldown;
import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Unload a world", inGameOnly = false, permission = "swm.unloadworld")
public class subCommand_unloadworld extends SWMCommand implements CommandCooldown {

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Usage: /swm unloadworld <world>");
            return;
        }

        World world = Bukkit.getWorld(args[0]);

        if (world == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " is not loaded!");
            return;
        }

        // Teleport all players outside the world before unloading it
        List<Player> players = world.getPlayers();

        if (!players.isEmpty()) {
            World defaultWorld = Bukkit.getWorlds().get(0);
            Location spawnLocation = defaultWorld.getSpawnLocation();

            while (spawnLocation.getBlock().getType() != Material.AIR || spawnLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
                spawnLocation.add(0, 1, 0);
            }

            for (Player player : players) {
                player.teleport(spawnLocation);
            }
        }

        if (Bukkit.unloadWorld(world, true)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + " unloaded correctly.");
        } else {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to unload world " + args[0] + ".");
        }
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        List<String> toReturn = null;

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

        return toReturn == null ? Collections.emptyList() : toReturn;
    }

    @Override
    public long cooldownSeconds() {
        return 1;
    }
}