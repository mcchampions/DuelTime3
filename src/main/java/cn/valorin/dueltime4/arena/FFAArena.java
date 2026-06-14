package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.Location;

import java.util.*;

public class FFAArena extends Arena {

    private final int minPlayers;
    private final int maxPlayers;
    private final List<Location> spawnPoints;

    public FFAArena(String id, String name, int minPlayers, int maxPlayers, List<Location> spawnPoints) {
        super(id, name, "ffa");
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.spawnPoints = spawnPoints;
    }

    @Override public int getMaxPlayers() { return maxPlayers; }
    @Override public int getMinPlayers() { return minPlayers; }

    @Override
    public boolean canJoin(Gamer gamer) {
        return gamers.size() < maxPlayers;
    }

    @Override
    public boolean contains(Location loc) {
        if (spawnPoints.isEmpty()) return false;
        // Use bounding box of all spawn points, expanded
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        for (Location sp : spawnPoints) {
            if (!sp.getWorld().equals(loc.getWorld())) return false;
            minX = Math.min(minX, sp.getX()); maxX = Math.max(maxX, sp.getX());
            minY = Math.min(minY, sp.getY()); maxY = Math.max(maxY, sp.getY());
            minZ = Math.min(minZ, sp.getZ()); maxZ = Math.max(maxZ, sp.getZ());
        }
        return loc.getX() >= minX - 15 && loc.getX() <= maxX + 15
            && loc.getY() >= minY - 10 && loc.getY() <= maxY + 10
            && loc.getZ() >= minZ - 15 && loc.getZ() <= maxZ + 15;
    }

    @Override
    protected void onStart() {
        Random rand = new Random();
        for (int i = 0; i < gamers.size(); i++) {
            Location spawn = spawnPoints.get(i % spawnPoints.size());
            Gamer g = gamers.get(i);
            g.getPlayer().teleport(spawn);
            g.updateRecentLocation(spawn);
            g.getPlayer().setHealth(g.getPlayer().getMaxHealth());
        }
    }

    @Override
    protected void onTick(int secondsElapsed) {
        for (Gamer g : gamers) {
            if (!g.isDead() && g.getPlayer().isDead()) {
                g.setDead(true);
            }
        }
    }

    @Override
    protected Map<String, Object> onEnd() {
        Map<String, Object> result = new HashMap<>();
        long aliveCount = gamers.stream().filter(g -> !g.isDead()).count();

        if (aliveCount == 1) {
            result.put("reason", "CLEAR");
            gamers.stream().filter(g -> !g.isDead()).findFirst().ifPresent(w -> result.put("winner", w));
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {}
}
