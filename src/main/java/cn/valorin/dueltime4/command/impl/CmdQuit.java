package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.SpectateService;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class CmdQuit extends SubCommand {

    public CmdQuit() {
        super("quit", new String[]{"quit", "q", "leave"}, null, "/dt quit", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        SpectateService spectateService = DuelTimePlugin.getInstance().getSpectateService();

        String playerName = player.getName();

        // Check if spectating
        Arena spectArena = arenaService.getSpectating(player);
        if (spectArena != null) {
            spectateService.stopSpectating(player);
            player.sendMessage("You left spectating mode.");
            return;
        }

        // Check if in waiting queue
        Arena waitingArena = arenaService.getWaiting(player);
        if (waitingArena != null) {
            arenaService.removeFromWaiting(player);
            player.sendMessage("You left the waiting queue.");
            return;
        }

        // Check if in a match
        Arena matchArena = arenaService.getByPlayer(player);
        if (matchArena != null) {
            player.sendMessage("You are in a match. Use /dt stop to force-stop it.");
            return;
        }

        player.sendMessage("You are not in any arena, waiting queue, or spectating.");
    }
}
