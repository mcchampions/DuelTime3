package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.Location;

import java.util.*;

public class TeamArena extends Arena {

    private final int teamSize;
    private Location team1Spawn;
    private Location team2Spawn;
    private final Map<String, Integer> playerTeam = new HashMap<>();

    public TeamArena(String id, String name, int teamSize, Location t1Spawn, Location t2Spawn) {
        super(id, name, "team");
        this.teamSize = teamSize;
        this.team1Spawn = t1Spawn;
        this.team2Spawn = t2Spawn;
    }

    @Override public int getMaxPlayers() { return teamSize * 2; }
    @Override public int getMinPlayers() { return 2; }

    public int getTeamSize() { return teamSize; }
    public int getTeam(String playerName) { return playerTeam.getOrDefault(playerName, -1); }

    @Override
    public boolean canJoin(Gamer gamer) {
        return gamers.size() < getMaxPlayers();
    }

    @Override
    public void addGamer(Gamer gamer) {
        super.addGamer(gamer);
        int team0Count = (int) playerTeam.values().stream().filter(t -> t == 0).count();
        int team1Count = (int) playerTeam.values().stream().filter(t -> t == 1).count();
        playerTeam.put(gamer.getPlayerName(), team0Count <= team1Count ? 0 : 1);
    }

    @Override
    public void removeGamer(String playerName) {
        super.removeGamer(playerName);
        playerTeam.remove(playerName);
    }

    @Override
    public boolean contains(Location loc) {
        // Team arenas use two spawn points as region corners
        if (team1Spawn == null || team2Spawn == null) return false;
        if (!team1Spawn.getWorld().equals(loc.getWorld())) return false;
        double minX = Math.min(team1Spawn.getX(), team2Spawn.getX()), maxX = Math.max(team1Spawn.getX(), team2Spawn.getX());
        double minY = Math.min(team1Spawn.getY(), team2Spawn.getY()), maxY = Math.max(team1Spawn.getY(), team2Spawn.getY());
        double minZ = Math.min(team1Spawn.getZ(), team2Spawn.getZ()), maxZ = Math.max(team1Spawn.getZ(), team2Spawn.getZ());
        // Expand bounds by 20 blocks for team arenas
        return loc.getX() >= minX - 20 && loc.getX() <= maxX + 20
            && loc.getY() >= minY - 5 && loc.getY() <= maxY + 5
            && loc.getZ() >= minZ - 20 && loc.getZ() <= maxZ + 20;
    }

    @Override
    protected void onStart() {
        for (Gamer g : gamers) {
            Location spawn = playerTeam.get(g.getPlayerName()) == 0 ? team1Spawn : team2Spawn;
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
        boolean team0Alive = gamers.stream().anyMatch(g -> playerTeam.get(g.getPlayerName()) == 0 && !g.isDead());
        boolean team1Alive = gamers.stream().anyMatch(g -> playerTeam.get(g.getPlayerName()) == 1 && !g.isDead());

        if (team0Alive && !team1Alive) {
            result.put("reason", "CLEAR");
            result.put("winner", 0);
        } else if (!team0Alive && team1Alive) {
            result.put("reason", "CLEAR");
            result.put("winner", 1);
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {}

    public Map<String, Integer> getTeams() { return Collections.unmodifiableMap(playerTeam); }
}
