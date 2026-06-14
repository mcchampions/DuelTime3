package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.RequestService;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class CmdDecline extends SubCommand {

    public CmdDecline() {
        super("decline", new String[]{"decline", "dec"}, null, "/dt decline [playerName]", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt decline <playerName>");
            return;
        }
        RequestService requestService = DuelTimePlugin.getInstance().getRequestService();
        requestService.decline(player, args[0]);
        player.sendMessage("Declined request from " + args[0]);
    }
}
