package cn.valorin.dueltime.hook;

import cn.valorin.dueltime.DuelTimePlugin;
import cn.valorin.dueltime.cache.CacheManager;
import cn.valorin.dueltime.data.pojo.PlayerData;
import cn.valorin.dueltime.ranking.RankingManager;
import cn.valorin.dueltime.util.UtilFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DuelTimeExpansion extends PlaceholderExpansion {
    private final DuelTimePlugin plugin;

    public DuelTimeExpansion(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Valorin";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "dueltime";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";
        CacheManager cacheManager = DuelTimePlugin.getInstance().getCacheManager();
        PlayerData playerData = cacheManager.getPlayerDataCache().get(player.getName());
        if (playerData != null) {
            switch (identifier) {
                case "classic_win_number":
                    return "" + playerData.getArenaClassicWins();
                case "classic_loss_number":
                    return "" + playerData.getArenaClassicLoses();
                case "classic_draw_number":
                    return "" + playerData.getArenaClassicDraws();
                case "classic_win_rate":
                    return UtilFormat.round((playerData.getArenaClassicWins() / (double) (playerData.getArenaClassicLoses() != 0 ? playerData.getArenaClassicLoses() : 1)) * 100, 1) + "%";
                case "classic_game_number":
                    return "" + (playerData.getArenaClassicWins() + playerData.getArenaClassicLoses() + playerData.getArenaClassicDraws());
                case "classic_game_time":
                    return "" + playerData.getArenaClassicTime();
                case "total_game_number":
                    return "" + playerData.getTotalGameNumber();
                case "total_game_time":
                    return "" + playerData.getTotalGameTime();
                case "exp":
                    return "" + playerData.getExp();
                case "exp_to_next_level":
                    return "" + DuelTimePlugin.getInstance().getLevelManager().calculateRemainingExpForLevelUp(playerData.getExp());
                case "level":
                    return "" + DuelTimePlugin.getInstance().getLevelManager().getLevel(player.getName());
                case "tier":
                    return DuelTimePlugin.getInstance().getLevelManager().getTier(player.getName()).getTitle();
                case "point":
                    return "" + playerData.getPoint();
            }
        }
        RankingManager rankingManager = DuelTimePlugin.getInstance().getRankingManager();
        try {
            switch (identifier) {
                case "rank_classic_win_number":
                    return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_WIN_NUMBER.getId()).getRankString(player);
                case "rank_classic_win_rate":
                    return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_WIN_RATE.getId()).getRankString(player);
                case "rank_classic_game_time":
                    return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_GAME_TIME.getId()).getRankString(player);
                case "rank_classic_game_number":
                    return rankingManager.getRanking(RankingManager.InternalType.CLASSIC_GAME_NUMBER.getId()).getRankString(player);
                case "rank_total_game_number":
                    return rankingManager.getRanking(RankingManager.InternalType.TOTAL_GAME_NUMBER.getId()).getRankString(player);
                case "rank_total_game_time":
                    return rankingManager.getRanking(RankingManager.InternalType.TOTAL_GAME_TIME.getId()).getRankString(player);
                case "rank_exp":
                    return rankingManager.getRanking(RankingManager.InternalType.EXP.getId()).getRankString(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
