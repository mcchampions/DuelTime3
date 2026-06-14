package cn.valorin.dueltime4.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MigrationCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final int arenas;
    private final int players;
    private final int records;

    public MigrationCompleteEvent(int arenas, int players, int records) {
        this.arenas = arenas;
        this.players = players;
        this.records = records;
    }

    public int getArenas() {
        return arenas;
    }

    public int getPlayers() {
        return players;
    }

    public int getRecords() {
        return records;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
