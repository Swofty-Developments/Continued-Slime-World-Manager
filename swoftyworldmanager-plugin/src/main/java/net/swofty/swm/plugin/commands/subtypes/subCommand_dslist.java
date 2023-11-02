package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
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

@CommandParameters(description = "List all worlds inside a data source", inGameOnly = false, permission = "swm.dslist")
public class subCommand_dslist extends SWMCommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;

    @Override
    public void run(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.send("§cUsage: /swm dslist <data-source> [page]");
            return;
        }

        int page;

        if (args.length == 1) {
            page = 1;
        } else {
            String pageString = args[1];

            try {
                page = Integer.parseInt(pageString);

                if (page < 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "'" + pageString + "' is not a valid number.");
                return;
            }
        }

        String source = args[0];
        SlimeLoader loader = LoaderUtils.getLoader(source);

        if (loader == null) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + source + ".");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            List<String> worldList;

            try {
                worldList = loader.listWorlds();
            } catch (IOException ex) {
                if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                    sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world list. Take a look at the server console for more information.");
                }

                Logging.error("Failed to load world list:");
                ex.printStackTrace();
                return;
            }

            if (worldList.isEmpty()) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There are no worlds stored in data source " + source + ".");
                return;
            }

            int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
            double d = worldList.size() / (double) MAX_ITEMS_PER_PAGE;
            int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

            if (offset >= worldList.size()) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There " + (maxPages == 1 ? "is" :
                        "are") + " only " + maxPages + " page" + (maxPages == 1 ? "" : "s") + "!");
                return;
            }

            worldList.sort(String::compareTo);
            sender.send(Logging.COMMAND_PREFIX + "World list " + ChatColor.YELLOW + "[" + page + "/" + maxPages + "]" + ChatColor.GRAY + ":");

            for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldList.size(); i++) {
                String world = worldList.get(i);
                sender.send(ChatColor.GRAY + " - " + (isLoaded(loader, world) ? ChatColor.GREEN : ChatColor.RED) + world);
            }
        });
    }

    private boolean isLoaded(SlimeLoader loader, String worldName) {
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            SlimeWorld slimeWorld = SWMPlugin.getInstance().getNms().getSlimeWorld(world);

            if (slimeWorld != null) {
                return loader.equals(slimeWorld.getLoader());
            }
        }

        return false;
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        if (args.length == 2) {
            return new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return Collections.emptyList();
    }
}
