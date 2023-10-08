package com.grinderwolf.swm.plugin.command;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class SWMCommand implements CommandExecutor, TabCompleter {
    private static final Map<UUID, HashMap<SWMCommand, Long>> CMD_COOLDOWN = new HashMap<>();
    public static final String COMMAND_SUFFIX = "subCommand_";

    private final CommandParameters params;
    private final String name;
    private final String description;
    private final String usage;
    private final Boolean inGameOnly;
    private final List<String> aliases;
    private final String permission;

    private CommandSource sender;

    protected SWMCommand() {
        this.params = this.getClass().getAnnotation(CommandParameters.class);
        this.name = this.getClass().getSimpleName().replace(COMMAND_SUFFIX, "").toLowerCase();
        this.description = this.params.description();
        this.usage = this.params.usage();
        this.aliases = Arrays.asList(this.params.aliases().split(","));
        this.permission = this.params.permission();
        this.inGameOnly = this.params.inGameOnly();
    }

    public abstract void run(CommandSource sender, String[] args);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    public static void register() {
        SWMPlugin.getInstance().commandMap.register("", new SlimeCommandHandler());
    }

    public static class SlimeCommandHandler extends Command {

        public SWMCommand command;

        public SlimeCommandHandler() {
            super("swm", "Manage SlimeWorldManager", "", new ArrayList<>());
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (args.length == 0) {
                sender.sendMessage((sender instanceof Player) ?
                        Logging.COMMAND_PREFIX + "§bSlime World Manager §7is a plugin that implements the Slime Region Format. To check out the help page, type §e/swm help§7." :
                                Logging.CONSOLE_PREFIX + "Slime World Manager is a plugin that implements the Slime Region Format. To check out the help page, type /swm help.");
                return false;
            }

            for (SWMCommand swmCommand : CommandLoader.commands) {
                if (swmCommand.name.equals(args[0]) || swmCommand.aliases.contains(args[0])) {
                    this.command = swmCommand;
                }
            }

            if (this.command == null) {
                sender.sendMessage((sender instanceof Player) ?
                        Logging.COMMAND_PREFIX + " §cUnknown command. To check out the help page, type §e/swm help§c." :
                                Logging.CONSOLE_PREFIX + " Unknown command. To check out the help page, type /swm help.");
                return false;
            }

            if (this.command.inGameOnly && !(sender instanceof Player)) {
                sender.sendMessage(Logging.CONSOLE_PREFIX + " This command can only be run in-game.");
                return false;
            }

            command.sender = new CommandSource(sender);

            if (!command.permission.equals("") && !sender.hasPermission(command.permission)) {
                sender.sendMessage("§cYou do not have permission to perform this command.");
                return false;
            }

            if (command instanceof CommandCooldown && sender instanceof Player) {
                HashMap<SWMCommand, Long> cooldowns = new HashMap<>();
                if (CMD_COOLDOWN.containsKey(((Player) sender).getUniqueId())) {
                    cooldowns = CMD_COOLDOWN.get(((Player) sender).getUniqueId());
                    if (cooldowns.containsKey(command)) {
                        if (System.currentTimeMillis() - cooldowns.get(command) < ((CommandCooldown) command).getCooldown()) {
                            sender.sendMessage("§cYou are on cooldown for $SECONDSs.".replace("$SECONDS", String.valueOf((double) (System.currentTimeMillis() - cooldowns.get(command)) / 1000)));
                            return false;
                        }
                    }
                }
                cooldowns.put(command, System.currentTimeMillis() + ((CommandCooldown) command).getCooldown());
                CMD_COOLDOWN.put(((Player) sender).getUniqueId(), cooldowns);
            }

            command.run(command.sender, args);
            return false;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (args.length <= 1) {
                List<String> list = new ArrayList<>();
                CommandLoader.commands.stream().forEach(entry -> list.add(entry.name));
                return list;
            } else {
                for (SWMCommand swmCommand : CommandLoader.commands) {
                    if (swmCommand.name.equals(args[0]) || swmCommand.aliases.contains(args[0])) {
                        this.command = swmCommand;
                        return swmCommand.tabCompleters(sender, alias, args);
                    }
                }

                this.command = null;
                return new ArrayList<>();
            }
        }
    }

    public abstract List<String> tabCompleters(CommandSender sender, String alias, String[] args);

    public void send(String message, CommandSource sender) {
        sender.send(ChatColor.GRAY + message.replace("&", "§"));
    }

    public void send(String message) {
        send(ChatColor.translateAlternateColorCodes('&', message), sender);
    }
}