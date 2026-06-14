package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.event.RankingRefreshEvent;
import cn.valorin.dueltime4.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class RankingService {

    private final PlayerService playerService;
    private int taskId = -1;

    public RankingService(PlayerService playerService) { this.playerService = playerService; }

    public void startAutoRefresh(int intervalSeconds) {
        new BukkitRunnable() {
            @Override
            public void run() { refresh(); }
        }.runTaskTimerAsynchronously(DuelTimePlugin.getInstance(), intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void refresh() {
        List<PlayerProfile> topList = playerService.getTopByExp(50);
        Bukkit.getPluginManager().callEvent(new RankingRefreshEvent(topList));
    }
}
