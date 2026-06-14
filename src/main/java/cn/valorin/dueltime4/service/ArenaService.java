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

        return switch (type) {
            case "classic" -> {
                var p1 = parseLocation(json, "pos1");
                var p2 = parseLocation(json, "pos2");
                yield new ClassicArena(id, name, p1, p2);
            }
            case "team" -> {
                int size = extractInt(json, "team_size", 2);
                var t1 = parseLocation(json, "t1_spawn");
                var t2 = parseLocation(json, "t2_spawn");
                yield new TeamArena(id, name, size, t1, t2);
            }
            case "ffa" -> {
                int min = extractInt(json, "min_players", 3);
                int max = extractInt(json, "max_players", 8);
                List<Location> spawns = parseLocationList(json, "spawns");
                yield new FFAArena(id, name, min, max, spawns);
            }
            default -> null;
        };
    }

    private Location parseLocation(String json, String key) {
        String worldName = extractString(json, "world", "world");
        double x = extractDouble(json, key + "\":{\"x\"", 0);
        double y = extractDouble(json, key + "\":{\"y\"", 0);
        double z = extractDouble(json, key + "\":{\"z\"", 0);
        float yaw = (float) extractDouble(json, key + "\":{\"yaw\"", 0);
        float pitch = (float) extractDouble(json, key + "\":{\"pitch\"", 0);
        var world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private List<Location> parseLocationList(String json, String key) {
        List<Location> list = new ArrayList<>();
        int idx = 0;
        while (true) {
            if (!json.contains("\"" + idx + "\":{\"x\"")) break;
            double x = extractDouble(json, "\"" + idx + "\":{\"x\"", 0);
            double y = extractDouble(json, "\"" + idx + "\":{\"y\"", 0);
            double z = extractDouble(json, "\"" + idx + "\":{\"z\"", 0);
            var world = Bukkit.getWorlds().get(0);
            list.add(new Location(world, x, y, z));
            idx++;
        }
        if (list.isEmpty()) {
            // Fallback: give a default spawn at origin
            list.add(new Location(Bukkit.getWorlds().get(0), 0, 64, 0));
        }
        return list;
    }

    private double extractDouble(String json, String search, double def) {
        int idx = json.indexOf(search);
        if (idx < 0) return def;
        idx = json.indexOf(":", idx) + 1;
        int end = json.indexOf(",", idx);
        if (end < 0) end = json.indexOf("}", idx);
        if (end < 0) return def;
        try { return Double.parseDouble(json.substring(idx, end).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private int extractInt(String json, String key, int def) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return def;
        idx += search.length();
        int end = json.indexOf(",", idx);
        if (end < 0) end = json.indexOf("}", idx);
        if (end < 0) return def;
        try { return Integer.parseInt(json.substring(idx, end).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private String extractString(String json, String key, String def) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return def;
        idx += search.length();
        int end = json.indexOf("\"", idx);
        if (end < 0) return def;
        return json.substring(idx, end);
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
        if (enabled) {
            repo.findById(id).ifPresent(row -> {
                Arena arena = buildArena(row);
                if (arena != null) activeArenas.put(id, arena);
            });
        } else {
            activeArenas.remove(id);
        }
    }

    public void deleteArena(String id) {
        repo.delete(id);
        activeArenas.remove(id);
    }
}
