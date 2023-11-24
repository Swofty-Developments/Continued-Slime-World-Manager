package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldAlreadyExistsException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.data.WorldData;
import net.swofty.swm.api.world.data.WorldsConfig;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandCooldown;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.loader.LoaderUtils;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Migrates a world from one data source to another", inGameOnly = false, permission = "swm.migrate")
public class subCommand_migrate extends SWMCommand implements CommandCooldown {
    @Override
    public long cooldownSeconds() {
        return 0;
    }

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send("§cUsage: /swm migrate <world> <new-data-source>");
            return;
        }

        String worldName = args[0];
        WorldsConfig config = new ConfigManager().getWorldConfig();
        WorldData worldData = config.getWorlds().get(worldName);

        if (worldData == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you configured it correctly?");
            return;
        }

        String newSource = args[1];
        SlimeLoader newLoader = LoaderUtils.getLoader(newSource);

        if (newLoader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + newSource + "!");
            return;
        }

        String currentSource = worldData.getDataSource();

        if (newSource.equalsIgnoreCase(currentSource)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already stored using data source " + currentSource + "!");
            return;
        }

        SlimeLoader oldLoader = LoaderUtils.getLoader(currentSource);

        if (oldLoader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + currentSource + "! Are you sure you configured it correctly?");
            return;
        }

        if (SWMCommand.getWorldsInUse().contains(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");
            return;
        }

        SWMCommand.getWorldsInUse().add(worldName);

        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            try {
                long start = System.currentTimeMillis();
                SWMPlugin.getInstance().migrateWorld(worldName, oldLoader, newLoader);

                worldData.setDataSource(newSource);
                config.save();

                sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " migrated in "
                        + (System.currentTimeMillis() - start) + "ms!");
            } catch (IOException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to migrate world " + worldName + " (using data sources "
                            + currentSource + " and " + newSource + "). Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world " + worldName + " (using data source " + currentSource + "):");
                ex.printStackTrace();
            } catch (WorldInUseException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is being used on another server.");
            } catch (WorldAlreadyExistsException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + newSource + " already contains a world named " + worldName + "!");
            } catch (UnknownWorldException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Can't find world " + worldName + " in data source " + currentSource + ".");
            } finally {
                SWMCommand.getWorldsInUse().remove(worldName);
            }
        });
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

        if (args.length == 3) {
            toReturn = new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}
