package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.config.Messages;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.command.CommandSender;

public class CmdReload extends SubCommand {

    public CmdReload() {
        super("reload", new String[]{"reload", "rl"}, "dueltime4.admin", "/dt reload", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Config config = DuelTimePlugin.getInstance().getCfg();
        Messages messages = DuelTimePlugin.getInstance().getMsg();
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();

        config.reload();
        messages.reload();
        arenaService.loadAll();
        sender.sendMessage("DuelTime4 reloaded.");
    }
}
