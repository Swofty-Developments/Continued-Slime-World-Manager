package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

@CommandParameters(description = "Prints to the player where they are", inGameOnly = true)
public class subCommand_whereami extends SWMCommand {
    @Override
    public void run(CommandSource sender, String[] args) {
        sender.send(Logging.COMMAND_PREFIX + ChatColor.GRAY + " You are currently in " +
                ChatColor.GREEN + sender.getPlayer().getWorld().getName() + ChatColor.GRAY + "!");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
