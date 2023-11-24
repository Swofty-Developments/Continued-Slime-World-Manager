package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.exceptions.CorruptedWorldException;
import net.swofty.swm.api.exceptions.NewerFormatException;
import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.api.loaders.SlimeLoader;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@CommandParameters(description = "Loads a world into SWM", inGameOnly = false, permission = "swm.loadworld")

public class subCommand_load extends SWMCommand implements CommandCooldown {
    @Override
    public long cooldownSeconds() {
        return 0;
    }

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send("§cUsage: /swm load <world>");
            return;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");
            return;
        }

        WorldsConfig config = SWMPlugin.getInstance().getConfigManager().getWorldConfig();
        WorldData worldData = config.getWorlds().get(worldName);

        if (worldData == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to find world " + worldName + " inside the worlds config file.");
            return;
        }

        if (SWMCommand.getWorldsInUse().contains(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");
            return;
        }

        SWMCommand.getWorldsInUse().add(worldName);
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Loading world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            try {
                long start = System.currentTimeMillis();
                SlimeLoader loader = SWMPlugin.getInstance().getLoader(worldData.getDataSource());

                if (loader == null) {
                    throw new IllegalArgumentException("invalid data source " + worldData.getDataSource());
                }

                SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, worldName, worldData.isReadOnly(), worldData.toPropertyMap());
                Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                    try {
                        SWMPlugin.getInstance().generateWorld(slimeWorld);
                    } catch (IllegalArgumentException ex) {
                        sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to generate world " + worldName + ": " + ex.getMessage() + ".");
                        return;
                    }

                    sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                            + ChatColor.GREEN + " loaded and generated in " + (System.currentTimeMillis() - start) + "ms!");
                });
            } catch (CorruptedWorldException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world seems to be corrupted.");
                }

                Logging.error("Failed to load world " + worldName + ": world seems to be corrupted.");
                ex.printStackTrace();
            } catch (NewerFormatException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": this world" +
                        " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
            } catch (UnknownWorldException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                        ": world could not be found (using data source '" + worldData.getDataSource() + "').");
            } catch (WorldInUseException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                        ": world is already in use. If you think this is a mistake, please wait some time and try again.");
            } catch (IllegalArgumentException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                        ": " + ex.getMessage());
            } catch (IOException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName
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
}
