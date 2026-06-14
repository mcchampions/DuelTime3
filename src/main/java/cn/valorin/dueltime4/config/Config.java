package cn.valorin.dueltime4.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Config {

    private final JavaPlugin plugin;

    // Cached — populated on reload()
    private String prefix;
    private String language;
    private String dbType;
    private String dbMysqlHost;
    private int dbMysqlPort;
    private String dbMysqlDatabase;
    private String dbMysqlUsername;
    private String dbMysqlPassword;
    private int rankingRefreshSeconds;
    private boolean rankingHologramEnabled;
    private int rankingHologramMaxSize;
    private int recordShowCooldown;
    private int recordPrintCost;
    private String levelChatPrefix;
    private List<Map<String, Object>> levelTiers;
    private boolean migrationEnabled;
    private String migrationOldPluginFolder;
    private String migrationOldDbType;
    private String migrationOldDbSqlitePath;
    private String migrationOldDbMysqlHost;
    private int migrationOldDbMysqlPort;
    private String migrationOldDbMysqlDatabase;
    private String migrationOldDbMysqlUsername;
    private String migrationOldDbMysqlPassword;

    // Per-arena-type caches
    private final Map<String, ArenaDefaults> arenaDefaults = new HashMap<>();

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration y = plugin.getConfig();

        prefix = y.getString("core.prefix", "&7&l[&bDuelTime&7&l] ");
        language = y.getString("core.language", "zh_CN");

        dbType = y.getString("database.type", "sqlite");
        dbMysqlHost = y.getString("database.mysql.host", "localhost");
        dbMysqlPort = y.getInt("database.mysql.port", 3306);
        dbMysqlDatabase = y.getString("database.mysql.database", "dueltime");
        dbMysqlUsername = y.getString("database.mysql.username", "root");
        dbMysqlPassword = y.getString("database.mysql.password", "");

        rankingRefreshSeconds = y.getInt("ranking.refresh-seconds", 30);
        rankingHologramEnabled = y.getBoolean("ranking.hologram.enabled", true);
        rankingHologramMaxSize = y.getInt("ranking.hologram.max-size", 10);

        recordShowCooldown = y.getInt("record.show-cooldown", 10);
        recordPrintCost = y.getInt("record.print-cost", 1);

        levelChatPrefix = y.getString("level.chat-prefix", "&f[%level%&f]");
        levelTiers = new ArrayList<>();
        for (Map<?, ?> m : y.getMapList("level.tiers")) {
            Map<String, Object> tier = new LinkedHashMap<>();
            tier.put("level", m.get("level"));
            tier.put("title", m.get("title"));
            tier.put("exp-to-next", m.get("exp-to-next"));
            levelTiers.add(tier);
        }

        arenaDefaults.clear();
        for (String type : List.of("classic", "team", "ffa")) {
            arenaDefaults.put(type, new ArenaDefaults(y, type));
        }

        migrationEnabled = y.getBoolean("migration.enabled", false);
        migrationOldPluginFolder = y.getString("migration.old-plugin-folder", "plugins/DuelTime");
        migrationOldDbType = y.getString("migration.old-database.type", "sqlite");
        migrationOldDbSqlitePath = y.getString("migration.old-database.sqlite.path", "plugins/DuelTime/sqlite.db");
        migrationOldDbMysqlHost = y.getString("migration.old-database.mysql.host", "localhost");
        migrationOldDbMysqlPort = y.getInt("migration.old-database.mysql.port", 3306);
        migrationOldDbMysqlDatabase = y.getString("migration.old-database.mysql.database", "dueltime");
        migrationOldDbMysqlUsername = y.getString("migration.old-database.mysql.username", "root");
        migrationOldDbMysqlPassword = y.getString("migration.old-database.mysql.password", "");
    }

    public JavaPlugin getPlugin() { return plugin; }

    // ─── Generic accessors (for uncached dynamic paths like shop.items) ───

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }
    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }
    public boolean getBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }
    public double getDouble(String path, double def) {
        return plugin.getConfig().getDouble(path, def);
    }
    public List<String> getStringList(String path) {
        return plugin.getConfig().getStringList(path);
    }
    public List<Map<?, ?>> getMapList(String path) {
        return plugin.getConfig().getMapList(path);
    }
    public Map<String, Object> getSectionValues(String path) {
        var sec = plugin.getConfig().getConfigurationSection(path);
        return sec == null ? Map.of() : sec.getValues(false);
    }

    public void set(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }
    public void save() { plugin.saveConfig(); }

    // ─── Cached getters ───

    public String getPrefix() { return prefix; }
    public String getLanguage() { return language; }
    public String getDbType() { return dbType; }
    public String getDbMysqlHost() { return dbMysqlHost; }
    public int getDbMysqlPort() { return dbMysqlPort; }
    public String getDbMysqlDatabase() { return dbMysqlDatabase; }
    public String getDbMysqlUsername() { return dbMysqlUsername; }
    public String getDbMysqlPassword() { return dbMysqlPassword; }
    public int getRankingRefreshSeconds() { return rankingRefreshSeconds; }
    public boolean getRankingHologramEnabled() { return rankingHologramEnabled; }
    public int getRankingHologramMaxSize() { return rankingHologramMaxSize; }
    public int getRecordShowCooldown() { return recordShowCooldown; }
    public int getRecordPrintCost() { return recordPrintCost; }
    public String getLevelChatPrefix() { return levelChatPrefix; }
    public List<Map<String, Object>> getLevelTiers() { return levelTiers; }
    public boolean getMigrationEnabled() { return migrationEnabled; }
    public String getMigrationOldPluginFolder() { return migrationOldPluginFolder; }
    public String getMigrationOldDbType() { return migrationOldDbType; }
    public String getMigrationOldDbSqlitePath() { return migrationOldDbSqlitePath; }
    public String getMigrationOldDbMysqlHost() { return migrationOldDbMysqlHost; }
    public int getMigrationOldDbMysqlPort() { return migrationOldDbMysqlPort; }
    public String getMigrationOldDbMysqlDatabase() { return migrationOldDbMysqlDatabase; }
    public String getMigrationOldDbMysqlUsername() { return migrationOldDbMysqlUsername; }
    public String getMigrationOldDbMysqlPassword() { return migrationOldDbMysqlPassword; }

    // ─── Arena defaults ───

    public int getArenaCountdown(String type) { return arenaDefaults.get(type).countdown; }
    public boolean getArenaCountdownFreeze(String type) { return arenaDefaults.get(type).countdownFreeze; }
    public int getArenaTimeLimit(String type) { return arenaDefaults.get(type).timeLimit; }
    public boolean getArenaAllowSpectate(String type) { return arenaDefaults.get(type).allowSpectate; }
    public boolean getArenaShowHealthBossbar(String type) { return arenaDefaults.get(type).showHealthBossbar; }
    public int getArenaWinExp(String type) { return arenaDefaults.get(type).rewardWinExp; }
    public int getArenaWinPoint(String type) { return arenaDefaults.get(type).rewardWinPoint; }
    public double getArenaLoseExpRate(String type) { return arenaDefaults.get(type).rewardLoseExpRate; }
    public boolean getArenaAutoRespawn(String type) { return arenaDefaults.get(type).autoRespawn; }
    public int getArenaDelayedBack(String type) { return arenaDefaults.get(type).delayedBack; }
    public boolean getWinStreakEnabled(String type) { return arenaDefaults.get(type).winStreakEnabled; }
    public Map<String, Object> getWinStreakSection(String type, String key) {
        return arenaDefaults.get(type).winStreakSections.getOrDefault(key, Map.of());
    }

    private static class ArenaDefaults {
        final int countdown, timeLimit, rewardWinExp, rewardWinPoint, delayedBack;
        final boolean countdownFreeze, allowSpectate, showHealthBossbar, autoRespawn, winStreakEnabled;
        final double rewardLoseExpRate;
        final Map<String, Map<String, Object>> winStreakSections = new HashMap<>();

        @SuppressWarnings("unchecked")
        ArenaDefaults(FileConfiguration y, String type) {
            String base = "arena.defaults." + type;
            countdown = y.getInt(base + ".countdown", 5);
            countdownFreeze = y.getBoolean(base + ".countdown-freeze", true);
            timeLimit = y.getInt(base + ".time-limit", 300);
            allowSpectate = y.getBoolean(base + ".allow-spectate", true);
            showHealthBossbar = y.getBoolean(base + ".show-health-bossbar", true);
            rewardWinExp = y.getInt(base + ".reward.win-exp", 30);
            rewardWinPoint = y.getInt(base + ".reward.win-point", 1);
            rewardLoseExpRate = y.getDouble(base + ".reward.lose-exp-rate", 0.3);
            autoRespawn = y.getBoolean(base + ".auto-respawn", true);
            delayedBack = y.getInt(base + ".delayed-back", 5);
            winStreakEnabled = y.getBoolean(base + ".win-streak.enabled", false);
            for (String section : List.of("bonus-point", "bonus-exp-rate")) {
                var sec = y.getConfigurationSection(base + ".win-streak." + section);
                if (sec != null) winStreakSections.put(section, sec.getValues(false));
            }
        }
    }
}
