package net.swofty.swm.plugin.commands.subtypes;

import lombok.SneakyThrows;
import net.swofty.swm.api.world.data.WorldData;
import net.swofty.swm.api.world.data.WorldsConfig;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandCooldown;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Sets a world spawn", inGameOnly = true, permission = "swm.setworldspawn")
public class subCommand_setworldspawn extends SWMCommand implements CommandCooldown {

    @SneakyThrows
    @Override
    public void run(CommandSource sender, String[] args) {
        World world = sender.getPlayer().getWorld();
        WorldsConfig config = SWMPlugin.getInstance().getConfigManager().getWorldConfig();
        WorldData worldData = config.getWorlds().get(world.getName());

        if (worldData == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "You are not currently in a slime world!");
            return;
        }

        worldData.setSpawn(sender.getPlayer().getLocation().getBlockX() + ", " + sender.getPlayer().getLocation().getBlockY() + ", " + sender.getPlayer().getLocation().getBlockZ());
        config.save();

        world.setSpawnLocation(sender.getPlayer().getLocation().getBlockX(), sender.getPlayer().getLocation().getBlockY(), sender.getPlayer().getLocation().getBlockZ());

        sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "Set world spawn for world " + world.getName() + " to " + sender.getPlayer().getLocation().getBlockX() + ", " + sender.getPlayer().getLocation().getBlockY() + ", " + sender.getPlayer().getLocation().getBlockZ() + "!");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        List<String> toReturn = null;

        if (args.length == 2) {
            final String typed = args[1].toLowerCase();

            for (String worldName : SWMPlugin.getInstance().getSlimeWorlds().keySet()) {
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