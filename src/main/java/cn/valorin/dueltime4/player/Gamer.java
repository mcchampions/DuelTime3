package cn.valorin.dueltime4.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Gamer {

    private final Player player;
    private final Location originalLocation;
    private final GameMode originalGameMode;
    private Location recentLocation;
    private double totalDamage;
    private double maxDamage;
    private int hitCount;
    private boolean dead;
    private String result; // WIN, LOSE, DRAW

    public Gamer(Player player) {
        this.player = player;
        this.originalLocation = player.getLocation().clone();
        this.originalGameMode = player.getGameMode();
        this.recentLocation = player.getLocation().clone();
    }

    public Player getPlayer() { return player; }
    public String getPlayerName() { return player.getName(); }
    public Location getOriginalLocation() { return originalLocation; }
    public GameMode getOriginalGameMode() { return originalGameMode; }

    public void updateRecentLocation(Location loc) { this.recentLocation = loc.clone(); }
    public Location getRecentLocation() { return recentLocation; }

    public void recordHit(double damage) {
        hitCount++;
        totalDamage += damage;
        if (damage > maxDamage) maxDamage = damage;
    }

    public double getTotalDamage() { return totalDamage; }
    public double getMaxDamage() { return maxDamage; }
    public int getHitCount() { return hitCount; }

    public void setDead(boolean dead) { this.dead = dead; }
    public boolean isDead() { return dead; }

    public void setResult(String result) { this.result = result; }
    public String getResult() { return result; }
}
