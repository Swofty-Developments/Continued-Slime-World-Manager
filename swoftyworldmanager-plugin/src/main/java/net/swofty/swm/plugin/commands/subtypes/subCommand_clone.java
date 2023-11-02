package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.exceptions.*;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.config.WorldData;
import net.swofty.swm.plugin.config.WorldsConfig;
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

@CommandParameters(description = "Clones a world", inGameOnly = false, permission = "swm.cloneworld")
public class subCommand_clone extends SWMCommand {
    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length < 1) {
            sender.send("§cUsage: /swm clone <template-world> <world-name> [new-data-source]");
            return;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");
            return;
        }

        String templateWorldName = args[0];

        WorldsConfig config = ConfigManager.getWorldConfig();
        WorldData worldData = config.getWorlds().get(templateWorldName);

        if (worldData == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to find world " + templateWorldName + " inside the worlds config file.");
            return;
        }

        if (templateWorldName.equals(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "The template world name cannot be the same as the cloned world one!");
            return;
        }

        if (SWMCommand.getWorldsInUse().contains(worldName)) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");
            return;
        }

        String dataSource = args.length > 2 ? args[2] : worldData.getDataSource();
        SlimeLoader loader = SWMPlugin.getInstance().getLoader(dataSource);

        if (loader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + dataSource + "!");
            return;
        }

        SWMCommand.getWorldsInUse().add(worldName);
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Creating world " + ChatColor.YELLOW + worldName
                + ChatColor.GRAY + " using " + ChatColor.YELLOW + templateWorldName + ChatColor.GRAY + " as a template...");

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            try {
                long start = System.currentTimeMillis();

                SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, templateWorldName, true, worldData.toPropertyMap()).clone(worldName, loader);
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
            } catch (WorldAlreadyExistsException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There is already a world called " + worldName + " stored in " + dataSource + ".");
            } catch (CorruptedWorldException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                            ": world seems to be corrupted.");
                }

                Logging.error("Failed to load world " + templateWorldName + ": world seems to be corrupted.");
                ex.printStackTrace();
            } catch (NewerFormatException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName + ": this world" +
                        " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
            } catch (UnknownWorldException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                        ": world could not be found (using data source '" + worldData.getDataSource() + "').");
            } catch (IllegalArgumentException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                        ": " + ex.getMessage());
            } catch (IOException ex) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName
                            + ". Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world " + templateWorldName + ":");
                ex.printStackTrace();
            } catch (WorldInUseException ignored) { } finally {
                SWMCommand.getWorldsInUse().remove(worldName);
            }
        });
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length == 4) {
            return new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return Collections.emptyList();
    }
}
