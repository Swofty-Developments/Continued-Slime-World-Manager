package net.swofty.swm.plugin.command.subtypes;

import net.swofty.swm.api.exceptions.CorruptedWorldException;
import net.swofty.swm.api.exceptions.NewerFormatException;
import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.command.CommandCooldown;
import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.config.WorldData;
import net.swofty.swm.plugin.config.WorldsConfig;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.List;

@CommandParameters(description = "Loads a template world into SWM which wont be saved", inGameOnly = false, permission = "swm.loadworld.template")
public class subCommand_loadtemplate extends SWMCommand implements CommandCooldown {
    @Override
    public long cooldownSeconds() {
        return 0;
    }

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 1) {
            sender.send("§cUsage: /swm loadtemplate <template-world> <world-name>");
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

        SWMCommand.getWorldsInUse().add(worldName);
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Creating world " + ChatColor.YELLOW + worldName
                + ChatColor.GRAY + " using " + ChatColor.YELLOW + templateWorldName + ChatColor.GRAY + " as a template...");

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

            try {
                long start = System.currentTimeMillis();
                SlimeLoader loader = SWMPlugin.getInstance().getLoader(worldData.getDataSource());

                if (loader == null) {
                    throw new IllegalArgumentException("invalid data source " + worldData.getDataSource());
                }

                SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, templateWorldName, true, worldData.toPropertyMap()).clone(worldName);
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
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName
                            + ". Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world " + templateWorldName + ":");
                ex.printStackTrace();
            } catch (WorldInUseException ignored) {

            } finally {
                SWMCommand.getWorldsInUse().remove(worldName);
            }
        });

    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
