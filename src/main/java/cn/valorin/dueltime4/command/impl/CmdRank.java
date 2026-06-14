package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CmdRank extends SubCommand {

    public CmdRank() {
        super("rank", new String[]{"rank", "ranking", "top"}, null, "/dt rank", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PlayerService playerService = DuelTimePlugin.getInstance().getPlayerService();
        List<PlayerProfile> topList = playerService.getTopByExp(10);

        if (topList.isEmpty()) {
            sender.sendMessage("No ranking data available yet.");
            return;
        }

        sender.sendMessage("§6===== Top Players (by EXP) =====");
        int rank = 1;
        for (PlayerProfile profile : topList) {
            sender.sendMessage("§e#" + rank + " §f" + profile.getPlayerName()
                + " §7- EXP: §a" + String.format("%.1f", profile.getExp())
                + " §7Wins: §b" + profile.getClassicWins()
                + " §7Games: §f" + profile.getTotalGames());
            rank++;
        }
    }
}
