package cn.valorin.dueltime4.config;

import org.bukkit.plugin.java.JavaPlugin;

public class Messages {
    private final JavaPlugin plugin;
    private final Config config;

    public Messages(JavaPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }
}
