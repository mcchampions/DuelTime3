package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.Spectator;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public abstract class Arena {

    protected String id;
    protected String name;
    protected String typeName;
    protected ArenaState state = ArenaState.WAITING;
    protected final List<Gamer> gamers = new ArrayList<>();
    protected final List<Spectator> spectators = new ArrayList<>();
    protected BukkitTask timer;

    public Arena(String id, String name, String typeName) {
        this.id = id;
        this.name = name;
        this.typeName = typeName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTypeName() { return typeName; }
    public ArenaState getState() { return state; }
    public List<Gamer> getGamers() { return Collections.unmodifiableList(gamers); }
    public List<Spectator> getSpectators() { return Collections.unmodifiableList(spectators); }

    public boolean isFull() { return gamers.size() >= getMaxPlayers(); }
    public abstract int getMaxPlayers();
    public abstract int getMinPlayers();

    public void setState(ArenaState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition from " + state + " to " + newState);
        }
        this.state = newState;
    }

    public abstract boolean canJoin(Gamer gamer);
    protected abstract void onStart();
    protected abstract void onTick(int secondsElapsed);
    protected abstract Map<String, Object> onEnd();
    protected abstract void onForceStop();

    /** Public wrappers for cross-package access */
    public void start() { onStart(); }
    public void tick(int secondsElapsed) { onTick(secondsElapsed); }
    public Map<String, Object> end() { return onEnd(); }
    public void forceStop() { onForceStop(); }

    /** Whether a world location falls within this arena's boundaries */
    public abstract boolean contains(Location loc);

    public void addGamer(Gamer gamer) { gamers.add(gamer); }
    public void removeGamer(String playerName) {
        gamers.removeIf(g -> g.getPlayerName().equals(playerName));
    }

    public void addSpectator(Spectator spectator) { spectators.add(spectator); }
    public void removeSpectator(String playerName) {
        spectators.removeIf(s -> s.getPlayerName().equals(playerName));
    }

    public Gamer getGamer(String playerName) {
        return gamers.stream().filter(g -> g.getPlayerName().equals(playerName)).findFirst().orElse(null);
    }

    public boolean hasGamer(String playerName) {
        return getGamer(playerName) != null;
    }

    public boolean hasSpectator(String playerName) {
        return spectators.stream().anyMatch(s -> s.getPlayerName().equals(playerName));
    }

    public void cancelTimer() {
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
    }

    public void setTimer(BukkitTask timer) { this.timer = timer; }
}
