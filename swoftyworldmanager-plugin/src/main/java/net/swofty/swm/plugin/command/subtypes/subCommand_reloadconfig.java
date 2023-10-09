package net.swofty.swm.plugin.command.subtypes;

import net.swofty.swm.plugin.command.CommandCooldown;
import net.swofty.swm.plugin.command.CommandParameters;
import net.swofty.swm.plugin.command.CommandSource;
import net.swofty.swm.plugin.command.SWMCommand;
import net.swofty.swm.plugin.config.ConfigManager;
import net.swofty.swm.plugin.log.Logging;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.List;

@CommandParameters(description = "Reloads the config files", inGameOnly = false, permission = "swm.reload")
public class subCommand_reloadconfig extends SWMCommand implements CommandCooldown {

    @Override
    public long cooldownSeconds() {
        return 3;
    }

    @Override
    public void run(CommandSource sender, String[] args) {
        try {
            ConfigManager.initialize();
        } catch (IOException | ObjectMappingException ex) {
            if (!(sender.getSender() instanceof ConsoleCommandSender)) {
                sender.send(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to reload the config file. Take a look at the server console for more information.");
            }

            Logging.error("Failed to load config files:");
            ex.printStackTrace();
            return;
        }

        sender.send(Logging.COMMAND_PREFIX + ChatColor.GREEN + "Config reloaded.");
    }

    @Override
    public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
