package cn.valorin.dueltime4;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.config.Messages;
import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.repository.*;
import cn.valorin.dueltime4.service.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class DuelTimePlugin extends JavaPlugin {

    private static DuelTimePlugin instance;

    private Config config;
    private Messages messages;
    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private ArenaRepository arenaRepository;
    private RecordRepository recordRepository;
    private LocationRepository locationRepository;
    private BlacklistRepository blacklistRepository;
    private PlayerService playerService;
    private ArenaService arenaService;
    private MatchService matchService;
    private SpectateService spectateService;
    private RankingService rankingService;
    private ShopService shopService;
    private BlacklistService blacklistService;
    private MigrationService migrationService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Config & Messages
        config = new Config(this);
        messages = new Messages(this, config);

        // 2. Database & Repositories
        databaseManager = new DatabaseManager(config);
        // Repositories will be created in Task 4
        // initTables() will be called in Task 4

        getLogger().info("DuelTime4 v" + getDescription().getVersion() + " enabled (skeleton)");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
    }

    public static DuelTimePlugin getInstance() { return instance; }
    public Config getCfg() { return config; }
    public Messages getMsg() { return messages; }
    public DatabaseManager getDb() { return databaseManager; }
}
