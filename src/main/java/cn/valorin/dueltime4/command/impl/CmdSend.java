package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.RequestService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class CmdSend extends SubCommand {

    public CmdSend() {
        super("send", new String[]{"send", "invite"}, null, "/dt send <player> <arenaId>", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /dt send <player> <arenaId>");
            return;
        }
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[0]);
            return;
        }
        RequestService requestService = DuelTimePlugin.getInstance().getRequestService();
        requestService.sendRequest(player, target, args[1]);
    }
}
