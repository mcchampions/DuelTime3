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
    private RequestService requestService;
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
        playerRepository = new PlayerRepository(databaseManager);
        arenaRepository = new ArenaRepository(databaseManager);
        recordRepository = new RecordRepository(databaseManager);
        locationRepository = new LocationRepository(databaseManager);
        blacklistRepository = new BlacklistRepository(databaseManager);

        // 3. Services
        playerService = new PlayerService(playerRepository, config);
        arenaService = new ArenaService(arenaRepository, locationRepository);
        matchService = new MatchService(arenaService, playerService, recordRepository, config);
        spectateService = new SpectateService(arenaService);
        rankingService = new RankingService(playerService);
        shopService = new ShopService(playerService, config);
        blacklistService = new BlacklistService(blacklistRepository);
        requestService = new RequestService();
        migrationService = new MigrationService();

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
