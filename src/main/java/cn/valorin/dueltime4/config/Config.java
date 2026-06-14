package cn.valorin.dueltime4.config;

import org.bukkit.plugin.java.JavaPlugin;

public class Config {
    private final JavaPlugin plugin;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }
}
