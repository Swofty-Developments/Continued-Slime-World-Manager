package net.swofty.swm.plugin.commands.subtypes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.data.WorldData;
import net.swofty.swm.api.world.data.WorldsConfig;
import net.swofty.swm.plugin.SWMPlugin;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandParameters(description = "Deletes a world", inGameOnly = false, permission = "swm.deleteworld")
public class subCommand_delete extends SWMCommand {
    private final Cache<String, String[]> deleteCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send("§cUsage: /swm delete <world> [data-source]");
            return;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is loaded on this server! Unload " +
                    "it by running the command " + ChatColor.GRAY + "/swm unloadworld " + worldName + ChatColor.RED + ".");
            return;
        }

        String source;

        if (args.length > 1) {
            source = args[1];
        } else {
            net.swofty.swm.api.world.data.WorldsConfig config = new ConfigManager().getWorldConfig();
            WorldData worldData = config.getWorlds().get(worldName);

            if (worldData == null) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you've typed it correctly?");
                return;
            }

            source = worldData.getDataSource();
        }

        SlimeLoader loader = LoaderUtils.getLoader(source);

        if (loader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + source + "!  Are you sure you've typed it correctly?");
            return;
        }

        if (SWMCommand.getWorldsInUse().contains(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");
            return;
        }

        String[] oldArgs = deleteCache.getIfPresent(sender.getSender().getName());

        if (oldArgs != null) {
            deleteCache.invalidate(sender.getSender().getName());

            if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Deleting world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

                // No need to do this synchronously
                SWMCommand.getWorldsInUse().add(worldName);
                Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                    try {
                        if (loader.isWorldLocked(worldName)) {
                            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is being used on another server.");
                            return;
                        }

                        long start = System.currentTimeMillis();
                        loader.deleteWorld(worldName);

                        // Now let's delete it from the config file
                        WorldsConfig config = new ConfigManager().getWorldConfig();

                        config.getWorlds().remove(worldName);
                        config.save();

                        sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " deleted in " + (System.currentTimeMillis() - start) + "ms!");
                    } catch (IOException ex) {
                        if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to delete world " + worldName
                                    + ". Take a look at the server console for more information.");
                        }

                        Logging.error("Failed to delete world " + worldName + ". Stack trace:");
                        ex.printStackTrace();
                    } catch (UnknownWorldException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + source + " does not contain any world called " + worldName + ".");
                    } finally {
                        SWMCommand.getWorldsInUse().remove(worldName);
                    }
                });
            }
            return;
        }

        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "You're about to delete " +
                "world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + ". This action cannot be undone.");

        sender.send(" ");
        sender.send(ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

        deleteCache.put(sender.getSender().getName(), args);
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        List<String> toReturn = null;
        final String typed = args[1].toLowerCase();

        if (args.length == 2) {
            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();

                if (worldName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(worldName);
                }
            }
            return toReturn;
        }

        if (args.length == 3) {
            toReturn = new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        if (args.length == 4) {
            toReturn = new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}
