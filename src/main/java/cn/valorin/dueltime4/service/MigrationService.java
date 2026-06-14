package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.event.MigrationCompleteEvent;
import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.repository.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationService {

    private final DatabaseManager db;
    private final Config config;
    private final ArenaRepository arenaRepo;
    private final PlayerRepository playerRepo;
    private final RecordRepository recordRepo;
    private final LocationRepository locationRepo;
    private final BlacklistRepository blacklistRepo;
    private final Logger log = Bukkit.getLogger();

    // DT3 location format: "DUELTIME LOCATION world,x,y,z,yaw,pitch"
    private static final Pattern LOC_PATTERN =
        Pattern.compile("DUELTIME LOCATION (.+?),(-?[\\d.]+),(-?[\\d.]+),(-?[\\d.]+),(-?[\\d.]+),(-?[\\d.]+)");

    private int arenaCount, playerCount, recordCount, shopItemCount;

    public MigrationService(DatabaseManager db, Config config, ArenaRepository arenaRepo,
                            PlayerRepository playerRepo, RecordRepository recordRepo,
                            LocationRepository locationRepo, BlacklistRepository blacklistRepo) {
        this.db = db;
        this.config = config;
        this.arenaRepo = arenaRepo;
        this.playerRepo = playerRepo;
        this.recordRepo = recordRepo;
        this.locationRepo = locationRepo;
        this.blacklistRepo = blacklistRepo;
    }

    public void run() {
        log.info("[DuelTime4] Starting migration from DuelTime3...");
        String oldType = config.getString("migration.old-database.type", "sqlite");
        String jdbcUrl;
        String user = null, pass = null;

        if ("mysql".equalsIgnoreCase(oldType)) {
            String host = config.getString("migration.old-database.mysql.host", "localhost");
            int port = config.getInt("migration.old-database.mysql.port", 3306);
            String dbName = config.getString("migration.old-database.mysql.database", "dueltime");
            user = config.getString("migration.old-database.mysql.username", "root");
            pass = config.getString("migration.old-database.mysql.password", "");
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false";
        } else {
            String path = config.getString("migration.old-database.sqlite.path", "plugins/DuelTime/sqlite.db");
            File f = new File(path);
            if (!f.isAbsolute()) {
                // Resolve relative to server root (plugins/ folder parent)
                f = new File(config.getPlugin().getDataFolder().getParentFile().getParentFile(), path);
            }
            jdbcUrl = "jdbc:sqlite:" + f.getAbsolutePath();
        }

        // Migrate config values from old config.yml
        try { migrateConfigValues(); } catch (Exception e) { log.warning("Config migration error: " + e.getMessage()); }

        log.info("[DuelTime4] Connecting to old database: " + jdbcUrl);
        try (Connection oldConn = user != null ?
                DriverManager.getConnection(jdbcUrl, user, pass) :
                DriverManager.getConnection(jdbcUrl);
             SqlHelper oldDb = new SqlHelper(oldConn)) {

            db.withTransaction(newDb -> {
                try { migratePlayers(oldDb); } catch (Exception e) { log.warning("Player migration error: " + e.getMessage()); e.printStackTrace(); }
                try { migrateArenas(oldDb); } catch (Exception e) { log.warning("Arena migration error: " + e.getMessage()); e.printStackTrace(); }
                try { migrateRecords(oldDb); } catch (Exception e) { log.warning("Record migration error: " + e.getMessage()); e.printStackTrace(); }
                try { migrateLocations(oldDb); } catch (Exception e) { log.warning("Location migration error: " + e.getMessage()); e.printStackTrace(); }
                try { migrateBlacklist(oldDb); } catch (Exception e) { log.warning("Blacklist migration error: " + e.getMessage()); e.printStackTrace(); }
                try { migrateShopItems(oldDb); } catch (Exception e) { log.warning("Shop migration error: " + e.getMessage()); e.printStackTrace(); }
                return null;
            });

            config.set("migration.enabled", false);
            config.reload();
            log.info("[DuelTime4] Migration done: " + arenaCount + " arenas, "
                + playerCount + " players, " + recordCount + " records, "
                + shopItemCount + " shop items");
            Bukkit.getPluginManager().callEvent(new MigrationCompleteEvent(arenaCount, playerCount, recordCount));

        } catch (SQLException e) {
            log.severe("[DuelTime4] Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Players: dueltime_playerdata ───

    private void migratePlayers(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM dueltime_playerdata", rs -> {
            String name = rs.getString("id"); // DT3 uses 'id' for player name
            PlayerProfile p = new PlayerProfile(name);
            try { p.setExp(rs.getDouble("exp")); } catch (Exception ignored) {}
            try { p.setPoint((int) rs.getDouble("point")); } catch (Exception ignored) {}
            try { p.setTotalGames(rs.getInt("total_game_number")); } catch (Exception ignored) {}
            try { p.setTotalTime(rs.getInt("total_game_time")); } catch (Exception ignored) {}
            try { p.setClassicWins(rs.getInt("arena_classic_wins")); } catch (Exception ignored) {}
            try { p.setClassicLoses(rs.getInt("arena_classic_loses")); } catch (Exception ignored) {}
            try { p.setClassicDraws(rs.getInt("arena_classic_draws")); } catch (Exception ignored) {}
            playerRepo.upsert(p);
            playerCount++;
            return null;
        });
    }

    // ─── Arenas: dueltime_arena_classic ───

    private void migrateArenas(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM dueltime_arena_classic", rs -> {
            String id = rs.getString("id");
            String name = rs.getString("name");
            Location p1 = parseDt3Location(rs.getString("player_location_1"));
            Location p2 = parseDt3Location(rs.getString("player_location_2"));
            String world = (p1 != null && p1.getWorld() != null) ? p1.getWorld().getName() : "world";

            String dataJson = buildArenaJson(world, p1, p2);
            arenaRepo.save(id, name, "classic", world, dataJson);
            arenaCount++;
            return null;
        });
    }

    private String buildArenaJson(String world, Location p1, Location p2) {
        if (p1 == null) p1 = new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
        if (p2 == null) p2 = new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
        return String.format(
            "{\"world\":\"%s\",\"pos1\":{\"x\":%f,\"y\":%f,\"z\":%f},\"pos2\":{\"x\":%f,\"y\":%f,\"z\":%f}}",
            world, p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
    }

    // ─── Records: dueltime_arena_record_classic ───

    private void migrateRecords(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM dueltime_arena_record_classic", rs -> {
            recordRepo.insert(
                rs.getString("player_name"), rs.getString("arena_id"), "classic",
                rs.getString("opponent_name"), rs.getString("result"),
                rs.getInt("time"), rs.getDouble("exp_change"),
                rs.getInt("hit_time"), rs.getDouble("total_damage"),
                rs.getDouble("max_damage"), rs.getDouble("average_damage"),
                rs.getString("date"));
            recordCount++;
            return null;
        });
    }

    // ─── Locations: dueltime_location ───

    private void migrateLocations(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM dueltime_location", rs -> {
            String dt3Id = rs.getString("id"); // e.g. "dueltime:lobby"
            String locStr = rs.getString("location");
            Location loc = parseDt3Location(locStr);
            if (loc == null) return null;

            // Map DT3 keys to DT4 keys
            String dt4Key = dt3Id;
            if ("dueltime:lobby".equals(dt3Id)) dt4Key = "lobby";

            locationRepo.set(dt4Key, loc);
            return null;
        });
    }

    // ─── Blacklist: dueltime_blacklist ───

    private void migrateBlacklist(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM dueltime_blacklist", rs -> {
            blacklistRepo.add(rs.getString("id"), "");
            return null;
        });
    }

    // ─── Shop: dueltime_shop ───

    private void migrateShopItems(SqlHelper oldDb) {
        var items = new ArrayList<LinkedHashMap<String, Object>>();
        oldDb.query("SELECT * FROM dueltime_shop", rs -> {
            var item = new LinkedHashMap<String, Object>();
            String itemId = String.valueOf(shopItemCount);
            try { itemId = String.valueOf(rs.getInt("id")); } catch (Exception ignored) {}
            int cost = 1;
            try { cost = (int) rs.getDouble("point"); } catch (Exception ignored) {}

            List<String> commands = new ArrayList<>();
            try {
                String cmds = rs.getString("commands");
                if (cmds != null && !cmds.isEmpty()) {
                    commands = new ArrayList<>(List.of(cmds.split(";")));
                }
            } catch (Exception ignored) {}

            String material = "STONE";
            int amount = 1;
            String displayName = null;
            List<String> lore = new ArrayList<>();
            try {
                String base64 = rs.getString("item_stack");
                if (base64 != null && !base64.isEmpty()) {
                    byte[] data = Base64.getDecoder().decode(base64);
                    try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
                        Object obj = ois.readObject();
                        if (obj instanceof ItemStack stack && stack.getType() != Material.AIR) {
                            material = stack.getType().name();
                            amount = stack.getAmount();
                            if (stack.hasItemMeta()) {
                                var meta = stack.getItemMeta();
                                displayName = LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName());
                                var lores = meta.lore();
                                if (lores != null && !lores.isEmpty()) {
                                    for (var c : lores) {
                                        lore.add(LegacyComponentSerializer.legacyAmpersand().serialize(c));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to deserialize shop item " + itemId + ": " + e.getMessage());
            }

            item.put("id", itemId);
            item.put("material", material);
            item.put("amount", amount);
            if (displayName != null) item.put("name", displayName);
            item.put("lore", lore);
            item.put("cost", cost);
            item.put("commands", commands);
            items.add(item);
            shopItemCount++;
            return null;
        });
        if (!items.isEmpty()) {
            config.set("shop.items", items);
        }
    }

    // ─── Location parsing ───

    // ─── Config migration ───

    private void migrateConfigValues() {
        String folder = config.getString("migration.old-plugin-folder", "plugins/DuelTime");
        File oldConfigFile = new File(folder, "config.yml");
        if (!oldConfigFile.isAbsolute()) {
            oldConfigFile = new File(config.getPlugin().getDataFolder().getParentFile().getParentFile(), folder + "/config.yml");
        }
        if (!oldConfigFile.exists()) {
            log.warning("Old config.yml not found at: " + oldConfigFile.getAbsolutePath());
            return;
        }
        YamlConfiguration old = YamlConfiguration.loadConfiguration(oldConfigFile);

        // Map DT3 → DT4 config paths
        copyIfSet(old, "Message.prefix", "core.prefix");
        copyIfSet(old, "Arena.classic.reward.win-exp", "arena.defaults.classic.reward.win-exp");
        copyIfSet(old, "Arena.classic.reward.win-point", "arena.defaults.classic.reward.win-point");
        copyIfSet(old, "Arena.classic.reward.lose-exp-rate", "arena.defaults.classic.reward.lose-exp-rate");
        copyIfSet(old, "Arena.classic.auto-respawn.enabled", "arena.defaults.classic.auto-respawn");
        copyIfSet(old, "Arena.classic.delayed-back.time", "arena.defaults.classic.delayed-back");

        copyIfSet(old, "Ranking.auto-refresh-interval", "ranking.refresh-seconds");
        copyIfSet(old, "Ranking.hologram.enabled", "ranking.hologram.enabled");
        copyIfSet(old, "Ranking.hologram.size", "ranking.hologram.max-size");

        copyIfSet(old, "Record.show.cooldown", "record.show-cooldown");
        copyIfSet(old, "Record.print.cost", "record.print-cost");

        // Migrate level tiers
        if (old.contains("Level.tier.showed-in-chat-box.format")) {
            config.set("level.chat-prefix", old.getString("Level.tier.showed-in-chat-box.format"));
        }
        if (old.contains("Level.tier.default")) {
            String title = old.getString("Level.tier.default.title", "&7无段位");
            int expNext = old.getInt("Level.tier.default.exp-for-level-up", 10);
            config.set("level.tiers", List.of(
                Map.of("level", 0, "title", title, "exp-to-next", expNext)
            ));
        }
        // Migrate custom tiers
        if (old.contains("Level.tier.custom")) {
            var customSec = old.getConfigurationSection("Level.tier.custom");
            if (customSec != null) {
                var tiers = new ArrayList<Map<String, Object>>();
                // Add default tier
                String defTitle = old.getString("Level.tier.default.title", "&7无段位");
                int defExp = old.getInt("Level.tier.default.exp-for-level-up", 10);
                tiers.add(Map.of("level", 0, "title", (Object) defTitle, "exp-to-next", (Object) defExp));
                // Add custom tiers
                for (String key : customSec.getKeys(false)) {
                    int lvl = old.getInt("Level.tier.custom." + key + ".level", 0);
                    String ttl = old.getString("Level.tier.custom." + key + ".title", "&7?");
                    int exp = old.getInt("Level.tier.custom." + key + ".exp-for-level-up", 10);
                    tiers.add(Map.of("level", (Object) lvl, "title", (Object) ttl, "exp-to-next", (Object) exp));
                }
                config.set("level.tiers", tiers);
            }
        }

        log.info("[DuelTime4] Config values migrated from DT3");
    }

    private void copyIfSet(YamlConfiguration old, String oldPath, String newPath) {
        if (old.contains(oldPath)) {
            config.set(newPath, old.get(oldPath));
        }
    }

    /** Parse DT3 location string: "DUELTIME LOCATION world,x,y,z,yaw,pitch" */
    private Location parseDt3Location(String str) {
        if (str == null) return null;
        Matcher m = LOC_PATTERN.matcher(str);
        if (!m.find()) return null;
        try {
            String worldName = m.group(1);
            double x = Double.parseDouble(m.group(2));
            double y = Double.parseDouble(m.group(3));
            double z = Double.parseDouble(m.group(4));
            float yaw = Float.parseFloat(m.group(5));
            float pitch = Float.parseFloat(m.group(6));
            var world = Bukkit.getWorld(worldName);
            if (world == null) world = Bukkit.getWorlds().get(0);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            log.warning("Failed to parse DT3 location: " + str);
            return null;
        }
    }
}
