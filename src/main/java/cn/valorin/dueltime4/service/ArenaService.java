package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.arena.*;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.Spectator;
import cn.valorin.dueltime4.repository.ArenaRepository;
import cn.valorin.dueltime4.repository.LocationRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaService {

    private final ArenaRepository repo;
    private final LocationRepository locationRepo;
    private final Map<String, Arena> activeArenas = new ConcurrentHashMap<>();
    private final Map<String, String> playerArenaMap = new ConcurrentHashMap<>();
    private final Map<String, String> spectatorArenaMap = new ConcurrentHashMap<>();
    private final Map<String, String> waitingMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> arenaWaitingList = new ConcurrentHashMap<>();

    public ArenaService(ArenaRepository repo, LocationRepository locationRepo) {
        this.repo = repo;
        this.locationRepo = locationRepo;
    }

    // --- Arena lifecycle ---

    public void loadAll() {
        activeArenas.clear();
        for (Map<String, Object> row : repo.findAll()) {
            Arena arena = buildArena(row);
            if (arena != null) {
                activeArenas.put(arena.getId(), arena);
            }
        }
    }

    private Arena buildArena(Map<String, Object> row) {
        String type = (String) row.get("type");
        String id = (String) row.get("id");
        String name = (String) row.get("name");
        String json = (String) row.get("data_json");
        // Full deserialization in Task 19. For now, create skeleton arena objects.
        return null;
    }

    public Arena get(String id) { return activeArenas.get(id); }
    public Arena getByPlayer(Player player) { return activeArenas.get(playerArenaMap.get(player.getName())); }
    public Arena getSpectating(Player player) { return activeArenas.get(spectatorArenaMap.get(player.getName())); }
    public List<Arena> getAll() { return new ArrayList<>(activeArenas.values()); }

    // --- Player-arena mappings ---

    public void addGamerMapping(String playerName, String arenaId) { playerArenaMap.put(playerName, arenaId); }
    public void removeGamerMapping(String playerName) { playerArenaMap.remove(playerName); }
    public void addSpectatorMapping(String playerName, String arenaId) { spectatorArenaMap.put(playerName, arenaId); }
    public void removeSpectatorMapping(String playerName) { spectatorArenaMap.remove(playerName); }

    // --- Waiting queue ---

    public void addToWaiting(Player player, String arenaId) {
        waitingMap.put(player.getName(), arenaId);
        arenaWaitingList.computeIfAbsent(arenaId, k -> new ArrayList<>()).add(player.getName());
    }

    public void removeFromWaiting(Player player) {
        String arenaId = waitingMap.remove(player.getName());
        if (arenaId != null) {
            List<String> list = arenaWaitingList.get(arenaId);
            if (list != null) list.remove(player.getName());
        }
    }

    public Arena getWaiting(Player player) {
        return activeArenas.get(waitingMap.get(player.getName()));
    }

    public List<String> getWaitingPlayers(String arenaId) {
        return arenaWaitingList.getOrDefault(arenaId, List.of());
    }

    // --- Lobby ---

    public Location getLobby() { return locationRepo.get("lobby").orElse(null); }
    public void setLobby(Location loc) { locationRepo.set("lobby", loc); }

    // --- Persistence ---

    public void saveArena(Arena arena, String dataJson) {
        String world = "";
        repo.save(arena.getId(), arena.getName(), arena.getTypeName(), world, dataJson);
        activeArenas.put(arena.getId(), arena);
    }

    public void setArenaEnabled(String id, boolean enabled) {
        repo.setEnabled(id, enabled);
        if (!enabled) activeArenas.remove(id);
    }

    public void deleteArena(String id) {
        repo.delete(id);
        activeArenas.remove(id);
    }
}
