package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.config.Messages;
import org.bukkit.command.CommandSender;

public class CmdLang extends SubCommand {

    public CmdLang() {
        super("lang", new String[]{"lang", "language"}, null, "/dt lang <language>", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /dt lang <language>");
            return;
        }
        Config config = DuelTimePlugin.getInstance().getCfg();
        Messages messages = DuelTimePlugin.getInstance().getMsg();

        String lang = args[0];
        config.set("core.language", lang);
        messages.reload();
        sender.sendMessage("Language changed to: " + lang);
    }
}
