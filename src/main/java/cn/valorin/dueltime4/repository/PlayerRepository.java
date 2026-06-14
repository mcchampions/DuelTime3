package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import cn.valorin.dueltime4.player.PlayerProfile;

import java.util.List;
import java.util.Optional;

public class PlayerRepository {

    private final DatabaseManager db;

    public PlayerRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS player_data (
                player_name TEXT PRIMARY KEY,
                exp REAL DEFAULT 0,
                point INTEGER DEFAULT 0,
                classic_wins INTEGER DEFAULT 0,
                classic_loses INTEGER DEFAULT 0,
                classic_draws INTEGER DEFAULT 0,
                total_games INTEGER DEFAULT 0,
                total_time INTEGER DEFAULT 0,
                win_streak INTEGER DEFAULT 0,
                max_win_streak INTEGER DEFAULT 0
            )
        """);
    }

    public Optional<PlayerProfile> findByName(String name) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne(
                "SELECT * FROM player_data WHERE player_name = ?",
                PlayerProfile::fromResultSet, name
            );
        }
    }

    public List<PlayerProfile> findTop(int limit, String orderBy) {
        try (SqlHelper sql = db.open()) {
            return sql.query(
                "SELECT * FROM player_data ORDER BY " + orderBy + " DESC LIMIT ?",
                PlayerProfile::fromResultSet, limit
            );
        }
    }

    public void upsert(PlayerProfile profile) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO player_data (player_name, exp, point, classic_wins, classic_loses, classic_draws,
                    total_games, total_time, win_streak, max_win_streak)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_name) DO UPDATE SET
                    exp = excluded.exp, point = excluded.point,
                    classic_wins = excluded.classic_wins, classic_loses = excluded.classic_loses,
                    classic_draws = excluded.classic_draws, total_games = excluded.total_games,
                    total_time = excluded.total_time, win_streak = excluded.win_streak,
                    max_win_streak = excluded.max_win_streak
            """,
                profile.getPlayerName(), profile.getExp(), profile.getPoint(),
                profile.getClassicWins(), profile.getClassicLoses(), profile.getClassicDraws(),
                profile.getTotalGames(), profile.getTotalTime(),
                profile.getWinStreak(), profile.getMaxWinStreak()
            );
        }
    }
}
