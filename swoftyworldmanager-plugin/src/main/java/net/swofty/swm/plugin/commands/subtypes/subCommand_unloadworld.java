package net.swofty.swm.plugin.commands.subtypes;

import lombok.SneakyThrows;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.data.WorldData;
import net.swofty.swm.api.world.data.WorldsConfig;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandCooldown;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.*;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Unload a world", inGameOnly = false, permission = "swm.unloadworld")
public class subCommand_unloadworld extends SWMCommand implements CommandCooldown {

    @SneakyThrows
    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Usage: /swm unloadworld <world>");
            return;
        }

        WorldsConfig config = SWMPlugin.getInstance().getConfigManager().getWorldConfig();
        WorldData worldData = config.getWorlds().get(args[0]);

        if (worldData == null || Bukkit.getWorld(args[0]) == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown slime world " + args[0] + "! Are you sure you've typed it correctly?");
            return;
        }

        SlimeWorld world = SWMPlugin.getInstance().getNms().getSlimeWorld(Bukkit.getWorld(args[0]));
        world.unloadWorld(true);
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