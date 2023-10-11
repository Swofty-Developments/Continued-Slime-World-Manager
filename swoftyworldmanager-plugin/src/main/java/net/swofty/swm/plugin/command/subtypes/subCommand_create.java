package net.swofty.swm.plugin.command.subtypes;

import net.swofty.swm.api.exceptions.WorldAlreadyExistsException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.config.WorldData;
import net.swofty.swm.plugin.config.WorldsConfig;
import net.swofty.swm.plugin.loaders.LoaderUtils;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.List;

@CommandParameters(description = "Creates an empty world", inGameOnly = false, permission = "swm.createworld")
public class subCommand_create extends SWMCommand {
    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length <= 1) {
            sender.send("§cUsage: /swm create <world> <data-source>");
            return;
        }

        String worldName = args[0];

        if (SWMCommand.getWorldsInUse().contains(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " already exists!");
            return;
        }

        WorldsConfig config = ConfigManager.getWorldConfig();

        if (config.getWorlds().containsKey(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There is already a world called  " + worldName + " inside the worlds config file.");
            return;
        }

        String dataSource = args[1];
        SlimeLoader loader = SWMPlugin.getInstance().getLoader(dataSource);

        if (loader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source  " + dataSource + ".");
            return;
        }

        SWMCommand.getWorldsInUse().add(worldName);
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Creating empty world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            try {
                long start = System.currentTimeMillis();

                WorldData worldData = new WorldData();
                worldData.setSpawn("0, 64, 0");

                SlimePropertyMap propertyMap = worldData.toPropertyMap();
                SlimeWorld slimeWorld = SWMPlugin.getInstance().createEmptyWorld(loader, worldName, false, propertyMap);

                Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                    try {
                        SWMPlugin.getInstance().generateWorld(slimeWorld);

                        // Bedrock block
                        Location location = new Location(Bukkit.getWorld(worldName), 0, 61, 0);
                        location.getBlock().setType(Material.BEDROCK);

                        // Config
                        config.getWorlds().put(worldName, worldData);
                        config.save();

                        sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " created in " + (System.currentTimeMillis() - start) + "ms!");
                    } catch (IllegalArgumentException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to create world " + worldName + ": " + ex.getMessage() + ".");
                    }
                });
            } catch (WorldAlreadyExistsException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to create world " + worldName +
                        ": world already exists (using data source '" + dataSource + "').");
            } catch (IOException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to create world " + worldName
                            + ". Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world " + worldName + ":");
                ex.printStackTrace();
            } finally {
                SWMCommand.getWorldsInUse().remove(worldName);
            }
        });
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length == 3)
            return LoaderUtils.getAvailableLoadersNames();
        return null;
    }
}
