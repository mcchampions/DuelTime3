package cn.valorin.dueltime4.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class Config {

    private final JavaPlugin plugin;
    private FileConfiguration yaml;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.yaml = plugin.getConfig();
    }

    public JavaPlugin getPlugin() { return plugin; }

    public String getString(String path, String def) {
        return yaml.getString(path, def);
    }

    public int getInt(String path, int def) {
        return yaml.getInt(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return yaml.getBoolean(path, def);
    }

    public double getDouble(String path, double def) {
        return yaml.getDouble(path, def);
    }

    public List<String> getStringList(String path) {
        return yaml.getStringList(path);
    }

    public List<Map<?, ?>> getMapList(String path) {
        return yaml.getMapList(path);
    }

    public Map<String, Object> getSectionValues(String path) {
        var sec = yaml.getConfigurationSection(path);
        return sec == null ? Map.of() : sec.getValues(false);
    }

    public void set(String path, Object value) {
        yaml.set(path, value);
        plugin.saveConfig();
    }

    public FileConfiguration raw() { return yaml; }

    // --- Convenience accessors ---

    public int getArenaCountdown(String arenaType) {
        return getInt("arena.defaults." + arenaType + ".countdown", 5);
    }

    public boolean getArenaCountdownFreeze(String arenaType) {
        return getBoolean("arena.defaults." + arenaType + ".countdown-freeze", true);
    }

    public int getArenaTimeLimit(String arenaType) {
        return getInt("arena.defaults." + arenaType + ".time-limit", 0);
    }

    public int getArenaWinExp(String arenaType) {
        return getInt("arena.defaults." + arenaType + ".reward.win-exp", 30);
    }

    public int getArenaWinPoint(String arenaType) {
        return getInt("arena.defaults." + arenaType + ".reward.win-point", 1);
    }

    public double getArenaLoseExpRate(String arenaType) {
        return getDouble("arena.defaults." + arenaType + ".reward.lose-exp-rate", 0.3);
    }

    public boolean getWinStreakEnabled(String arenaType) {
        return getBoolean("arena.defaults." + arenaType + ".win-streak.enabled", false);
    }

    public Map<String, Object> getWinStreakSection(String arenaType, String key) {
        return getSectionValues("arena.defaults." + arenaType + ".win-streak." + key);
    }
}
