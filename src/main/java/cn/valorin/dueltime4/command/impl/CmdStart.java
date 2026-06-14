package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.MatchService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CmdStart extends SubCommand {

    public CmdStart() {
        super("start", new String[]{"start", "begin"}, "dueltime4.admin", "/dt start <arenaId>", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt start <arenaId>");
            return;
        }
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        MatchService matchService = DuelTimePlugin.getInstance().getMatchService();

        Arena arena = arenaService.get(args[0]);
        if (arena == null) {
            sender.sendMessage("Arena not found: " + args[0]);
            return;
        }

        List<String> waitingNames = arenaService.getWaitingPlayers(args[0]);
        if (waitingNames.isEmpty()) {
            sender.sendMessage("No players in waiting queue for arena: " + args[0]);
            return;
        }

        List<Player> players = new ArrayList<>();
        for (String name : waitingNames) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) players.add(p);
        }

        if (players.isEmpty()) {
            sender.sendMessage("No online players in waiting queue.");
            return;
        }

        matchService.startMatch(args[0], players);
        sender.sendMessage("Force-started match in arena: " + args[0]);
    }
}
