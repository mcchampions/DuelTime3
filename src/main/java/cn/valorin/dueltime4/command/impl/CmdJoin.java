package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.BlacklistService;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

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
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        Arena arena = arenaService.get(args[0]);
        if (arena == null) {
            player.sendMessage("Arena not found: " + args[0]);
            return;
        }
        arenaService.addToWaiting(player, args[0]);
        player.sendMessage("You joined the waiting queue for: " + arena.getName());
    }
}
