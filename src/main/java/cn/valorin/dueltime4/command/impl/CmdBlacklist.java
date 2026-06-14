package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.BlacklistService;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CmdBlacklist extends SubCommand {

    public CmdBlacklist() {
        super("blacklist", new String[]{"blacklist", "bl"}, "dueltime4.admin", "/dt blacklist <add|remove|list> [player]", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt blacklist <add|remove|list> [player]");
            return;
        }
        BlacklistService blacklistService = DuelTimePlugin.getInstance().getBlacklistService();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /dt blacklist add <player>");
                    return;
                }
                blacklistService.add(args[1], "Admin blacklist");
                sender.sendMessage("Added " + args[1] + " to blacklist.");
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /dt blacklist remove <player>");
                    return;
                }
                blacklistService.remove(args[1]);
                sender.sendMessage("Removed " + args[1] + " from blacklist.");
            }
            case "list" -> {
                List<String> list = blacklistService.list();
                if (list.isEmpty()) {
                    sender.sendMessage("Blacklist is empty.");
                    return;
                }
                sender.sendMessage("§6===== Blacklisted Players =====");
                for (String name : list) {
                    sender.sendMessage("§c" + name);
                }
            }
            default -> sender.sendMessage("Unknown action. Use: add, remove, list");
        }
    }
}
