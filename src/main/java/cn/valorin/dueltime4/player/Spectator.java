package cn.valorin.dueltime4.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Spectator {

    private final Player player;
    private final Location originalLocation;
    private final GameMode originalGameMode;

    public Spectator(Player player) {
        this.player = player;
        this.originalLocation = player.getLocation().clone();
        this.originalGameMode = player.getGameMode();
    }

    public Player getPlayer() { return player; }
    public String getPlayerName() { return player.getName(); }
    public Location getOriginalLocation() { return originalLocation; }
    public GameMode getOriginalGameMode() { return originalGameMode; }
}
