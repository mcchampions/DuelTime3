package cn.valorin.dueltime4.event;

import cn.valorin.dueltime4.arena.Arena;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ArenaEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Arena arena;
    private final Map<String, Object> result;

    public ArenaEndEvent(Arena arena, Map<String, Object> result) {
        this.arena = arena;
        this.result = result;
    }

    public Arena getArena() {
        return arena;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
