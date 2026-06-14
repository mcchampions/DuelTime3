package cn.valorin.dueltime4.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Messages {

    private final JavaPlugin plugin;
    private final Config config;
    private YamlConfiguration yaml;
    private final Map<String, String> cache = new HashMap<>();
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public Messages(JavaPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        load();
    }

    public void load() {
        String lang = config.getString("core.language", "zh_CN");
        File langFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("messages_zh_CN.yml", false);
            langFile = new File(plugin.getDataFolder(), "messages_zh_CN.yml");
        }
        yaml = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();
    }

    public String getRaw(String path) {
        return cache.computeIfAbsent(path, p -> {
            String val = yaml.getString(p);
            return val != null ? val : path;
        });
    }

    public String get(String path, Map<String, String> placeholders) {
        String msg = getRaw(path);
        String prefix = config.getString("core.prefix", "");
        msg = msg.replace("%prefix%", prefix);
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return msg;
    }

    public String get(String path) {
        return get(path, null);
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        return SERIALIZER.deserialize(get(path, placeholders));
    }

    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    public void reload() {
        load();
    }
}
