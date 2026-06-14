package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.MatchService;
import org.bukkit.command.CommandSender;

public class CmdStop extends SubCommand {

    public CmdStop() {
        super("stop", new String[]{"stop", "end"}, "dueltime4.admin", "/dt stop <arenaId>", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt stop <arenaId>");
            return;
        }
        MatchService matchService = DuelTimePlugin.getInstance().getMatchService();
        matchService.forceStop(args[0], "Admin forced stop");
        sender.sendMessage("Force-stopped match in arena: " + args[0]);
    }
}
