package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import org.bukkit.command.CommandSender;

public class CmdLevel extends SubCommand {

    public CmdLevel() {
        super("level", new String[]{"level", "lv", "tier"}, null, "/dt level [player]", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String targetName = args.length > 0 ? args[0] : sender.getName();
        PlayerService playerService = DuelTimePlugin.getInstance().getPlayerService();
        PlayerProfile profile = playerService.getOrCreate(targetName);

        double exp = profile.getExp();
        // Simple level formula: level = sqrt(exp / 10)
        int level = Math.max(1, (int) Math.sqrt(exp / 10.0));
        double nextLevelExp = (level + 1) * (level + 1) * 10.0;
        double currentLevelExp = level * level * 10.0;
        double progress = exp - currentLevelExp;
        double needed = nextLevelExp - currentLevelExp;
        double percent = Math.min(100.0, (progress / needed) * 100.0);

        sender.sendMessage("§6===== Player Info: " + targetName + " =====");
        sender.sendMessage("§eLevel: §f" + level + " §7(EXP: §a" + String.format("%.1f", exp) + "§7)");
        sender.sendMessage("§eProgress: §f" + String.format("%.1f", percent) + "% to level " + (level + 1));
        sender.sendMessage("§ePoints: §6" + profile.getPoint());
        sender.sendMessage("§eWins: §a" + profile.getClassicWins()
            + " §eLoses: §c" + profile.getClassicLoses()
            + " §eDraws: §e" + profile.getClassicDraws());
        sender.sendMessage("§eWin Streak: §b" + profile.getWinStreak() + " §7(Best: " + profile.getMaxWinStreak() + ")");
        sender.sendMessage("§eTotal Games: §f" + profile.getTotalGames() + " §7Total Time: §f" + formatTime(profile.getTotalTime()));
    }

    private String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
