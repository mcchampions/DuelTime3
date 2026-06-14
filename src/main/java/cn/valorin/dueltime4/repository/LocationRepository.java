package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class LocationRepository {

    private final DatabaseManager db;

    public LocationRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS location_data (
                key TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL DEFAULT 0,
                pitch REAL DEFAULT 0
            )
        """);
    }

    public Optional<Location> get(String key) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT * FROM location_data WHERE key = ?", rs -> {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world,
                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
            }, key);
        }
    }

    public void set(String key, Location loc) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO location_data (key, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    world = excluded.world, x = excluded.x, y = excluded.y,
                    z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch
            """, key, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
        }
    }

    public Map<String, Location> getAll() {
        Map<String, Location> map = new HashMap<>();
        try (SqlHelper sql = db.open()) {
            sql.query("SELECT * FROM location_data", rs -> {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world != null) {
                    map.put(rs.getString("key"), new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
                return null;
            });
        }
        return map;
    }
}
