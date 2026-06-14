package cn.valorin.dueltime4.hook;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class DuelTimePlaceholderExpansion extends PlaceholderExpansion {

    private final PlayerService playerService;

    public DuelTimePlaceholderExpansion(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override public @NotNull String getIdentifier() { return "dueltime"; }
    @Override public @NotNull String getAuthor() { return "valorin"; }
    @Override public @NotNull String getVersion() { return "4.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        PlayerProfile p = playerService.getOrCreate(player.getName());
        return switch (params.toLowerCase()) {
            case "exp" -> String.format("%.1f", p.getExp());
            case "point" -> String.valueOf(p.getPoint());
            case "wins" -> String.valueOf(p.getClassicWins());
            case "loses" -> String.valueOf(p.getClassicLoses());
            case "draws" -> String.valueOf(p.getClassicDraws());
            case "total_games" -> String.valueOf(p.getTotalGames());
            case "win_streak" -> String.valueOf(p.getWinStreak());
            case "max_win_streak" -> String.valueOf(p.getMaxWinStreak());
            default -> null;
        };
    }
}
