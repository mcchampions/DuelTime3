package cn.valorin.dueltime4.event;

import cn.valorin.dueltime4.player.PlayerProfile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RankingRefreshEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final List<PlayerProfile> topList;

    public RankingRefreshEvent(List<PlayerProfile> topList) {
        this.topList = topList;
    }

    public List<PlayerProfile> getTopList() {
        return topList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
