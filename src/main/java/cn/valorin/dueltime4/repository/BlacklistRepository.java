package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.List;

public class BlacklistRepository {

    private final DatabaseManager db;

    public BlacklistRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS blacklist (
                player_name TEXT PRIMARY KEY,
                reason TEXT DEFAULT ''
            )
        """);
    }

    public boolean isBlacklisted(String playerName) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT 1 FROM blacklist WHERE player_name = ?",
                rs -> true, playerName).isPresent();
        }
    }

    public void add(String playerName, String reason) {
        try (SqlHelper sql = db.open()) {
            sql.update("INSERT OR REPLACE INTO blacklist (player_name, reason) VALUES (?, ?)",
                playerName, reason);
        }
    }

    public void remove(String playerName) {
        try (SqlHelper sql = db.open()) {
            sql.update("DELETE FROM blacklist WHERE player_name = ?", playerName);
        }
    }

    public List<String> getAll() {
        try (SqlHelper sql = db.open()) {
            return sql.query("SELECT player_name FROM blacklist", rs -> rs.getString("player_name"));
        }
    }
}
