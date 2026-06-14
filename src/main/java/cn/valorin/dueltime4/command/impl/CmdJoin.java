package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.BlacklistService;
import cn.valorin.dueltime4.service.MatchService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CmdJoin extends SubCommand {

    public CmdJoin() {
        super("join", new String[]{"join", "j"}, null, "/dt join <arenaId>", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt join <arenaId>");
            return;
        }
        Player player = (Player) sender;
        BlacklistService blacklistService = DuelTimePlugin.getInstance().getBlacklistService();
        if (blacklistService.isBlacklisted(player.getName())) {
            player.sendMessage("You are blacklisted and cannot join arenas.");
            return;
        }
        String arenaId = args[0];
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        Arena arena = arenaService.get(arenaId);
        if (arena == null) {
            player.sendMessage("Arena not found: " + arenaId);
            return;
        }
        if (arena.getState() != ArenaState.WAITING) {
            player.sendMessage("This arena is not accepting players right now.");
            return;
        }
        if (arenaService.getByPlayer(player) != null) {
            player.sendMessage("You are already in an arena. Use /dt quit first.");
            return;
        }
        if (arenaService.getWaiting(player) != null) {
            player.sendMessage("You are already waiting for an arena. Use /dt quit first.");
            return;
        }
        arenaService.addToWaiting(player, arenaId);
        player.sendMessage("You joined the waiting queue for: " + arena.getName());

        // Auto-start if enough players are waiting
        List<String> waitingNames = arenaService.getWaitingPlayers(arenaId);
        if (waitingNames.size() >= arena.getMinPlayers()) {
            List<Player> toStart = new ArrayList<>();
            for (String name : waitingNames) {
                if (toStart.size() >= arena.getMaxPlayers()) break;
                Player p = Bukkit.getPlayerExact(name);
                if (p != null) toStart.add(p);
            }
            if (toStart.size() >= arena.getMinPlayers()) {
                MatchService matchService = DuelTimePlugin.getInstance().getMatchService();
                matchService.startMatch(arenaId, toStart);
            }
        }
    }
}
