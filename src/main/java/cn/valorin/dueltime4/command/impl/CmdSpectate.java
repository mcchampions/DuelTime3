package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.SpectateService;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class CmdSpectate extends SubCommand {

    public CmdSpectate() {
        super("spectate", new String[]{"spectate", "spec", "watch"}, null, "/dt spectate <arenaId>", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt spectate <arenaId>");
            return;
        }
        Player player = (Player) sender;
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        SpectateService spectateService = DuelTimePlugin.getInstance().getSpectateService();

        Arena arena = arenaService.get(args[0]);
        if (arena == null) {
            player.sendMessage("Arena not found: " + args[0]);
            return;
        }

        if (!spectateService.canSpectate(player, arena)) {
            player.sendMessage("Cannot spectate this arena. It may not be in progress or you are already in it.");
            return;
        }

        spectateService.startSpectating(player, arena);
        player.sendMessage("Now spectating arena: " + arena.getName());
    }
}
