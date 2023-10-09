package net.swofty.swm.plugin.command.subtypes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.swofty.swm.api.exceptions.InvalidWorldException;
import net.swofty.swm.api.exceptions.WorldAlreadyExistsException;
import net.swofty.swm.api.exceptions.WorldLoadedException;
import net.swofty.swm.api.exceptions.WorldTooBigException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.loaders.LoaderUtils;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandParameters(description = "Imports a world into SWM saves it", inGameOnly = false, permission = "swm.importworld")
public class subCommand_importworld extends SWMCommand {

    private final Cache<String, String[]> importCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length <= 1) {
            sender.send("§cUsage: /swm import <path-to-world> <datasource> [new-world-name]");
            return;
        }

        String dataSource = args[1];
        SlimeLoader loader = LoaderUtils.getLoader(dataSource);

        if (loader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + dataSource + " does not exist.");
            return;
        }

        File worldDir = new File(args[0]);

        if (!worldDir.exists() || !worldDir.isDirectory()) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Path " + worldDir.getPath() + " does not point out to a valid world directory.");
            return;
        }

        String[] oldArgs = importCache.getIfPresent(sender.getSender().getName());

        if (oldArgs != null) {
            importCache.invalidate(sender.getSender().getName());

            if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                String worldName = (args.length > 2 ? args[2] : worldDir.getName());
                sender.send(Logging.COMMAND_PREFIX + "Importing world " + worldDir.getName() + " into data source " + dataSource + "...");

                Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                    try {
                        long start = System.currentTimeMillis();
                        SWMPlugin.getInstance().importWorld(worldDir, worldName, loader);

                        sender.send(Logging.COMMAND_PREFIX +  ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " imported " +
                                "successfully in " + (System.currentTimeMillis() - start) + "ms. Remember to add it to the worlds config file before loading it.");
                    } catch (WorldAlreadyExistsException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + dataSource + " already contains a world called " + worldName + ".");
                    } catch (InvalidWorldException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Directory " + worldDir.getName() + " does not contain a valid Minecraft world.");
                    } catch (WorldLoadedException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldDir.getName() + " is loaded on this server. Please unload it before importing it.");
                    } catch (WorldTooBigException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Hey! Didn't you just read the warning? The Slime Format isn't meant for big worlds." +
                                " The world you provided just breaks everything. Please, trim it by using the MCEdit tool and try again.");
                    } catch (IOException ex) {
                        if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to import world " + worldName
                                    + ". Take a look at the server console for more information.");
                        }

                        Logging.error("Failed to import world " + worldName + ". Stack trace:");
                        ex.printStackTrace();
                    }

                });
                return;
            }
        }

        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "The Slime Format is meant to " +
                "be used on tiny maps, not big survival worlds. It is recommended to trim your world by using the Prune MCEdit tool to ensure " +
                "you don't save more chunks than you want to.");

        sender.send(" ");
        sender.send(Logging.COMMAND_PREFIX + ChatColor.YELLOW + ChatColor.BOLD + "NOTE: " + ChatColor.GRAY + "This command will automatically ignore every " +
                "chunk that doesn't contain any blocks.");
        sender.send(" ");
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

        importCache.put(sender.getSender().getName(), args);
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        List<String> toReturn = null;

        if (args.length == 3) {
            return new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return Collections.emptyList();
    }
}
