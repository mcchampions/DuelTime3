package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.command.CommandManager;
import cn.valorin.dueltime4.command.SubCommand;
import org.bukkit.command.CommandSender;

public class CmdHelp extends SubCommand {

    private final CommandManager commandManager;

    public CmdHelp(CommandManager commandManager) {
        super("help", new String[]{"help", "?"}, null, "/dt help", false);
        this.commandManager = commandManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6===== DuelTime4 Commands =====");
        for (SubCommand cmd : commandManager.getCommands()) {
            sender.sendMessage("§e" + cmd.getUsage() + " §7- " + (cmd.getPermission() != null ? "§c[admin] " : ""));
        }
    }
}
