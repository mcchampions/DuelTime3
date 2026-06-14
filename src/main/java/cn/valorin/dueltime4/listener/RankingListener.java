package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.event.RankingRefreshEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RankingListener implements Listener {

    @EventHandler
    public void onRefresh(RankingRefreshEvent e) {
        // Hologram update handled by HologramManager when wired
    }
}
