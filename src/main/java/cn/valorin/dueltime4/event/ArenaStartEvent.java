package cn.valorin.dueltime4.event;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Arena arena;
    private final List<Gamer> gamers;

    public ArenaStartEvent(Arena arena, List<Gamer> gamers) {
        this.arena = arena;
        this.gamers = gamers;
    }

    public Arena getArena() {
        return arena;
    }

    public List<Gamer> getGamers() {
        return gamers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
