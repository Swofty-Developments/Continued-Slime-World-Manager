package net.swofty.swm.plugin.log;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logging {

    public static final String COMMAND_PREFIX = ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "CSWM " + ChatColor.GRAY + ">> ";
    public static final String CONSOLE_PREFIX = ChatColor.LIGHT_PURPLE + "[CSWM] ";

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(CONSOLE_PREFIX + ChatColor.GRAY + message);
    }

    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(CONSOLE_PREFIX + ChatColor.YELLOW + message);
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(CONSOLE_PREFIX + ChatColor.RED + message);
    }
}
