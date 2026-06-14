package cn.valorin.dueltime4.player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerProfile {
    private String playerName;
    private double exp;
    private int point;
    private int classicWins, classicLoses, classicDraws;
    private int totalGames, totalTime;
    private int winStreak, maxWinStreak;

    public PlayerProfile(String playerName) { this.playerName = playerName; }

    public static PlayerProfile fromResultSet(ResultSet rs) throws SQLException {
        PlayerProfile p = new PlayerProfile(rs.getString("player_name"));
        p.exp = rs.getDouble("exp");
        p.point = rs.getInt("point");
        p.classicWins = rs.getInt("classic_wins");
        p.classicLoses = rs.getInt("classic_loses");
        p.classicDraws = rs.getInt("classic_draws");
        p.totalGames = rs.getInt("total_games");
        p.totalTime = rs.getInt("total_time");
        p.winStreak = rs.getInt("win_streak");
        p.maxWinStreak = rs.getInt("max_win_streak");
        return p;
    }

    public String getPlayerName() { return playerName; }
    public double getExp() { return exp; }
    public int getPoint() { return point; }
    public int getClassicWins() { return classicWins; }
    public int getClassicLoses() { return classicLoses; }
    public int getClassicDraws() { return classicDraws; }
    public int getTotalGames() { return totalGames; }
    public int getTotalTime() { return totalTime; }
    public int getWinStreak() { return winStreak; }
    public int getMaxWinStreak() { return maxWinStreak; }
    public void setExp(double exp) { this.exp = exp; }
    public void setPoint(int point) { this.point = point; }
    public void setTotalGames(int n) { this.totalGames = n; }
    public void setTotalTime(int n) { this.totalTime = n; }
    public void setClassicWins(int n) { this.classicWins = n; }
    public void setClassicLoses(int n) { this.classicLoses = n; }
    public void setClassicDraws(int n) { this.classicDraws = n; }
    public void addExp(double amount) { this.exp += amount; }
    public void addPoint(int amount) { this.point += amount; }
    public void incrementWins() { this.classicWins++; this.totalGames++; }
    public void incrementLoses() { this.classicLoses++; this.totalGames++; }
    public void incrementDraws() { this.classicDraws++; this.totalGames++; }
    public void addTime(int seconds) { this.totalTime += seconds; }
    public void onWin() { winStreak++; if (winStreak > maxWinStreak) maxWinStreak = winStreak; }
    public void onLose() { winStreak = 0; }
    public void onDraw() { winStreak = 0; }
}
