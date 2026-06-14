package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.MigrationService;
import org.bukkit.command.CommandSender;

public class CmdMigrate extends SubCommand {

    public CmdMigrate() {
        super("migrate", new String[]{"migrate", "migration"}, "dueltime4.admin", "/dt migrate", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MigrationService migrationService = DuelTimePlugin.getInstance().getMigrationService();
        migrationService.run();
        sender.sendMessage("Migration started. Check console for details.");
    }
}
