package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.Location;

import java.util.*;

public class ClassicArena extends Arena {

    private Location pos1;
    private Location pos2;

    public ClassicArena(String id, String name, Location pos1, Location pos2) {
        super(id, name, "classic");
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public void setPositions(Location p1, Location p2) { this.pos1 = p1; this.pos2 = p2; }

    public String getOpponentName(String playerName) {
        return gamers.stream()
            .filter(g -> !g.getPlayerName().equals(playerName))
            .findFirst()
            .map(Gamer::getPlayerName)
            .orElse(null);
    }

    @Override
    public boolean contains(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        if (!pos1.getWorld().equals(loc.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
            && loc.getY() >= minY && loc.getY() <= maxY
            && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    @Override public int getMaxPlayers() { return 2; }
    @Override public int getMinPlayers() { return 2; }

    @Override
    public boolean canJoin(Gamer gamer) {
        if (gamers.size() >= 2) return false;
        if (gamers.size() == 1 && gamers.get(0).getPlayerName().equals(gamer.getPlayerName())) return false;
        return true;
    }

    @Override
    protected void onStart() {
        if (gamers.size() < 2) return;
        gamers.get(0).getPlayer().teleport(pos1);
        gamers.get(0).updateRecentLocation(pos1);
        gamers.get(1).getPlayer().teleport(pos2);
        gamers.get(1).updateRecentLocation(pos2);
        for (Gamer g : gamers) {
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
        long alive = gamers.stream().filter(g -> !g.isDead()).count();
        if (alive == 2) {
            result.put("reason", "DRAW");
        } else if (alive == 1) {
            Gamer winner = gamers.stream().filter(g -> !g.isDead()).findFirst().orElse(null);
            result.put("reason", "CLEAR");
            result.put("winner", winner);
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {}
}
