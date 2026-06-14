package cn.valorin.dueltime4.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class Config {

    private final JavaPlugin plugin;
    private FileConfiguration yaml;

    // Cached values — populated on reload()
    private String prefix;
    private String language;
    private String dbType;
    private int rankingRefreshSeconds;
    private int recordShowCooldown;
    private int recordPrintCost;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.yaml = plugin.getConfig();
        this.prefix = yaml.getString("core.prefix", "&7&l[&bDuelTime&7&l] ");
        this.language = yaml.getString("core.language", "zh_CN");
        this.dbType = yaml.getString("database.type", "sqlite");
        this.rankingRefreshSeconds = yaml.getInt("ranking.refresh-seconds", 30);
        this.recordShowCooldown = yaml.getInt("record.show-cooldown", 10);
        this.recordPrintCost = yaml.getInt("record.print-cost", 1);
    }

    public JavaPlugin getPlugin() { return plugin; }

    public String getString(String path, String def) { return yaml.getString(path, def); }
    public int getInt(String path, int def) { return yaml.getInt(path, def); }
    public boolean getBoolean(String path, boolean def) { return yaml.getBoolean(path, def); }
    public double getDouble(String path, double def) { return yaml.getDouble(path, def); }
    public List<String> getStringList(String path) { return yaml.getStringList(path); }

    public List<Map<?, ?>> getMapList(String path) { return yaml.getMapList(path); }

    public Map<String, Object> getSectionValues(String path) {
        var sec = yaml.getConfigurationSection(path);
        return sec == null ? Map.of() : sec.getValues(false);
    }

    public void set(String path, Object value) {
        yaml.set(path, value);
        plugin.saveConfig();
    }

    public void save() { plugin.saveConfig(); }

    // --- Cached getters ---
    public String getPrefix() { return prefix; }
    public String getLanguage() { return language; }
    public String getDbType() { return dbType; }
    public int getRankingRefreshSeconds() { return rankingRefreshSeconds; }
    public int getRecordShowCooldown() { return recordShowCooldown; }
    public int getRecordPrintCost() { return recordPrintCost; }

    // --- Arena config (dynamic: depends on arena type parameter) ---
    public int getArenaCountdown(String arenaType) {
        return yaml.getInt("arena.defaults." + arenaType + ".countdown", 5);
    }

    public boolean getArenaCountdownFreeze(String arenaType) {
        return yaml.getBoolean("arena.defaults." + arenaType + ".countdown-freeze", true);
    }

    public int getArenaTimeLimit(String arenaType) {
        return yaml.getInt("arena.defaults." + arenaType + ".time-limit", 0);
    }

    public int getArenaWinExp(String arenaType) {
        return yaml.getInt("arena.defaults." + arenaType + ".reward.win-exp", 30);
    }

    public int getArenaWinPoint(String arenaType) {
        return yaml.getInt("arena.defaults." + arenaType + ".reward.win-point", 1);
    }

    public double getArenaLoseExpRate(String arenaType) {
        return yaml.getDouble("arena.defaults." + arenaType + ".reward.lose-exp-rate", 0.3);
    }

    public boolean getWinStreakEnabled(String arenaType) {
        return yaml.getBoolean("arena.defaults." + arenaType + ".win-streak.enabled", false);
    }

    public Map<String, Object> getWinStreakSection(String arenaType, String key) {
        return getSectionValues("arena.defaults." + arenaType + ".win-streak." + key);
    }
}
