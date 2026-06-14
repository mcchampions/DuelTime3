package cn.valorin.dueltime4;

import cn.valorin.dueltime4.command.CommandManager;
import cn.valorin.dueltime4.command.impl.*;
import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.config.Messages;
import cn.valorin.dueltime4.hook.DuelTimePlaceholderExpansion;
import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.listener.*;
import cn.valorin.dueltime4.repository.*;
import cn.valorin.dueltime4.service.*;
import org.bukkit.Bukkit;
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
    private RequestService requestService;
    private MigrationService migrationService;
    private DuelTimePlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Config & Messages
        config = new Config(this);
        messages = new Messages(this, config);

        // 2. Database & Repositories
        databaseManager = new DatabaseManager(config);
        playerRepository = new PlayerRepository(databaseManager);
        arenaRepository = new ArenaRepository(databaseManager);
        recordRepository = new RecordRepository(databaseManager);
        locationRepository = new LocationRepository(databaseManager);
        blacklistRepository = new BlacklistRepository(databaseManager);

        // 3. Initialize DB tables
        playerRepository.createTableIfNotExists();
        arenaRepository.createTableIfNotExists();
        recordRepository.createTableIfNotExists();
        locationRepository.createTableIfNotExists();
        blacklistRepository.createTableIfNotExists();

        // 4. Services
        playerService = new PlayerService(playerRepository, config);
        arenaService = new ArenaService(arenaRepository, locationRepository);
        matchService = new MatchService(arenaService, playerService, recordRepository, config);
        spectateService = new SpectateService(arenaService);
        rankingService = new RankingService(playerService);
        shopService = new ShopService(playerService, config);
        blacklistService = new BlacklistService(blacklistRepository);
        requestService = new RequestService();
        migrationService = new MigrationService(databaseManager, config, arenaRepository,
            playerRepository, recordRepository, locationRepository, blacklistRepository);

        // 5. Load arenas from DB
        arenaService.loadAll();

        // 6. Migration (if enabled in config)
        if (config.getBoolean("migration.enabled", false)) {
            migrationService.run();
        }

        // 7. Register commands
        CommandManager cmdManager = new CommandManager();
        cmdManager.register(new CmdHelp(cmdManager));
        cmdManager.register(new CmdArena());
        cmdManager.register(new CmdSend());
        cmdManager.register(new CmdAccept());
        cmdManager.register(new CmdDecline());
        cmdManager.register(new CmdJoin());
        cmdManager.register(new CmdQuit());
        cmdManager.register(new CmdSpectate());
        cmdManager.register(new CmdStart());
        cmdManager.register(new CmdStop());
        cmdManager.register(new CmdShop());
        cmdManager.register(new CmdRank());
        cmdManager.register(new CmdRecord());
        cmdManager.register(new CmdLobby());
        cmdManager.register(new CmdBlacklist());
        cmdManager.register(new CmdLang());
        cmdManager.register(new CmdReload());
        cmdManager.register(new CmdMigrate());
        cmdManager.register(new CmdLevel());

        // 8. Register listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ArenaProtectionListener(), this);
        pm.registerEvents(new ArenaMatchListener(), this);
        pm.registerEvents(new ArenaSpectateListener(), this);
        pm.registerEvents(new PlayerDataListener(), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new RankingListener(), this);

        // 9. Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new DuelTimePlaceholderExpansion(playerService);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        // 10. Start ranking auto-refresh timer
        int interval = config.getInt("ranking.refresh-seconds", 30);
        rankingService.startAutoRefresh(interval);

        getLogger().info("DuelTime4 v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        if (matchService != null) matchService.shutdown();
        if (rankingService != null) rankingService.cancelAutoRefresh();
        if (databaseManager != null) databaseManager.close();
    }

    public static DuelTimePlugin getInstance() { return instance; }
    public Config getCfg() { return config; }
    public Messages getMsg() { return messages; }
    public DatabaseManager getDb() { return databaseManager; }
    public PlayerRepository getPlayerRepository() { return playerRepository; }
    public ArenaRepository getArenaRepository() { return arenaRepository; }
    public RecordRepository getRecordRepository() { return recordRepository; }
    public LocationRepository getLocationRepository() { return locationRepository; }
    public BlacklistRepository getBlacklistRepository() { return blacklistRepository; }
    public PlayerService getPlayerService() { return playerService; }
    public ArenaService getArenaService() { return arenaService; }
    public MatchService getMatchService() { return matchService; }
    public SpectateService getSpectateService() { return spectateService; }
    public RankingService getRankingService() { return rankingService; }
    public ShopService getShopService() { return shopService; }
    public BlacklistService getBlacklistService() { return blacklistService; }
    public RequestService getRequestService() { return requestService; }
    public MigrationService getMigrationService() { return migrationService; }
}
