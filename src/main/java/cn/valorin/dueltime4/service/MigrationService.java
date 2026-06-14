package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.event.MigrationCompleteEvent;
import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import cn.valorin.dueltime4.repository.*;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

public class MigrationService {

    private final DatabaseManager db;
    private final Config config;
    private final ArenaRepository arenaRepo;
    private final PlayerRepository playerRepo;
    private final RecordRepository recordRepo;
    private final LocationRepository locationRepo;
    private final BlacklistRepository blacklistRepo;
    private final Logger log = Bukkit.getLogger();

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
            String path = config.getString("migration.old-database.sqlite.path", "plugins/DuelTime3/dueltime.db");
            File f = new File(path);
            if (!f.isAbsolute()) f = new File(config.getPlugin().getDataFolder().getParentFile(), path);
            jdbcUrl = "jdbc:sqlite:" + f.getAbsolutePath();
        }

        try (Connection oldConn = user != null ?
                DriverManager.getConnection(jdbcUrl, user, pass) :
                DriverManager.getConnection(jdbcUrl);
             SqlHelper oldDb = new SqlHelper(oldConn)) {

            db.withTransaction(newDb -> {
                try { migratePlayers(oldDb); } catch (Exception e) { log.warning("Player migration error: " + e.getMessage()); }
                try { migrateArenas(oldDb); } catch (Exception e) { log.warning("Arena migration error: " + e.getMessage()); }
                try { migrateRecords(oldDb); } catch (Exception e) { log.warning("Record migration error: " + e.getMessage()); }
                try { migrateLocations(oldDb); } catch (Exception e) { log.warning("Location migration error: " + e.getMessage()); }
                try { migrateBlacklist(oldDb); } catch (Exception e) { log.warning("Blacklist migration error: " + e.getMessage()); }
                try { migrateShopItems(oldDb); } catch (Exception e) { log.warning("Shop migration error: " + e.getMessage()); }
                return null;
            });

            config.set("migration.enabled", false);
            log.info("[DuelTime4] Migration done: " + arenaCount + " arenas, "
                + playerCount + " players, " + recordCount + " records, "
                + shopItemCount + " shop items");
            Bukkit.getPluginManager().callEvent(new MigrationCompleteEvent(arenaCount, playerCount, recordCount));

        } catch (SQLException e) {
            log.severe("[DuelTime4] Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migratePlayers(SqlHelper oldDb) {
        try {
            oldDb.query("SELECT * FROM player_data", rs -> {
                var p = new cn.valorin.dueltime4.player.PlayerProfile(rs.getString("player_name"));
                try { p.setExp(rs.getDouble("exp")); } catch (Exception ignored) {}
                try { p.setPoint(rs.getInt("point")); } catch (Exception ignored) {}
                playerRepo.upsert(p);
                playerCount++;
                return null;
            });
        } catch (Exception e) {
            log.warning("Could not read player_data table: " + e.getMessage());
        }
    }

    private void migrateArenas(SqlHelper oldDb) {
        try {
            oldDb.query("SELECT * FROM classic_arena_data", rs -> {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String world = "";
                try { world = rs.getString("world"); } catch (Exception ignored) {}
                String dataJson = String.format(
                    "{\"pos1\":{\"x\":%f,\"y\":%f,\"z\":%f},\"pos2\":{\"x\":%f,\"y\":%f,\"z\":%f}}",
                    rs.getDouble("p1_x"), rs.getDouble("p1_y"), rs.getDouble("p1_z"),
                    rs.getDouble("p2_x"), rs.getDouble("p2_y"), rs.getDouble("p2_z"));
                arenaRepo.save(id, name, "classic", world, dataJson);
                arenaCount++;
                return null;
            });
        } catch (Exception e) {
            log.warning("Could not read classic_arena_data: " + e.getMessage());
        }
    }

    private void migrateRecords(SqlHelper oldDb) {
        try {
            oldDb.query("SELECT * FROM classic_arena_record_data", rs -> {
                recordRepo.insert(
                    rs.getString("player_name"), rs.getString("arena_id"), "classic",
                    rs.getString("opponent_name"), rs.getString("result"),
                    rs.getInt("time"), rs.getDouble("exp_change"),
                    rs.getInt("hit_time"), rs.getDouble("total_damage"),
                    rs.getDouble("max_damage"), rs.getDouble("average_damage"),
                    rs.getString("time_str"));
                recordCount++;
                return null;
            });
        } catch (Exception e) {
            log.warning("Could not read classic_arena_record_data: " + e.getMessage());
        }
    }

    private void migrateLocations(SqlHelper oldDb) {
        try {
            oldDb.query("SELECT * FROM location_data", rs -> {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world != null) {
                    locationRepo.set(rs.getString("key"),
                        new org.bukkit.Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
                return null;
            });
        } catch (Exception e) {
            log.warning("Could not read location_data: " + e.getMessage());
        }
    }

    private void migrateBlacklist(SqlHelper oldDb) {
        try {
            oldDb.query("SELECT * FROM blacklist", rs -> {
                blacklistRepo.add(rs.getString("player_name"), "");
                return null;
            });
        } catch (Exception e) {
            log.warning("Could not read blacklist: " + e.getMessage());
        }
    }

    private void migrateShopItems(SqlHelper oldDb) {
        try {
            var items = new java.util.ArrayList<java.util.LinkedHashMap<String, Object>>();
            oldDb.query("SELECT * FROM shop_reward_data", rs -> {
                var item = new java.util.LinkedHashMap<String, Object>();
                item.put("id", rs.getString("id") != null ? rs.getString("id") : "migrated_" + shopItemCount);
                item.put("material", "STONE");
                item.put("name", rs.getString("name") != null ? rs.getString("name") : "Migrated Item");
                item.put("cost", rs.getInt("cost"));
                item.put("lore", java.util.List.of("&7Migrated from DuelTime3"));
                item.put("commands", java.util.List.of("say %player% bought migrated item"));
                items.add(item);
                shopItemCount++;
                return null;
            });
            if (!items.isEmpty()) {
                config.set("shop.items", items);
            }
        } catch (Exception e) {
            log.warning("Could not read shop_reward_data: " + e.getMessage());
        }
    }
}
