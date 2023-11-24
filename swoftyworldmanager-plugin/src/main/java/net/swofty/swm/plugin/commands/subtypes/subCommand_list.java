package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandParameters(description = "Lists all loaded worlds in SWM", inGameOnly = false, permission = "swm.listworld")
public class subCommand_list extends SWMCommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;

    @Override
    public void run(CommandSource sender, String[] args) {
        Map<String, SlimeWorld> slimeWorlds = SWMPlugin.getInstance().getSlimeWorlds();
        ArrayList<World> bukkitWorlds = Bukkit.getWorlds().stream()
                .filter(world -> world != null && SWMPlugin.getInstance().getNms().getSlimeWorld(world) == null)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean onlySlime = args.length > 0 && args[0].equalsIgnoreCase("slime");

        if (onlySlime) {
            bukkitWorlds.clear();
        }

        int page;

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("slime"))) {
            page = 1;
        } else {
            String pageString = args[args.length - 1];

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

        List<String> worldsList = new ArrayList<>(slimeWorlds.keySet());
        worldsList.addAll(bukkitWorlds.stream().map(World::getName).collect(Collectors.toList()));
        SWMPlugin.getInstance().getConfigManager().getWorldConfig().getWorlds().keySet().stream().filter((world) -> !worldsList.contains(world)).forEach(worldsList::add);

        if (worldsList.isEmpty()) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There are no worlds configured.");
            return;
        }

        int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
        double d = worldsList.size() / (double) MAX_ITEMS_PER_PAGE;
        int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

        if (offset >= worldsList.size()) {
            sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "There " + (maxPages == 1 ? "is" :
                    "are") + " only " + maxPages + " page" + (maxPages == 1 ? "" : "s") + "!");
            return;
        }

        worldsList.sort(String::compareTo);
        sender.send(Logging.COMMAND_PREFIX + "World list " + ChatColor.YELLOW + "[" + page + "/" + maxPages + "]" + ChatColor.GRAY + ":");

        for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldsList.size(); i++) {
            String worldName = worldsList.get(i);
            SlimeWorld slimeWorld = slimeWorlds.getOrDefault(worldName, null);

            ChatColor worldColor = bukkitWorlds.contains(Bukkit.getWorld(worldName)) || (Bukkit.getWorld(worldName) != null && SWMPlugin.getInstance().getNms().getSlimeWorld(Bukkit.getWorld(worldName)) != null) ?
                    ChatColor.GREEN : ChatColor.RED;

            String environmentDisplay = "";
            if (worldColor == ChatColor.GREEN)
                environmentDisplay = SWMPlugin.getInstance().getNms().getSlimeWorld(Bukkit.getWorld(worldName)) != null ?
                    ChatColor.GRAY + "[" + slimeWorld.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT) + "]" : ChatColor.GRAY + "[BUKKIT]";

            sender.send(ChatColor.GRAY + " - " + worldColor + worldName + " " + environmentDisplay);
        }
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
