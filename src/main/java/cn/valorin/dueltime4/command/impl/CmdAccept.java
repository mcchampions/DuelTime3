package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.MatchService;
import cn.valorin.dueltime4.service.RequestService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public class CmdAccept extends SubCommand {

    public CmdAccept() {
        super("accept", new String[]{"accept", "acc"}, null, "/dt accept [playerName]", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        RequestService requestService = DuelTimePlugin.getInstance().getRequestService();
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        MatchService matchService = DuelTimePlugin.getInstance().getMatchService();

        String senderName = args.length > 0 ? args[0] : null;
        if (senderName == null) {
            player.sendMessage("Please specify the player name: /dt accept <player>");
            return;
        }

        // Validate BEFORE consuming the request
        var lookup = requestService.getRequest(senderName, player.getName());
        if (lookup.isEmpty()) {
            player.sendMessage("No pending request from " + senderName);
            return;
        }

        String arenaId = lookup.get().arenaId();
        Arena arena = arenaService.get(arenaId);
        if (arena == null) {
            player.sendMessage("Arena not found: " + arenaId);
            return;
        }

        if (arenaService.getByPlayer(player) != null) {
            player.sendMessage("You are already in a match. Use /dt quit first.");
            return;
        }

        Player senderPlayer = Bukkit.getPlayer(senderName);
        if (senderPlayer == null) {
            player.sendMessage("Player offline: " + senderName);
            return;
        }

        // Now consume the request
        requestService.accept(player, senderName);
        matchService.startMatch(arenaId, List.of(senderPlayer, player));
    }
}
