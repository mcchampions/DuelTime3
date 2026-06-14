package cn.valorin.dueltime4.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager implements CommandExecutor {

    private final Map<String, SubCommand> aliasMap = new HashMap<>();
    private final Set<SubCommand> commands = new LinkedHashSet<>();

    public CommandManager() {
        PluginCommand cmd = Bukkit.getPluginCommand("dueltime");
        if (cmd == null) throw new IllegalStateException("Command 'dueltime' not registered");
        cmd.setExecutor(this);
    }

    public void register(SubCommand cmd) {
        commands.add(cmd);
        aliasMap.put(cmd.getName().toLowerCase(), cmd);
        for (String alias : cmd.getAliases()) aliasMap.put(alias.toLowerCase(), cmd);
    }

    public Set<SubCommand> getCommands() { return commands; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0) {
            SubCommand help = aliasMap.get("help");
            if (help != null) help.execute(sender, args);
            return true;
        }
        SubCommand sub = aliasMap.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage("Unknown command. Use /dt help");
            return true;
        }
        if (sub.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage("Player only command.");
            return true;
        }
        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage("No permission.");
            return true;
        }
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }
}
