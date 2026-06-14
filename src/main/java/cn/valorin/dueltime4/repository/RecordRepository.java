package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.*;

public class RecordRepository {

    private final DatabaseManager db;

    public RecordRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("CREATE TABLE IF NOT EXISTS arena_record ("
            + "id INTEGER PRIMARY KEY " + db.autoInc() + ","
            + "player_name TEXT NOT NULL,"
            + "arena_id TEXT NOT NULL,"
            + "arena_type TEXT NOT NULL,"
            + "opponent_name TEXT,"
            + "result TEXT NOT NULL,"
            + "duration INTEGER DEFAULT 0,"
            + "exp_change REAL DEFAULT 0,"
            + "hit_count INTEGER DEFAULT 0,"
            + "total_damage REAL DEFAULT 0,"
            + "max_damage REAL DEFAULT 0,"
            + "avg_damage REAL DEFAULT 0,"
            + "time TEXT NOT NULL"
            + ")");
    }

    public void insert(String playerName, String arenaId, String arenaType, String opponent,
                       String result, int duration, double expChange, int hitCount,
                       double totalDamage, double maxDamage, double avgDamage, String time) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO arena_record (player_name, arena_id, arena_type, opponent_name, result,
                    duration, exp_change, hit_count, total_damage, max_damage, avg_damage, time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, playerName, arenaId, arenaType, opponent, result,
                duration, expChange, hitCount, totalDamage, maxDamage, avgDamage, time);
        }
    }

    public List<Map<String, Object>> findByPlayer(String playerName, int limit) {
        try (SqlHelper sql = db.open()) {
            return sql.query(
                "SELECT * FROM arena_record WHERE player_name = ? ORDER BY id DESC LIMIT ?",
                rs -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("player_name", rs.getString("player_name"));
                    row.put("arena_id", rs.getString("arena_id"));
                    row.put("arena_type", rs.getString("arena_type"));
                    row.put("opponent_name", rs.getString("opponent_name"));
                    row.put("result", rs.getString("result"));
                    row.put("duration", rs.getInt("duration"));
                    row.put("exp_change", rs.getDouble("exp_change"));
                    row.put("total_damage", rs.getDouble("total_damage"));
                    row.put("max_damage", rs.getDouble("max_damage"));
                    row.put("avg_damage", rs.getDouble("avg_damage"));
                    row.put("time", rs.getString("time"));
                    return row;
                }, playerName, limit);
        }
    }
}
