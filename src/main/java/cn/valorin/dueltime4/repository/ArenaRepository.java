package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.*;

public class ArenaRepository {

    private final DatabaseManager db;

    public ArenaRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS arena_data (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                world TEXT,
                enabled INTEGER DEFAULT 1,
                data_json TEXT NOT NULL
            )
        """);
    }

    public List<Map<String, Object>> findAll() {
        try (SqlHelper sql = db.open()) {
            return sql.query("SELECT * FROM arena_data WHERE enabled = 1", rs -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("name", rs.getString("name"));
                row.put("type", rs.getString("type"));
                row.put("world", rs.getString("world"));
                row.put("data_json", rs.getString("data_json"));
                return row;
            });
        }
    }

    public Optional<Map<String, Object>> findById(String id) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT * FROM arena_data WHERE id = ?", rs -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("name", rs.getString("name"));
                row.put("type", rs.getString("type"));
                row.put("world", rs.getString("world"));
                row.put("enabled", rs.getInt("enabled"));
                row.put("data_json", rs.getString("data_json"));
                return row;
            }, id);
        }
    }

    public void save(String id, String name, String type, String world, String dataJson) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO arena_data (id, name, type, world, enabled, data_json)
                VALUES (?, ?, ?, ?, 1, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name, type = excluded.type,
                    world = excluded.world, data_json = excluded.data_json
            """, id, name, type, world, dataJson);
        }
    }

    public void setEnabled(String id, boolean enabled) {
        try (SqlHelper sql = db.open()) {
            sql.update("UPDATE arena_data SET enabled = ? WHERE id = ?", enabled ? 1 : 0, id);
        }
    }

    public void delete(String id) {
        try (SqlHelper sql = db.open()) {
            sql.update("DELETE FROM arena_data WHERE id = ?", id);
        }
    }
}
