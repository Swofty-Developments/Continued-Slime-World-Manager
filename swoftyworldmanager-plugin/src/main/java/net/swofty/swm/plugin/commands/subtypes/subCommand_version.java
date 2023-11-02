package net.swofty.swm.plugin.commands.subtypes;

import net.swofty.swm.api.utils.SlimeFormat;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.commands.CommandCooldown;
import net.swofty.swm.plugin.commands.CommandParameters;
import net.swofty.swm.plugin.commands.CommandSource;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

@CommandParameters(description = "Prints out version information", inGameOnly = false)
public class subCommand_version extends SWMCommand implements CommandCooldown {

    @Override
    public void run(CommandSource sender, String[] args) {
        sender.send((sender.getSender() instanceof Player ? Logging.COMMAND_PREFIX : Logging.CONSOLE_PREFIX) +
                ChatColor.GRAY + "This server is running SWM " + ChatColor.YELLOW + "v" + SWMPlugin.getInstance()
                        .getDescription().getVersion() + ChatColor.GRAY + ", which supports up to Slime Format " + ChatColor.AQUA + "v" + SlimeFormat.SLIME_VERSION + ChatColor.GRAY + ".");;
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }

    @Override
    public long cooldownSeconds() {
        return 1;
    }
}