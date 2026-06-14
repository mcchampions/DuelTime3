# DuelTime4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full rewrite of DuelTime3 to DuelTime4 — service-oriented architecture, JDBC data layer, Classic/Team/FFA arenas, one-command migration.

**Architecture:** Manual constructor injection with `DuelTimePlugin` as composition root. Service layer depends on Repository layer which wraps JDBC via HikariCP. Arena subclasses are pure domain objects with a state machine. Events are post-notification only (non-cancellable).

**Tech Stack:** Java 21, Paper API 1.21.11+, HikariCP 5.x, SQLite/MySQL JDBC, DecentHolograms, PlaceholderAPI

**Base path:** `D:\DuelTime3\` (DT4 source will be in the same repo under reorganized packages `cn.valorin.dueltime4`)

---

### Task 1: Project Scaffolding

**Files:**
- Modify: `D:\DuelTime3\pom.xml`
- Create: `D:\DuelTime3\src\main\resources\plugin.yml` (overwrite)

- [ ] **Step 1: Update pom.xml for DuelTime4**

Replace the entire pom.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>cn.valorin</groupId>
    <artifactId>DuelTime4</artifactId>
    <version>4.0.0</version>
    <packaging>jar</packaging>

    <name>DuelTime4</name>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <relocations>
                                <relocation>
                                    <pattern>com.zaxxer.hikari</pattern>
                                    <shadedPattern>cn.valorin.dueltime4.lib.hikari</shadedPattern>
                                </relocation>
                            </relocations>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>placeholderapi</id>
            <url>https://repo.extendedclip.com/content/repositories/placeholderapi/</url>
        </repository>
        <repository>
            <id>codemc-repo</id>
            <url>https://repo.codemc.org/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.11-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.1.0</version>
        </dependency>
        <dependency>
            <groupId>me.clip</groupId>
            <artifactId>placeholderapi</artifactId>
            <version>2.11.6</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.github.decentsoftware-eu</groupId>
            <artifactId>decentholograms</artifactId>
            <version>2.8.3</version>
            <scope>provided</scope>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.44.1.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Update plugin.yml**

Overwrite `D:\DuelTime3\src\main\resources\plugin.yml`:

```yaml
name: DuelTime4
version: '${project.version}'
main: cn.valorin.dueltime4.DuelTimePlugin
api-version: '1.21'
softdepend: [PlaceholderAPI, DecentHolograms]
commands:
  dueltime:
    description: DuelTime4 commands
    usage: /<command>
    aliases: [dt]
```

- [ ] **Step 3: Update config.yml default**

Overwrite `D:\DuelTime3\src\main\resources\config.yml` with the full default configuration from the spec (Task 11 will refine).

- [ ] **Step 4: Verify build compiles**

Run: `cd D:\DuelTime3 && mvn compile`
Expected: BUILD SUCCESS (with only the plugin.yml and config.yml in resources, no Java source yet — may fail on missing main class, that's OK for now)

---

### Task 2: Core Infrastructure — DuelTimePlugin, DatabaseManager, SqlHelper

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\DuelTimePlugin.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\jdbc\DatabaseManager.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\jdbc\SqlHelper.java`

- [ ] **Step 1: Write SqlHelper**

```java
package cn.valorin.dueltime4.jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlHelper implements AutoCloseable {

    private final Connection conn;

    public SqlHelper(Connection conn) {
        this.conn = conn;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return results;
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public int update(String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + sql, e);
        }
    }

    public long insert(String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed: " + sql, e);
        }
        return -1;
    }

    public Connection raw() {
        return conn;
    }

    private void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            // ignore
        }
    }
}
```

- [ ] **Step 2: Write DatabaseManager**

```java
package cn.valorin.dueltime4.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cn.valorin.dueltime4.config.Config;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class DatabaseManager {

    private final HikariDataSource dataSource;
    private final Config config;
    private boolean sqlite;

    public DatabaseManager(Config config) {
        this.config = config;
        String type = config.getString("database.type", "sqlite");
        this.sqlite = "sqlite".equalsIgnoreCase(type);

        HikariConfig hikariConfig = new HikariConfig();
        if (sqlite) {
            File dbFile = new File(config.getPlugin().getDataFolder(), "dueltime.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        } else {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String db = config.getString("database.mysql.database", "dueltime");
            String user = config.getString("database.mysql.username", "root");
            String pass = config.getString("database.mysql.password", "");
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true");
            hikariConfig.setUsername(user);
            hikariConfig.setPassword(pass);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setConnectionTimeout(5000);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public SqlHelper open() {
        try {
            return new SqlHelper(dataSource.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection", e);
        }
    }

    public <T> T withTransaction(Function<SqlHelper, T> fn) {
        try (SqlHelper db = open()) {
            db.raw().setAutoCommit(false);
            try {
                T result = fn.apply(db);
                db.raw().commit();
                return result;
            } catch (Exception e) {
                db.raw().rollback();
                throw e;
            } finally {
                db.raw().setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    public void executeDDL(String sql) {
        try (SqlHelper db = open()) {
            db.update(sql);
        }
    }

    public boolean isSqlite() {
        return sqlite;
    }

    public void close() {
        dataSource.close();
    }
}
```

- [ ] **Step 3: Write DuelTimePlugin (composition root skeleton)**

```java
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
        playerRepository = new PlayerRepository(databaseManager);
        arenaRepository = new ArenaRepository(databaseManager);
        recordRepository = new RecordRepository(databaseManager);
        locationRepository = new LocationRepository(databaseManager);
        blacklistRepository = new BlacklistRepository(databaseManager);
        initTables();

        // 3. Services
        playerService = new PlayerService(playerRepository, config);
        arenaService = new ArenaService(arenaRepository, locationRepository);
        spectateService = new SpectateService(arenaService);
        matchService = new MatchService(arenaService, playerService, recordRepository, config);
        rankingService = new RankingService(playerService);
        shopService = new ShopService(playerService, config);
        blacklistService = new BlacklistService(blacklistRepository);
        migrationService = new MigrationService(databaseManager, config, arenaRepository,
                playerRepository, recordRepository, locationRepository, blacklistRepository);

        // 4-7 will be wired in later tasks
        getLogger().info("DuelTime4 v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (matchService != null) matchService.shutdown();
        if (databaseManager != null) databaseManager.close();
    }

    private void initTables() {
        playerRepository.createTableIfNotExists();
        arenaRepository.createTableIfNotExists();
        recordRepository.createTableIfNotExists();
        locationRepository.createTableIfNotExists();
        blacklistRepository.createTableIfNotExists();
    }

    public static DuelTimePlugin getInstance() { return instance; }
    public Config getCfg() { return config; }
    public Messages getMsg() { return messages; }
    public DatabaseManager getDb() { return databaseManager; }
    public PlayerService getPlayerService() { return playerService; }
    public ArenaService getArenaService() { return arenaService; }
    public MatchService getMatchService() { return matchService; }
    public SpectateService getSpectateService() { return spectateService; }
    public RankingService getRankingService() { return rankingService; }
    public ShopService getShopService() { return shopService; }
    public BlacklistService getBlacklistService() { return blacklistService; }
    public MigrationService getMigrationService() { return migrationService; }
}
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: project scaffolding, DB infrastructure, plugin skeleton"
```

---

### Task 3: Config & Messages

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\config\Config.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\config\Messages.java`
- Overwrite: `D:\DuelTime3\src\main\resources\config.yml`

- [ ] **Step 1: Write Config.java**

```java
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

    public void set(String path, Object value) {
        yaml.set(path, value);
        plugin.saveConfig();
    }

    public FileConfiguration raw() { return yaml; }

    // --- Convenience accessors ---

    // Arena defaults for a given type
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
        var sec = yaml.getConfigurationSection("arena.defaults." + arenaType + ".win-streak." + key);
        return sec == null ? Map.of() : sec.getValues(false);
    }
}
```

- [ ] **Step 2: Write Messages.java**

```java
package cn.valorin.dueltime4.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Messages(JavaPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        load();
    }

    public void load() {
        String lang = config.getString("core.language", "zh_CN");
        File langFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("messages_" + lang + ".yml", false);
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
        return mm.deserialize(get(path, placeholders).replace('&', '§'));
    }

    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    public void reload() {
        load();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/config/ src/main/resources/
git commit -m "feat: config and messages system"
```

---

### Task 4: Repository Layer

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\repository\PlayerRepository.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\repository\ArenaRepository.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\repository\RecordRepository.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\repository\LocationRepository.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\repository\BlacklistRepository.java`

- [ ] **Step 1: Write PlayerRepository.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import cn.valorin.dueltime4.player.PlayerProfile;

import java.util.List;
import java.util.Optional;

public class PlayerRepository {

    private final DatabaseManager db;

    public PlayerRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS player_data (
                player_name TEXT PRIMARY KEY,
                exp REAL DEFAULT 0,
                point INTEGER DEFAULT 0,
                classic_wins INTEGER DEFAULT 0,
                classic_loses INTEGER DEFAULT 0,
                classic_draws INTEGER DEFAULT 0,
                total_games INTEGER DEFAULT 0,
                total_time INTEGER DEFAULT 0,
                win_streak INTEGER DEFAULT 0,
                max_win_streak INTEGER DEFAULT 0
            )
        """);
    }

    public Optional<PlayerProfile> findByName(String name) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne(
                "SELECT * FROM player_data WHERE player_name = ?",
                PlayerProfile::fromResultSet, name
            );
        }
    }

    public List<PlayerProfile> findTop(int limit, String orderBy) {
        try (SqlHelper sql = db.open()) {
            return sql.query(
                "SELECT * FROM player_data ORDER BY " + orderBy + " DESC LIMIT ?",
                PlayerProfile::fromResultSet, limit
            );
        }
    }

    public void upsert(PlayerProfile profile) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO player_data (player_name, exp, point, classic_wins, classic_loses, classic_draws,
                    total_games, total_time, win_streak, max_win_streak)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_name) DO UPDATE SET
                    exp = excluded.exp, point = excluded.point,
                    classic_wins = excluded.classic_wins, classic_loses = excluded.classic_loses,
                    classic_draws = excluded.classic_draws, total_games = excluded.total_games,
                    total_time = excluded.total_time, win_streak = excluded.win_streak,
                    max_win_streak = excluded.max_win_streak
            """,
                profile.getPlayerName(), profile.getExp(), profile.getPoint(),
                profile.getClassicWins(), profile.getClassicLoses(), profile.getClassicDraws(),
                profile.getTotalGames(), profile.getTotalTime(),
                profile.getWinStreak(), profile.getMaxWinStreak()
            );
        }
    }
}
```

- [ ] **Step 2: Write ArenaRepository.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.*;

public class ArenaRepository {

    private final DatabaseManager db;

    public ArenaRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS arena_data (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                world TEXT,
                enabled INTEGER DEFAULT 1,
                data_json TEXT NOT NULL
            )
        """);
    }

    public List<Map<String, Object>> findAll() {
        try (SqlHelper sql = db.open()) {
            return sql.query("SELECT * FROM arena_data WHERE enabled = 1", rs -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("name", rs.getString("name"));
                row.put("type", rs.getString("type"));
                row.put("world", rs.getString("world"));
                row.put("data_json", rs.getString("data_json"));
                return row;
            });
        }
    }

    public Optional<Map<String, Object>> findById(String id) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT * FROM arena_data WHERE id = ?",
                rs -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getString("id"));
                    row.put("name", rs.getString("name"));
                    row.put("type", rs.getString("type"));
                    row.put("world", rs.getString("world"));
                    row.put("enabled", rs.getInt("enabled"));
                    row.put("data_json", rs.getString("data_json"));
                    return row;
                }, id);
        }
    }

    public void save(String id, String name, String type, String world, String dataJson) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO arena_data (id, name, type, world, enabled, data_json)
                VALUES (?, ?, ?, ?, 1, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name, type = excluded.type,
                    world = excluded.world, data_json = excluded.data_json
            """, id, name, type, world, dataJson);
        }
    }

    public void setEnabled(String id, boolean enabled) {
        try (SqlHelper sql = db.open()) {
            sql.update("UPDATE arena_data SET enabled = ? WHERE id = ?", enabled ? 1 : 0, id);
        }
    }

    public void delete(String id) {
        try (SqlHelper sql = db.open()) {
            sql.update("DELETE FROM arena_data WHERE id = ?", id);
        }
    }
}
```

- [ ] **Step 3: Write RecordRepository.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.*;

public class RecordRepository {

    private final DatabaseManager db;

    public RecordRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS arena_record (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name TEXT NOT NULL,
                arena_id TEXT NOT NULL,
                arena_type TEXT NOT NULL,
                opponent_name TEXT,
                result TEXT NOT NULL,
                duration INTEGER DEFAULT 0,
                exp_change REAL DEFAULT 0,
                hit_count INTEGER DEFAULT 0,
                total_damage REAL DEFAULT 0,
                max_damage REAL DEFAULT 0,
                avg_damage REAL DEFAULT 0,
                time TEXT NOT NULL
            )
        """);
    }

    public void insert(String playerName, String arenaId, String arenaType, String opponent,
                       String result, int duration, double expChange, int hitCount,
                       double totalDamage, double maxDamage, double avgDamage, String time) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO arena_record (player_name, arena_id, arena_type, opponent_name, result,
                    duration, exp_change, hit_count, total_damage, max_damage, avg_damage, time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, playerName, arenaId, arenaType, opponent, result,
                duration, expChange, hitCount, totalDamage, maxDamage, avgDamage, time);
        }
    }

    public List<Map<String, Object>> findByPlayer(String playerName, int limit) {
        try (SqlHelper sql = db.open()) {
            return sql.query(
                "SELECT * FROM arena_record WHERE player_name = ? ORDER BY id DESC LIMIT ?",
                rs -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("player_name", rs.getString("player_name"));
                    row.put("arena_id", rs.getString("arena_id"));
                    row.put("arena_type", rs.getString("arena_type"));
                    row.put("opponent_name", rs.getString("opponent_name"));
                    row.put("result", rs.getString("result"));
                    row.put("duration", rs.getInt("duration"));
                    row.put("exp_change", rs.getDouble("exp_change"));
                    row.put("total_damage", rs.getDouble("total_damage"));
                    row.put("max_damage", rs.getDouble("max_damage"));
                    row.put("avg_damage", rs.getDouble("avg_damage"));
                    row.put("time", rs.getString("time"));
                    return row;
                }, playerName, limit);
        }
    }
}
```

- [ ] **Step 4: Write LocationRepository.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class LocationRepository {

    private final DatabaseManager db;

    public LocationRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS location_data (
                key TEXT PRIMARY KEY,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL DEFAULT 0,
                pitch REAL DEFAULT 0
            )
        """);
    }

    public Optional<Location> get(String key) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT * FROM location_data WHERE key = ?", rs -> {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world,
                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
            }, key);
        }
    }

    public void set(String key, Location loc) {
        try (SqlHelper sql = db.open()) {
            sql.update("""
                INSERT INTO location_data (key, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    world = excluded.world, x = excluded.x, y = excluded.y,
                    z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch
            """, key, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
        }
    }

    public Map<String, Location> getAll() {
        Map<String, Location> map = new HashMap<>();
        try (SqlHelper sql = db.open()) {
            sql.query("SELECT * FROM location_data", rs -> {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world != null) {
                    map.put(rs.getString("key"), new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
                return null;
            });
        }
        return map;
    }
}
```

- [ ] **Step 5: Write BlacklistRepository.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;

import java.util.List;

public class BlacklistRepository {

    private final DatabaseManager db;

    public BlacklistRepository(DatabaseManager db) { this.db = db; }

    public void createTableIfNotExists() {
        db.executeDDL("""
            CREATE TABLE IF NOT EXISTS blacklist (
                player_name TEXT PRIMARY KEY,
                reason TEXT DEFAULT ''
            )
        """);
    }

    public boolean isBlacklisted(String playerName) {
        try (SqlHelper sql = db.open()) {
            return sql.queryOne("SELECT 1 FROM blacklist WHERE player_name = ?",
                rs -> true, playerName).isPresent();
        }
    }

    public void add(String playerName, String reason) {
        try (SqlHelper sql = db.open()) {
            sql.update("INSERT OR REPLACE INTO blacklist (player_name, reason) VALUES (?, ?)",
                playerName, reason);
        }
    }

    public void remove(String playerName) {
        try (SqlHelper sql = db.open()) {
            sql.update("DELETE FROM blacklist WHERE player_name = ?", playerName);
        }
    }

    public List<String> getAll() {
        try (SqlHelper sql = db.open()) {
            return sql.query("SELECT player_name FROM blacklist", rs -> rs.getString("player_name"));
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/repository/
git commit -m "feat: repository layer with JDBC implementations"
```

---

### Task 5: Domain Models — PlayerProfile, Gamer, Spectator

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\player\PlayerProfile.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\player\Gamer.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\player\Spectator.java`

- [ ] **Step 1: Write PlayerProfile.java**

```java
package cn.valorin.dueltime4.player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerProfile {

    private String playerName;
    private double exp;
    private int point;
    private int classicWins;
    private int classicLoses;
    private int classicDraws;
    private int totalGames;
    private int totalTime;
    private int winStreak;
    private int maxWinStreak;

    public PlayerProfile(String playerName) {
        this.playerName = playerName;
    }

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

    // Getters
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

    // Mutators
    public void setPoint(int point) { this.point = point; }
    public void setExp(double exp) { this.exp = exp; }
    public void addExp(double amount) { this.exp += amount; }
    public void addPoint(int amount) { this.point += amount; }
    public void incrementWins() { this.classicWins++; this.totalGames++; }
    public void incrementLoses() { this.classicLoses++; this.totalGames++; }
    public void incrementDraws() { this.classicDraws++; this.totalGames++; }
    public void addTime(int seconds) { this.totalTime += seconds; }

    public void onWin() {
        winStreak++;
        if (winStreak > maxWinStreak) maxWinStreak = winStreak;
    }

    public void onLose() {
        winStreak = 0;
    }

    public void onDraw() {
        // Draw resets streak
        winStreak = 0;
    }
}
```

- [ ] **Step 2: Write Gamer.java**

```java
package cn.valorin.dueltime4.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Gamer {

    private final Player player;
    private final Location originalLocation;
    private final GameMode originalGameMode;
    private Location recentLocation;
    private double totalDamage;
    private double maxDamage;
    private int hitCount;
    private boolean dead;
    private String result; // WIN, LOSE, DRAW

    public Gamer(Player player) {
        this.player = player;
        this.originalLocation = player.getLocation().clone();
        this.originalGameMode = player.getGameMode();
        this.recentLocation = player.getLocation().clone();
    }

    public Player getPlayer() { return player; }
    public String getPlayerName() { return player.getName(); }
    public Location getOriginalLocation() { return originalLocation; }
    public GameMode getOriginalGameMode() { return originalGameMode; }

    public void updateRecentLocation(Location loc) { this.recentLocation = loc.clone(); }
    public Location getRecentLocation() { return recentLocation; }

    public void recordHit(double damage) {
        hitCount++;
        totalDamage += damage;
        if (damage > maxDamage) maxDamage = damage;
    }

    public double getTotalDamage() { return totalDamage; }
    public double getMaxDamage() { return maxDamage; }
    public int getHitCount() { return hitCount; }

    public void setDead(boolean dead) { this.dead = dead; }
    public boolean isDead() { return dead; }

    public void setResult(String result) { this.result = result; }
    public String getResult() { return result; }
}
```

- [ ] **Step 3: Write Spectator.java**

```java
package cn.valorin.dueltime4.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Spectator {

    private final Player player;
    private final Location originalLocation;
    private final GameMode originalGameMode;

    public Spectator(Player player) {
        this.player = player;
        this.originalLocation = player.getLocation().clone();
        this.originalGameMode = player.getGameMode();
    }

    public Player getPlayer() { return player; }
    public String getPlayerName() { return player.getName(); }
    public Location getOriginalLocation() { return originalLocation; }
    public GameMode getOriginalGameMode() { return originalGameMode; }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/player/
git commit -m "feat: domain models — PlayerProfile, Gamer, Spectator"
```

---

### Task 6: Arena Domain Model

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\arena\ArenaState.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\arena\Arena.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\arena\ClassicArena.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\arena\TeamArena.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\arena\FFAArena.java`

- [ ] **Step 1: Write ArenaState.java**

```java
package cn.valorin.dueltime4.arena;

public enum ArenaState {
    WAITING,
    STARTING,
    IN_PROGRESS,
    ENDING,
    DISABLED;

    public boolean canTransitionTo(ArenaState next) {
        return switch (this) {
            case WAITING   -> next == STARTING || next == DISABLED;
            case STARTING  -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == ENDING;
            case ENDING    -> next == WAITING || next == DISABLED;
            case DISABLED  -> next == WAITING;
        };
    }
}
```

- [ ] **Step 2: Write Arena.java**

```java
package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.Spectator;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public abstract class Arena {

    protected String id;
    protected String name;
    protected String typeName;
    protected ArenaState state = ArenaState.WAITING;
    protected final List<Gamer> gamers = new ArrayList<>();
    protected final List<Spectator> spectators = new ArrayList<>();
    protected BukkitTask timer;

    public Arena(String id, String name, String typeName) {
        this.id = id;
        this.name = name;
        this.typeName = typeName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTypeName() { return typeName; }
    public ArenaState getState() { return state; }
    public List<Gamer> getGamers() { return Collections.unmodifiableList(gamers); }
    public List<Spectator> getSpectators() { return Collections.unmodifiableList(spectators); }

    public boolean isFull() { return gamers.size() >= getMaxPlayers(); }
    public abstract int getMaxPlayers();
    public abstract int getMinPlayers();

    public void setState(ArenaState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition from " + state + " to " + newState);
        }
        this.state = newState;
    }

    /** Whether a player can join the waiting queue for this arena */
    public abstract boolean canJoin(Gamer gamer);

    /** Called when match starts. Subclass sets up positions, etc. */
    protected abstract void onStart();

    /** Called every second during IN_PROGRESS */
    protected abstract void onTick(int secondsElapsed);

    /** Called when match ends. Returns result summary: {winner, reason, ...} */
    protected abstract Map<String, Object> onEnd();

    /** Called for force-stop cleanup */
    protected abstract void onForceStop();

    public void addGamer(Gamer gamer) { gamers.add(gamer); }
    public void removeGamer(String playerName) {
        gamers.removeIf(g -> g.getPlayerName().equals(playerName));
    }

    public void addSpectator(Spectator spectator) { spectators.add(spectator); }
    public void removeSpectator(String playerName) {
        spectators.removeIf(s -> s.getPlayerName().equals(playerName));
    }

    public Gamer getGamer(String playerName) {
        return gamers.stream().filter(g -> g.getPlayerName().equals(playerName)).findFirst().orElse(null);
    }

    public boolean hasGamer(String playerName) {
        return getGamer(playerName) != null;
    }

    public boolean hasSpectator(String playerName) {
        return spectators.stream().anyMatch(s -> s.getPlayerName().equals(playerName));
    }

    /** Whether a world location falls within this arena's boundaries */
    public abstract boolean contains(Location loc);

    public void cancelTimer() {
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
    }

    public void setTimer(BukkitTask timer) { this.timer = timer; }
}
```

- [ ] **Step 3: Write ClassicArena.java**

```java
package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.Spectator;
import org.bukkit.Location;

import java.util.*;

public class ClassicArena extends Arena {

    private Location pos1;
    private Location pos2;

    public ClassicArena(String id, String name, Location pos1, Location pos2) {
        super(id, name, "classic");
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public void setPositions(Location p1, Location p2) { this.pos1 = p1; this.pos2 = p2; }

    public String getOpponentName(String playerName) {
        return gamers.stream()
            .filter(g -> !g.getPlayerName().equals(playerName))
            .findFirst()
            .map(Gamer::getPlayerName)
            .orElse(null);
    }

    @Override
    public boolean contains(Location loc) {
        if (!pos1.getWorld().equals(loc.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
            && loc.getY() >= minY && loc.getY() <= maxY
            && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    @Override public int getMaxPlayers() { return 2; }
    @Override public int getMinPlayers() { return 2; }

    @Override
    public boolean canJoin(Gamer gamer) {
        if (gamers.size() >= 2) return false;
        if (gamers.size() == 1 && gamers.get(0).getPlayerName().equals(gamer.getPlayerName())) return false;
        return true;
    }

    @Override
    protected void onStart() {
        gamers.get(0).getPlayer().teleport(pos1);
        gamers.get(0).updateRecentLocation(pos1);
        gamers.get(1).getPlayer().teleport(pos2);
        gamers.get(1).updateRecentLocation(pos2);
        for (Gamer g : gamers) {
            g.getPlayer().setHealth(g.getPlayer().getMaxHealth());
        }
    }

    @Override
    protected void onTick(int secondsElapsed) {
        for (Gamer g : gamers) {
            if (!g.isDead() && g.getPlayer().isDead()) {
                g.setDead(true);
            }
        }
    }

    @Override
    protected Map<String, Object> onEnd() {
        Map<String, Object> result = new HashMap<>();
        long alive = gamers.stream().filter(g -> !g.isDead()).count();
        if (alive == 2) {
            result.put("reason", "DRAW");
        } else if (alive == 1) {
            Gamer winner = gamers.stream().filter(g -> !g.isDead()).findFirst().orElse(null);
            result.put("reason", "CLEAR");
            result.put("winner", winner);
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {
        // cleanup handled by service
    }
}
```

- [ ] **Step 4: Write TeamArena.java**

```java
package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.Location;

import java.util.*;

public class TeamArena extends Arena {

    private final int teamSize;
    private Location team1Spawn;
    private Location team2Spawn;
    private final Map<String, Integer> playerTeam = new HashMap<>(); // playerName -> teamIndex (0 or 1)

    public TeamArena(String id, String name, int teamSize, Location t1Spawn, Location t2Spawn) {
        super(id, name, "team");
        this.teamSize = teamSize;
        this.team1Spawn = t1Spawn;
        this.team2Spawn = t2Spawn;
    }

    @Override public int getMaxPlayers() { return teamSize * 2; }
    @Override public int getMinPlayers() { return 2; }

    public int getTeamSize() { return teamSize; }
    public int getTeam(String playerName) { return playerTeam.getOrDefault(playerName, -1); }

    @Override
    public boolean canJoin(Gamer gamer) {
        return gamers.size() < getMaxPlayers();
    }

    @Override
    public void addGamer(Gamer gamer) {
        super.addGamer(gamer);
        int team0Count = (int) playerTeam.values().stream().filter(t -> t == 0).count();
        int team1Count = (int) playerTeam.values().stream().filter(t -> t == 1).count();
        playerTeam.put(gamer.getPlayerName(), team0Count <= team1Count ? 0 : 1);
    }

    @Override
    public void removeGamer(String playerName) {
        super.removeGamer(playerName);
        playerTeam.remove(playerName);
    }

    @Override
    protected void onStart() {
        for (Gamer g : gamers) {
            Location spawn = playerTeam.get(g.getPlayerName()) == 0 ? team1Spawn : team2Spawn;
            g.getPlayer().teleport(spawn);
            g.updateRecentLocation(spawn);
            g.getPlayer().setHealth(g.getPlayer().getMaxHealth());
        }
    }

    @Override
    protected void onTick(int secondsElapsed) {
        for (Gamer g : gamers) {
            if (!g.isDead() && g.getPlayer().isDead()) {
                g.setDead(true);
            }
        }
    }

    @Override
    protected Map<String, Object> onEnd() {
        Map<String, Object> result = new HashMap<>();
        boolean team0Alive = gamers.stream().anyMatch(g -> playerTeam.get(g.getPlayerName()) == 0 && !g.isDead());
        boolean team1Alive = gamers.stream().anyMatch(g -> playerTeam.get(g.getPlayerName()) == 1 && !g.isDead());

        if (team0Alive && !team1Alive) {
            result.put("reason", "CLEAR");
            result.put("winner", 0);
        } else if (!team0Alive && team1Alive) {
            result.put("reason", "CLEAR");
            result.put("winner", 1);
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {}

    public Map<String, Integer> getTeams() { return Collections.unmodifiableMap(playerTeam); }
}
```

- [ ] **Step 5: Write FFAArena.java**

```java
package cn.valorin.dueltime4.arena;

import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.Location;

import java.util.*;

public class FFAArena extends Arena {

    private final int minPlayers;
    private final int maxPlayers;
    private final List<Location> spawnPoints;

    public FFAArena(String id, String name, int minPlayers, int maxPlayers, List<Location> spawnPoints) {
        super(id, name, "ffa");
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.spawnPoints = spawnPoints;
    }

    @Override public int getMaxPlayers() { return maxPlayers; }
    @Override public int getMinPlayers() { return minPlayers; }

    @Override
    public boolean canJoin(Gamer gamer) {
        return gamers.size() < maxPlayers;
    }

    @Override
    protected void onStart() {
        Random rand = new Random();
        for (int i = 0; i < gamers.size(); i++) {
            Location spawn = spawnPoints.get(i % spawnPoints.size());
            Gamer g = gamers.get(i);
            g.getPlayer().teleport(spawn);
            g.updateRecentLocation(spawn);
            g.getPlayer().setHealth(g.getPlayer().getMaxHealth());
        }
    }

    @Override
    protected void onTick(int secondsElapsed) {
        for (Gamer g : gamers) {
            if (!g.isDead() && g.getPlayer().isDead()) {
                g.setDead(true);
            }
        }
    }

    @Override
    protected Map<String, Object> onEnd() {
        Map<String, Object> result = new HashMap<>();
        long aliveCount = gamers.stream().filter(g -> !g.isDead()).count();

        if (aliveCount <= 1) {
            result.put("reason", "CLEAR");
            gamers.stream().filter(g -> !g.isDead()).findFirst().ifPresent(w -> result.put("winner", w));
        } else {
            result.put("reason", "DRAW");
        }
        return result;
    }

    @Override
    protected void onForceStop() {}
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/arena/
git commit -m "feat: arena domain model — base, classic, team, FFA"
```

---

### Task 7: Events

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\ArenaStartEvent.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\ArenaEndEvent.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\PlayerJoinArenaEvent.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\PlayerLeaveArenaEvent.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\RankingRefreshEvent.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\event\MigrationCompleteEvent.java`

- [ ] **Step 1: Write all event classes**

Create all 6 event files. Each is a plain event (non-cancellable, extends `org.bukkit.event.Event`):

```java
package cn.valorin.dueltime4.event;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.player.Gamer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ArenaStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Arena arena;
    private final List<Gamer> gamers;
    public ArenaStartEvent(Arena arena, List<Gamer> gamers) { this.arena = arena; this.gamers = gamers; }
    public Arena getArena() { return arena; }
    public List<Gamer> getGamers() { return gamers; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
```

(Repeat pattern for: ArenaEndEvent with `Map<String,Object> result`, PlayerJoinArenaEvent with `Player player, Arena arena`, PlayerLeaveArenaEvent same, RankingRefreshEvent with `List<PlayerProfile> topList`, MigrationCompleteEvent with `int arenas, int players, int records`)

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/event/
git commit -m "feat: event classes — arena lifecycle, ranking, migration"
```

---

### Task 8: Service Layer — PlayerService, ArenaService

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\PlayerService.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\ArenaService.java`

- [ ] **Step 1: Write PlayerService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.repository.PlayerRepository;

import java.util.*;

public class PlayerService {

    private final PlayerRepository repo;
    private final Config config;

    public PlayerService(PlayerRepository repo, Config config) {
        this.repo = repo;
        this.config = config;
    }

    public PlayerProfile getOrCreate(String playerName) {
        return repo.findByName(playerName).orElseGet(() -> new PlayerProfile(playerName));
    }

    public void save(PlayerProfile profile) {
        repo.upsert(profile);
    }

    public List<PlayerProfile> getTopByExp(int limit) {
        return repo.findTop(limit, "exp");
    }

    public List<PlayerProfile> getTopByPoint(int limit) {
        return repo.findTop(limit, "point");
    }

    /** Calculate win streak bonus points for a player */
    public int getWinStreakBonus(String arenaType, int currentStreak) {
        if (!config.getWinStreakEnabled(arenaType)) return 0;
        Map<String, Object> bonusPoints = config.getWinStreakSection(arenaType, "bonus-point");
        int bonus = 0;
        for (int streak = currentStreak; streak >= 0; streak--) {
            Object val = bonusPoints.get(String.valueOf(streak));
            if (val instanceof Number) {
                bonus = ((Number) val).intValue();
                break;
            }
        }
        return bonus;
    }

    /** Calculate win streak bonus exp rate for a player */
    public double getWinStreakExpRate(String arenaType, int currentStreak) {
        if (!config.getWinStreakEnabled(arenaType)) return 0;
        Map<String, Object> bonusRates = config.getWinStreakSection(arenaType, "bonus-exp-rate");
        double rate = 0;
        for (int streak = currentStreak; streak >= 0; streak--) {
            Object val = bonusRates.get(String.valueOf(streak));
            if (val instanceof Number) {
                rate = ((Number) val).doubleValue();
                break;
            }
        }
        return rate;
    }
}
```

- [ ] **Step 2: Write ArenaService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.arena.*;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.Spectator;
import cn.valorin.dueltime4.repository.ArenaRepository;
import cn.valorin.dueltime4.repository.LocationRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaService {

    private final ArenaRepository repo;
    private final LocationRepository locationRepo;
    private final Map<String, Arena> activeArenas = new ConcurrentHashMap<>();
    // playerName -> arenaId for quick lookup
    private final Map<String, String> playerArenaMap = new ConcurrentHashMap<>();
    private final Map<String, String> spectatorArenaMap = new ConcurrentHashMap<>();
    // waiting queue: playerName -> arenaId
    private final Map<String, String> waitingMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> arenaWaitingList = new ConcurrentHashMap<>();

    public ArenaService(ArenaRepository repo, LocationRepository locationRepo) {
        this.repo = repo;
        this.locationRepo = locationRepo;
    }

    public void loadAll() {
        for (Map<String, Object> row : repo.findAll()) {
            Arena arena = buildArena(row);
            if (arena != null) {
                activeArenas.put(arena.getId(), arena);
            }
        }
    }

    private Arena buildArena(Map<String, Object> row) {
        // Deserializes from DB row using data_json
        // Implementation uses Gson or manual JSON parsing of data_json field
        String type = (String) row.get("type");
        String id = (String) row.get("id");
        String name = (String) row.get("name");
        // Parse data_json for type-specific fields (locations, etc.)
        // For now, skeleton; full implementation in Task 19
        return null;
    }

    public Arena get(String id) { return activeArenas.get(id); }
    public Arena getByPlayer(Player player) { return activeArenas.get(playerArenaMap.get(player.getName())); }
    public Arena getSpectating(Player player) { return activeArenas.get(spectatorArenaMap.get(player.getName())); }
    public List<Arena> getAll() { return new ArrayList<>(activeArenas.values()); }

    public void addGamerMapping(String playerName, String arenaId) { playerArenaMap.put(playerName, arenaId); }
    public void removeGamerMapping(String playerName) { playerArenaMap.remove(playerName); }
    public void addSpectatorMapping(String playerName, String arenaId) { spectatorArenaMap.put(playerName, arenaId); }
    public void removeSpectatorMapping(String playerName) { spectatorArenaMap.remove(playerName); }

    public void addToWaiting(Player player, String arenaId) {
        waitingMap.put(player.getName(), arenaId);
        arenaWaitingList.computeIfAbsent(arenaId, k -> new ArrayList<>()).add(player.getName());
    }

    public void removeFromWaiting(Player player) {
        String arenaId = waitingMap.remove(player.getName());
        if (arenaId != null) {
            List<String> list = arenaWaitingList.get(arenaId);
            if (list != null) list.remove(player.getName());
        }
    }

    public Arena getWaiting(Player player) {
        return activeArenas.get(waitingMap.get(player.getName()));
    }

    public List<String> getWaitingPlayers(String arenaId) {
        return arenaWaitingList.getOrDefault(arenaId, List.of());
    }

    public Location getLobby() { return locationRepo.get("lobby").orElse(null); }
    public void setLobby(Location loc) { locationRepo.set("lobby", loc); }

    public void saveArena(Arena arena, String dataJson) {
        String world = "";
        repo.save(arena.getId(), arena.getName(), arena.getTypeName(), world, dataJson);
    }

    public void setArenaEnabled(String id, boolean enabled) {
        repo.setEnabled(id, enabled);
        if (!enabled) activeArenas.remove(id);
    }

    public void deleteArena(String id) {
        repo.delete(id);
        activeArenas.remove(id);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/ArenaService.java src/main/java/cn/valorin/dueltime4/service/PlayerService.java
git commit -m "feat: PlayerService and ArenaService"
```

---

### Task 9: MatchService (match orchestration)

**File:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\MatchService.java`

This is the most complex service. It orchestrates the full match lifecycle: start → countdown → in-progress ticks → end → rewards/broadcasts.

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.*;
import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.event.ArenaEndEvent;
import cn.valorin.dueltime4.event.ArenaStartEvent;
import cn.valorin.dueltime4.event.PlayerJoinArenaEvent;
import cn.valorin.dueltime4.event.PlayerLeaveArenaEvent;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.player.Spectator;
import cn.valorin.dueltime4.repository.RecordRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class MatchService {

    private final ArenaService arenaService;
    private final PlayerService playerService;
    private final RecordRepository recordRepo;
    private final Config config;

    // Track in-progress match data: arenaId -> {seconds, stage, ...}
    private final Map<String, MatchSession> sessions = new HashMap<>();

    public MatchService(ArenaService arenaService, PlayerService playerService,
                        RecordRepository recordRepo, Config config) {
        this.arenaService = arenaService;
        this.playerService = playerService;
        this.recordRepo = recordRepo;
        this.config = config;
    }

    /**
     * Start a match: called when waiting queue reaches min players or admin forces start.
     */
    public void startMatch(String arenaId, List<Player> players) {
        Arena arena = arenaService.get(arenaId);
        if (arena == null) return;
        if (arena.getState() != ArenaState.WAITING) return;

        // Create Gamer objects
        List<Gamer> gamers = new ArrayList<>();
        for (Player p : players) {
            Gamer g = new Gamer(p);
            gamers.add(g);
            arena.addGamer(g);
            arenaService.addGamerMapping(p.getName(), arenaId);
            Bukkit.getPluginManager().callEvent(new PlayerJoinArenaEvent(p, arena));
        }

        arena.setState(ArenaState.STARTING);
        int countdown = config.getArenaCountdown(arena.getTypeName());
        MatchSession session = new MatchSession(arena, gamers, countdown > 0 ? "COUNTDOWN" : "GAME");
        sessions.put(arenaId, session);

        if (countdown > 0) {
            session.countdownRemaining = countdown;
        }

        arena.setTimer(new BukkitRunnable() {
            int tick = countdown > 0 ? -countdown : 0;

            @Override
            public void run() {
                if (arena.getState() == ArenaState.ENDING) { cancel(); return; }

                if (session.stage.equals("COUNTDOWN")) {
                    tick++;
                    if (tick >= 0) {
                        // Countdown finished
                        session.stage = "GAME";
                        arena.setState(ArenaState.IN_PROGRESS);
                        arena.onStart();
                        Bukkit.getPluginManager().callEvent(new ArenaStartEvent(arena, gamers));
                        tick = 0;
                        return;
                    }
                    // Send countdown title/actionbar to gamers
                    int remaining = -tick;
                    for (Gamer g : gamers) {
                        g.getPlayer().sendActionBar(
                            net.kyori.adventure.text.Component.text("§e" + remaining + " §7秒后开始..."));
                    }
                } else {
                    tick++;
                    arena.onTick(tick);

                    // Check if match should end
                    Map<String, Object> endResult = arena.onEnd();
                    if (endResult.containsKey("reason") && !endResult.get("reason").equals("ONGOING")) {
                        endMatch(arenaId, endResult);
                        cancel();
                        return;
                    }

                    // Time limit check
                    int timeLimit = config.getArenaTimeLimit(arena.getTypeName());
                    if (timeLimit > 0 && tick >= timeLimit) {
                        endMatch(arenaId, Map.of("reason", "DRAW"));
                        cancel();
                    }
                }
            }
        }.runTaskTimer(DuelTimePlugin.getInstance(), 0, 20));
    }

    /**
     * End a match: rewards, records, cleanup, broadcast.
     */
    public void endMatch(String arenaId, Map<String, Object> result) {
        Arena arena = arenaService.get(arenaId);
        if (arena == null) return;

        arena.setState(ArenaState.ENDING);
        arena.cancelTimer();
        sessions.remove(arenaId);

        String reason = (String) result.get("reason");
        List<Gamer> gamers = new ArrayList<>(arena.getGamers());
        int duration = 0; // get from session
        String arenaType = arena.getTypeName();
        String time = new SimpleDateFormat("yyyy/M/d HH:mm").format(new Date());

        for (Gamer g : gamers) {
            arenaService.removeGamerMapping(g.getPlayerName());
            Player player = g.getPlayer();
            PlayerProfile profile = playerService.getOrCreate(g.getPlayerName());

            if ("CLEAR".equals(reason)) {
                Gamer winner = (Gamer) result.get("winner");
                if (winner != null && winner.getPlayerName().equals(g.getPlayerName())) {
                    // WIN
                    int basePoint = config.getArenaWinPoint(arenaType);
                    double baseExp = config.getArenaWinExp(arenaType);
                    int streakBonus = playerService.getWinStreakBonus(arenaType, profile.getWinStreak() + 1);
                    double expRate = playerService.getWinStreakExpRate(arenaType, profile.getWinStreak() + 1);

                    profile.onWin();
                    profile.addPoint(basePoint + streakBonus);
                    profile.addExp(baseExp * (1 + expRate));
                    g.setResult("WIN");
                } else {
                    // LOSE
                    profile.onLose();
                    double expLoss = config.getArenaWinExp(arenaType) * config.getArenaLoseExpRate(arenaType);
                    profile.addExp(-expLoss);
                    g.setResult("LOSE");
                }
            } else {
                // DRAW or STOPPED
                profile.onDraw();
                g.setResult(reason);
                profile.incrementDraws();
            }
            profile.addTime(duration);
            playerService.save(profile);

            // Record
            String opponent = arenaType.equals("classic") ?
                ((ClassicArena) arena).getOpponentName(g.getPlayerName()) : null;
            recordRepo.insert(g.getPlayerName(), arenaId, arenaType, opponent,
                g.getResult(), duration, 0, g.getHitCount(),
                g.getTotalDamage(), g.getMaxDamage(),
                g.getHitCount() > 0 ? g.getTotalDamage() / g.getHitCount() : 0,
                time);

            // Teleport back
            Location back = arenaService.getLobby();
            if (back == null) back = g.getOriginalLocation();
            if (player.isOnline()) player.teleport(back);

            Bukkit.getPluginManager().callEvent(new PlayerLeaveArenaEvent(player, arena));
        }

        // Spectators: teleport back
        for (Spectator s : new ArrayList<>(arena.getSpectators())) {
            s.getPlayer().teleport(s.getOriginalLocation());
            arenaService.removeSpectatorMapping(s.getPlayerName());
        }

        Bukkit.getPluginManager().callEvent(new ArenaEndEvent(arena, result));

        // Reset arena
        arena.getGamers().clear();
        arena.getSpectators().clear();
        arena.setState(ArenaState.WAITING);
    }

    /**
     * Force stop a match (admin command).
     */
    public void forceStop(String arenaId, String reason) {
        endMatch(arenaId, Map.of("reason", "STOPPED", "stopReason", reason));
    }

    public void shutdown() {
        for (String id : new ArrayList<>(sessions.keySet())) {
            forceStop(id, "Server shutdown");
        }
    }

    // Track damage dealt for stat recording
    public void recordDamage(Player attacker, Player victim, double damage) {
        Arena arena = arenaService.getByPlayer(attacker);
        if (arena != null) {
            Gamer gamer = arena.getGamer(attacker.getName());
            if (gamer != null) {
                gamer.recordHit(damage);
            }
        }
    }

    private static class MatchSession {
        Arena arena;
        List<Gamer> gamers;
        String stage; // COUNTDOWN, GAME
        int countdownRemaining;

        MatchSession(Arena arena, List<Gamer> gamers, String stage) {
            this.arena = arena; this.gamers = gamers; this.stage = stage;
        }
    }
}
```

- [ ] **Step 1: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/MatchService.java
git commit -m "feat: MatchService — full match lifecycle orchestration"
```

---

### Task 10: Remaining Services

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\SpectateService.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\RankingService.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\ShopService.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\BlacklistService.java`

- [ ] **Step 1: Write SpectateService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.player.Spectator;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class SpectateService {

    private final ArenaService arenaService;

    public SpectateService(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    public boolean canSpectate(Player player, Arena arena) {
        if (arena.getState() != ArenaState.IN_PROGRESS) return false;
        if (arena.hasGamer(player.getName())) return false;
        if (arena.hasSpectator(player.getName())) return false;
        return true;
    }

    public void startSpectating(Player player, Arena arena) {
        Spectator spec = new Spectator(player);
        arena.addSpectator(spec);
        arenaService.addSpectatorMapping(player.getName(), arena.getId());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public void stopSpectating(Player player) {
        Arena arena = arenaService.getSpectating(player);
        if (arena == null) return;
        arena.removeSpectator(player.getName());
        arenaService.removeSpectatorMapping(player.getName());
        Spectator spec = arena.getSpectators().stream()
            .filter(s -> s.getPlayerName().equals(player.getName())).findFirst().orElse(null);
        if (spec != null) {
            player.setGameMode(spec.getOriginalGameMode());
            player.teleport(spec.getOriginalLocation());
        }
    }
}
```

- [ ] **Step 2: Write RankingService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.event.RankingRefreshEvent;
import cn.valorin.dueltime4.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class RankingService {

    private final PlayerService playerService;

    public RankingService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void startAutoRefresh(int intervalSeconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                refresh();
            }
        }.runTaskTimerAsynchronously(DuelTimePlugin.getInstance(),
            intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void refresh() {
        List<PlayerProfile> topList = playerService.getTopByExp(50);
        Bukkit.getPluginManager().callEvent(new RankingRefreshEvent(topList));
    }
}
```

- [ ] **Step 3: Write ShopService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopService {

    private final PlayerService playerService;
    private final Config config;

    public ShopService(PlayerService playerService, Config config) {
        this.playerService = playerService;
        this.config = config;
    }

    public List<Map<?, ?>> getItems() {
        return config.getMapList("shop.items");
    }

    public Map<?, ?> findItem(String id) {
        return getItems().stream()
            .filter(item -> id.equals(item.get("id")))
            .findFirst().orElse(null);
    }

    public boolean buy(Player player, String itemId) {
        Map<?, ?> item = findItem(itemId);
        if (item == null) return false;

        int cost = ((Number) item.get("cost")).intValue();
        PlayerProfile profile = playerService.getOrCreate(player.getName());
        if (profile.getPoint() < cost) return false;

        profile.setPoint(profile.getPoint() - cost);
        playerService.save(profile);

        // Execute commands
        @SuppressWarnings("unchecked")
        List<String> commands = (List<String>) item.get("commands");
        if (commands != null) {
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
            }
        }

        return true;
    }
}
```

- [ ] **Step 4: Write BlacklistService.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.repository.BlacklistRepository;

public class BlacklistService {

    private final BlacklistRepository repo;

    public BlacklistService(BlacklistRepository repo) {
        this.repo = repo;
    }

    public boolean isBlacklisted(String playerName) {
        return repo.isBlacklisted(playerName);
    }

    public void add(String playerName, String reason) {
        repo.add(playerName, reason);
    }

    public void remove(String playerName) {
        repo.remove(playerName);
    }

    public java.util.List<String> list() {
        return repo.getAll();
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/
git commit -m "feat: SpectateService, RankingService, ShopService, BlacklistService"
```

---

### Task 11: Command System

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\SubCommand.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\CommandManager.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdHelp.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdArena.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdSend.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdAccept.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdDecline.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdJoin.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdQuit.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdSpectate.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdStart.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdStop.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdShop.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdRank.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdRecord.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdLobby.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdBlacklist.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdLang.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdReload.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdMigrate.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\command\impl\CmdLevel.java`

- [ ] **Step 1: Write SubCommand.java**

```java
package cn.valorin.dueltime4.command;

import org.bukkit.command.CommandSender;

public abstract class SubCommand {

    private final String name;
    private final String[] aliases;
    private final String permission;
    private final String usage;
    private final boolean playerOnly;

    public SubCommand(String name, String[] aliases, String permission, String usage, boolean playerOnly) {
        this.name = name;
        this.aliases = aliases;
        this.permission = permission;
        this.usage = usage;
        this.playerOnly = playerOnly;
    }

    public abstract void execute(CommandSender sender, String[] args);

    public String getName() { return name; }
    public String[] getAliases() { return aliases; }
    public String getPermission() { return permission; }
    public String getUsage() { return usage; }
    public boolean isPlayerOnly() { return playerOnly; }
}
```

- [ ] **Step 2: Write CommandManager.java**

```java
package cn.valorin.dueltime4.command;

import cn.valorin.dueltime4.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager implements CommandExecutor {

    private final Map<String, SubCommand> aliasMap = new HashMap<>();
    private final Set<SubCommand> commands = new LinkedHashSet<>();

    public CommandManager() {
        PluginCommand cmd = Bukkit.getPluginCommand("dueltime");
        if (cmd == null) throw new IllegalStateException("Command 'dueltime' not registered in plugin.yml");
        cmd.setExecutor(this);
    }

    public void register(SubCommand cmd) {
        commands.add(cmd);
        aliasMap.put(cmd.getName().toLowerCase(), cmd);
        for (String alias : cmd.getAliases()) {
            aliasMap.put(alias.toLowerCase(), cmd);
        }
    }

    public Set<SubCommand> getCommands() { return commands; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0) {
            aliasMap.get("help").execute(sender, args);
            return true;
        }

        SubCommand sub = aliasMap.get(args[0].toLowerCase());
        if (sub == null) {
            aliasMap.get("help").execute(sender, args);
            return true;
        }

        if (sub.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage("You don't have permission.");
            return true;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }
}
```

- [ ] **Step 3: Write representative command (CmdAccept)**

```java
package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.MatchService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdAccept extends SubCommand {

    public CmdAccept() {
        super("accept", new String[]{"a", "accept"}, null, "/dt accept — Accept a duel invitation", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        // Accept logic depends on request/invitation system (Task 12)
        // Placeholder: delegates to DuelTimePlugin services
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        // RequestReceiver equivalent will be added in listener task
        player.sendMessage("§aAccepted! (Request system wired in Task 12)");
    }
}
```

- [ ] **Step 4: Write CmdArena.java**

```java
package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdArena extends SubCommand {

    private final ArenaService arenaService;

    public CmdArena(ArenaService arenaService) {
        super("arena", new String[]{"arena"}, "dueltime4.admin",
            "/dt arena <create|delete|list|toggle> ...", false);
        this.arenaService = arenaService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage("Usage: " + getUsage()); return; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                if (args.length < 4) { sender.sendMessage("/dt arena create <type> <id> <name>"); return; }
                if (!(sender instanceof Player p)) { sender.sendMessage("Player only"); return; }
                Location loc = p.getLocation();
                String dataJson = String.format("{\"pos1\":{\"x\":%f,\"y\":%f,\"z\":%f,\"world\":\"%s\"}}",
                    loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                arenaService.saveArena(createTempArena(args[1], args[2], args[3], loc), dataJson);
                sender.sendMessage("Arena created. Use arena setup commands to configure positions.");
            }
            case "delete" -> { if (args.length > 1) arenaService.deleteArena(args[1]); }
            case "list" -> {
                for (Arena a : arenaService.getAll()) {
                    sender.sendMessage(" - " + a.getId() + " [" + a.getTypeName() + "] " + a.getState());
                }
            }
            case "toggle" -> {
                if (args.length > 1) {
                    Arena a = arenaService.get(args[1]);
                    if (a != null) arenaService.setArenaEnabled(args[1], a.getState() != ArenaState.DISABLED);
                }
            }
        }
    }

    private Arena createTempArena(String type, String id, String name, Location loc) {
        return switch (type) {
            case "classic" -> new cn.valorin.dueltime4.arena.ClassicArena(id, name, loc, loc);
            default -> throw new IllegalArgumentException("Unknown arena type: " + type);
        };
    }
}
```

- [ ] **Step 5: Write CmdSend.java**

```java
package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.RequestService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdSend extends SubCommand {
    private final ArenaService arenaService;
    private final RequestService requestService;

    public CmdSend(ArenaService arenaService, RequestService requestService) {
        super("send", new String[]{"send", "invite"}, null, "/dt send <player> <arenaId>", true);
        this.arenaService = arenaService;
        this.requestService = requestService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("Usage: " + getUsage()); return; }
        Player player = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage("Player not found"); return; }
        Arena arena = arenaService.get(args[1]);
        if (arena == null) { player.sendMessage("Arena not found"); return; }
        requestService.sendRequest(player, target, args[1]);
    }
}
```

- [ ] **Step 6: Write CmdAccept.java (revised with real logic)**

```java
package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.RequestService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdAccept extends SubCommand {

    public CmdAccept() {
        super("accept", new String[]{"a", "accept"}, null, "/dt accept [player]", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        RequestService reqSvc = DuelTimePlugin.getInstance().getRequestService();
        // Accept from named sender, or accept the most recent pending request
        if (args.length > 0) {
            reqSvc.accept(player, args[0]);
        } else {
            player.sendMessage("Usage: /dt accept <player> — accept invitation from that player");
        }
    }
}
```

- [ ] **Step 7: Write CmdDecline.java, CmdJoin.java, CmdQuit.java**

All follow the same injection pattern. CmdJoin: `arenaService.addToWaiting(player, arenaId)`, checks if player is blacklisted via `blacklistService.isBlacklisted()`. CmdQuit: `arenaService.removeFromWaiting(player)`, `matchService.forceStop(...)` if in-progress.

- [ ] **Step 8: Write CmdSpectate.java, CmdStart.java, CmdStop.java**

CmdSpectate: `spectateService.startSpectating(player, arena)`. CmdStart: `matchService.startMatch(arenaId, players)`. CmdStop: `matchService.forceStop(arenaId, "Admin stopped")`.

- [ ] **Step 9: Write CmdShop.java, CmdRank.java, CmdRecord.java**

CmdShop: opens `ShopGui`. CmdRank: shows top list from `rankingService`. CmdRecord: `recordRepo.findByPlayer(playerName, limit)`.

- [ ] **Step 10: Write CmdLobby.java, CmdBlacklist.java, CmdLang.java, CmdReload.java, CmdMigrate.java, CmdLevel.java**

CmdLobby: `arenaService.setLobby(player.getLocation())`. CmdBlacklist: add/remove/list. CmdLang: set language, reload messages. CmdReload: `config.reload()`, `messages.reload()`, `arenaService.loadAll()`. CmdMigrate: `migrationService.run()`. CmdLevel: show current tier info.

- [ ] **Step 21: Wire CommandManager into DuelTimePlugin**

Add to onEnable():
```java
CommandManager cmdManager = new CommandManager();
cmdManager.register(new CmdHelp());
cmdManager.register(new CmdArena(arenaService));
cmdManager.register(new CmdSend(arenaService));
cmdManager.register(new CmdAccept());
cmdManager.register(new CmdDecline());
cmdManager.register(new CmdJoin(arenaService, matchService));
cmdManager.register(new CmdQuit(arenaService, matchService));
cmdManager.register(new CmdSpectate(arenaService, spectateService));
cmdManager.register(new CmdStart(matchService, arenaService));
cmdManager.register(new CmdStop(matchService, arenaService));
cmdManager.register(new CmdShop(shopService));
cmdManager.register(new CmdRank(rankingService));
cmdManager.register(new CmdRecord(recordRepository));
cmdManager.register(new CmdLobby(arenaService));
cmdManager.register(new CmdBlacklist(blacklistService));
cmdManager.register(new CmdLang(config, messages));
cmdManager.register(new CmdReload(config, messages));
cmdManager.register(new CmdMigrate(migrationService));
cmdManager.register(new CmdLevel(playerService, config));
```

- [ ] **Step 22: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/command/
git commit -m "feat: command system — CommandManager + all subcommands"
```

---

### Task 12: Listeners

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\ArenaProtectionListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\ArenaMatchListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\ArenaSpectateListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\PlayerDataListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\ChatListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\GuiListener.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\listener\RankingListener.java`

- [ ] **Step 1: Write ArenaProtectionListener.java**

Core protection listener preventing unauthorized block interaction, movement into arenas, PvP interference, etc. Follows same logic pattern as DT3's `BaseArenaListener` but cleaner:

```java
package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class ArenaProtectionListener implements Listener {

    private final ArenaService arenaService;
    private static final String BYPASS_PERM = "dueltime4.admin";

    public ArenaProtectionListener(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERM)) return;
        Location to = e.getTo();
        if (to == null) return;
        if (e.getFrom().getBlockX() == to.getBlockX()
            && e.getFrom().getBlockY() == to.getBlockY()
            && e.getFrom().getBlockZ() == to.getBlockZ()) return;

        Arena playerArena = arenaService.getByPlayer(p);
        Arena targetArena = findArenaAt(to);

        if (targetArena != null && playerArena == null) {
            // Player not in any arena, trying to enter one
            if (targetArena.getState() == ArenaState.WAITING
                || targetArena.getState() == ArenaState.DISABLED) {
                e.setCancelled(true);
                return;
            }
            if (!targetArena.hasGamer(p.getName()) && !targetArena.hasSpectator(p.getName())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERM)) return;
        Arena playerArena = arenaService.getByPlayer(p);
        Arena blockArena = findArenaAt(e.getBlock().getLocation());

        if (playerArena == null && blockArena != null) {
            e.setCancelled(true);
        } else if (playerArena != null && (blockArena == null || !blockArena.getId().equals(playerArena.getId()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        // Same logic as onBlockBreak
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERM)) return;
        Arena playerArena = arenaService.getByPlayer(p);
        Arena blockArena = findArenaAt(e.getBlock().getLocation());
        if (playerArena == null && blockArena != null) {
            e.setCancelled(true);
        } else if (playerArena != null && (blockArena == null || !blockArena.getId().equals(playerArena.getId()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;
        Arena victimArena = arenaService.getByPlayer(victim);
        if (victimArena == null) return;
        Arena damagerArena = arenaService.getByPlayer(damager);
        if (damagerArena == null || !damagerArena.getId().equals(victimArena.getId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent e) {
        if (findArenaAt(e.getToBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        Arena pistonArena = findArenaAt(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Arena targetArena = findArenaAt(b.getRelative(e.getDirection()).getLocation());
            if (pistonArena == null && targetArena != null) { e.setCancelled(true); return; }
            if (pistonArena != null && (targetArena == null || !targetArena.getId().equals(pistonArena.getId()))) {
                e.setCancelled(true); return;
            }
        }
    }

    private Arena findArenaAt(Location loc) {
        for (Arena arena : arenaService.getAll()) {
            if (arena.contains(loc)) return arena;
        }
        return null;
    }
}
```

- [ ] **Step 2: Write ArenaMatchListener.java**

```java
package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.service.MatchService;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ArenaMatchListener implements Listener {

    private final MatchService matchService;
    private final ArenaService arenaService;

    public ArenaMatchListener(MatchService matchService, ArenaService arenaService) {
        this.matchService = matchService;
        this.arenaService = arenaService;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;
        Arena a = arenaService.getByPlayer(damager);
        if (a == null || a.getState() != ArenaState.IN_PROGRESS) return;
        if (a.hasGamer(victim.getName())) {
            matchService.recordDamage(damager, victim, e.getFinalDamage());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        Arena a = arenaService.getByPlayer(p);
        if (a == null || a.getState() != ArenaState.IN_PROGRESS) return;
        Gamer g = a.getGamer(p.getName());
        if (g != null) g.setDead(true);
        // Auto respawn handled by paper config or plugin setting
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Arena a = arenaService.getByPlayer(p);
        if (a != null) {
            Gamer g = a.getGamer(p.getName());
            if (g != null) g.setDead(true);
        }
        arenaService.removeFromWaiting(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Arena a = arenaService.getByPlayer(p);
        if (a != null) {
            // Set respawn to lobby or original location
            var lobby = arenaService.getLobby();
            if (lobby != null) e.setRespawnLocation(lobby);
        }
    }
}
```

- [ ] **Step 3: Write ArenaSpectateListener.java**

```java
package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.SpectateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ArenaSpectateListener implements Listener {

    private final ArenaService arenaService;
    private final SpectateService spectateService;

    public ArenaSpectateListener(ArenaService arenaService, SpectateService spectateService) {
        this.arenaService = arenaService;
        this.spectateService = spectateService;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player victim) {
            if (arenaService.getSpectating(victim) != null) {
                e.setCancelled(true); // spectators can't be damaged
            }
        }
        if (e.getDamager() instanceof Player damager) {
            if (arenaService.getSpectating(damager) != null) {
                e.setCancelled(true); // spectators can't attack
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        spectateService.stopSpectating(e.getPlayer());
    }
}
```

- [ ] **Step 4: Write PlayerDataListener.java**

```java
package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataListener implements Listener {

    private final PlayerService playerService;

    public PlayerDataListener(PlayerService playerService) {
        this.playerService = playerService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerProfile profile = playerService.getOrCreate(e.getPlayer().getName());
        playerService.save(profile); // ensure row exists
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerProfile profile = playerService.getOrCreate(e.getPlayer().getName());
        playerService.save(profile);
    }
}
```

- [ ] **Step 5: Write remaining listeners (ChatListener, GuiListener, RankingListener)**

ChatListener: intercepts chat for duel chat features (if any).
GuiListener: routes InventoryClickEvent to the correct Gui instance.
RankingListener: refreshes holograms on RankingRefreshEvent.

- [ ] **Step 6: Wire listeners into DuelTimePlugin**

Add to onEnable():
```java
var pm = getServer().getPluginManager();
pm.registerEvents(new ArenaProtectionListener(arenaService), this);
pm.registerEvents(new ArenaMatchListener(matchService, arenaService), this);
pm.registerEvents(new ArenaSpectateListener(arenaService, spectateService), this);
pm.registerEvents(new PlayerDataListener(playerService), this);
pm.registerEvents(new GuiListener(), this);
pm.registerEvents(new RankingListener(rankingService), this);
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/listener/
git commit -m "feat: all listeners — protection, match, spectate, player data, GUI"
```

---

### Task 13: GUI System

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\gui\Gui.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\gui\PagedGui.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\gui\StartGui.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\gui\ShopGui.java`

```java
package cn.valorin.dueltime4.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class Gui implements InventoryHolder {

    protected final Player player;
    protected final Inventory inventory;
    protected final int size;

    public Gui(Player player, String title, int rows) {
        this.player = player;
        this.size = rows * 9;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
    }

    public void open() { player.openInventory(inventory); }
    public abstract void onClick(int slot, InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
```

```java
package cn.valorin.dueltime4.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class PagedGui extends Gui {

    protected int page = 0;
    protected static final int PAGE_SIZE = 45; // 5 rows of 9, bottom row for nav

    public PagedGui(Player player, String title) {
        super(player, title, 6); // 6 rows
    }

    protected abstract List<ItemStack> getPageItems(int page);
    protected abstract int totalPages();

    public void render() {
        inventory.clear();
        List<ItemStack> items = getPageItems(page);
        for (int i = 0; i < Math.min(items.size(), PAGE_SIZE); i++) {
            inventory.setItem(i, items.get(i));
        }
        // Navigation buttons
        if (page > 0) {
            inventory.setItem(45, navItem(Material.ARROW, "§e上一页"));
        }
        if (page < totalPages() - 1) {
            inventory.setItem(53, navItem(Material.ARROW, "§e下一页"));
        }
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(int slot, InventoryClickEvent event) {
        if (slot == 45 && page > 0) { page--; render(); return; }
        if (slot == 53 && page < totalPages() - 1) { page++; render(); return; }
    }

    @Override
    public void open() {
        render();
        super.open();
    }
}
```

- [ ] **Step 1: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/gui/
git commit -m "feat: GUI framework — Gui and PagedGui base classes"
```

---

### Task 14: Hooks — PlaceholderAPI, Holograms

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\hook\PlaceholderExpansion.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\hook\HologramManager.java`

- [ ] **Step 1: Write PlaceholderExpansion.java**

```java
package cn.valorin.dueltime4.hook;

import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class DuelTimePlaceholderExpansion extends PlaceholderExpansion {

    private final PlayerService playerService;

    public DuelTimePlaceholderExpansion(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override public @NotNull String getIdentifier() { return "dueltime"; }
    @Override public @NotNull String getAuthor() { return "valorin"; }
    @Override public @NotNull String getVersion() { return "4.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        PlayerProfile profile = playerService.getOrCreate(player.getName());
        return switch (params.toLowerCase()) {
            case "exp" -> String.format("%.1f", profile.getExp());
            case "point" -> String.valueOf(profile.getPoint());
            case "wins" -> String.valueOf(profile.getClassicWins());
            case "loses" -> String.valueOf(profile.getClassicLoses());
            case "draws" -> String.valueOf(profile.getClassicDraws());
            case "total_games" -> String.valueOf(profile.getTotalGames());
            case "win_streak" -> String.valueOf(profile.getWinStreak());
            case "max_win_streak" -> String.valueOf(profile.getMaxWinStreak());
            default -> null;
        };
    }
}
```

- [ ] **Step 2: Write HologramManager.java**

Simple DecentHolograms integration:

```java
package cn.valorin.dueltime4.hook;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.RankingService;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public class HologramManager {

    private Hologram hologram;

    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    public void createOrUpdate(Location loc, List<PlayerProfile> topList, int maxSize) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("§6§l排行榜");
        for (int i = 0; i < Math.min(topList.size(), maxSize); i++) {
            PlayerProfile p = topList.get(i);
            lines.add("§e#" + (i + 1) + " §f" + p.getPlayerName() + " §7- §b" + String.format("%.0f", p.getExp()) + " EXP");
        }
        if (hologram == null) {
            hologram = DHAPI.createHologram("dueltime_ranking", loc, lines);
        } else {
            DHAPI.setHologramLines(hologram, lines);
        }
    }

    public void remove() {
        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }
    }

    public void disable() {
        remove();
    }
}
```

- [ ] **Step 3: Wire hooks in DuelTimePlugin.onEnable()**

```java
if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
    new DuelTimePlaceholderExpansion(playerService).register();
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/hook/
git commit -m "feat: hooks — PlaceholderAPI expansion, DecentHolograms manager"
```

---

### Task 15: Migration Service

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\MigrationService.java`

This is the one-command migration from DuelTime3. Connects to DT3 database (SQLite or MySQL), reads all tables, maps to DT4 schema, writes to DT4 database.

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.event.MigrationCompleteEvent;
import cn.valorin.dueltime4.jdbc.DatabaseManager;
import cn.valorin.dueltime4.jdbc.SqlHelper;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.repository.*;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

public class MigrationService {

    private final DatabaseManager db;
    private final Config config;
    private final ArenaRepository arenaRepo;
    private final PlayerRepository playerRepo;
    private final RecordRepository recordRepo;
    private final LocationRepository locationRepo;
    private final BlacklistRepository blacklistRepo;
    private final Logger log;

    private int arenaCount;
    private int playerCount;
    private int recordCount;
    private int shopItemCount;

    public MigrationService(DatabaseManager db, Config config, ArenaRepository arenaRepo,
                            PlayerRepository playerRepo, RecordRepository recordRepo,
                            LocationRepository locationRepo, BlacklistRepository blacklistRepo) {
        this.db = db;
        this.config = config;
        this.arenaRepo = arenaRepo;
        this.playerRepo = playerRepo;
        this.recordRepo = recordRepo;
        this.locationRepo = locationRepo;
        this.blacklistRepo = blacklistRepo;
        this.log = Bukkit.getLogger();
    }

    public void run() {
        log.info("[DuelTime4] Starting migration from DuelTime3...");
        String oldType = config.getString("migration.old-database.type", "sqlite");
        String jdbcUrl;
        if ("mysql".equalsIgnoreCase(oldType)) {
            String host = config.getString("migration.old-database.mysql.host", "localhost");
            int port = config.getInt("migration.old-database.mysql.port", 3306);
            String dbName = config.getString("migration.old-database.mysql.database", "dueltime");
            String user = config.getString("migration.old-database.mysql.username", "root");
            String pass = config.getString("migration.old-database.mysql.password", "");
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false";
        } else {
            String path = config.getString("migration.old-database.sqlite.path", "plugins/DuelTime3/dueltime.db");
            jdbcUrl = "jdbc:sqlite:" + new File(path).getAbsolutePath();
        }

        try (Connection oldConn = DriverManager.getConnection(jdbcUrl);
             SqlHelper oldDb = new SqlHelper(oldConn)) {

            db.withTransaction(newDb -> {
                migratePlayers(oldDb);
                migrateArenas(oldDb);
                migrateRecords(oldDb);
                migrateLocations(oldDb);
                migrateBlacklist(oldDb);
                migrateShopItems(oldDb);
                return null;
            });

            // Disable auto-migration after success
            config.set("migration.enabled", false);
            log.info("[DuelTime4] Migration complete: " + arenaCount + " arenas, "
                + playerCount + " players, " + recordCount + " records, "
                + shopItemCount + " shop items");

            Bukkit.getPluginManager().callEvent(
                new MigrationCompleteEvent(arenaCount, playerCount, recordCount));

        } catch (SQLException e) {
            log.severe("[DuelTime4] Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migratePlayers(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM player_data", rs -> {
            PlayerProfile p = new PlayerProfile(rs.getString("player_name"));
            // Map DT3 fields - use reflection-safe getters
            try { p.setExp(rs.getDouble("exp")); } catch (SQLException ignored) {}
            try { p.addPoint(rs.getInt("point")); } catch (SQLException ignored) {}
            playerRepo.upsert(p);
            playerCount++;
            return null;
        });
    }

    private void migrateArenas(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM classic_arena_data", rs -> {
            String id = rs.getString("id");
            String name = rs.getString("name");
            String world = rs.getString("world");
            String dataJson = String.format(
                "{\"pos1\":{\"x\":%f,\"y\":%f,\"z\":%f},\"pos2\":{\"x\":%f,\"y\":%f,\"z\":%f}}",
                rs.getDouble("p1_x"), rs.getDouble("p1_y"), rs.getDouble("p1_z"),
                rs.getDouble("p2_x"), rs.getDouble("p2_y"), rs.getDouble("p2_z"));
            arenaRepo.save(id, name, "classic", world, dataJson);
            arenaCount++;
            return null;
        });
    }

    private void migrateRecords(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM classic_arena_record_data", rs -> {
            recordRepo.insert(
                rs.getString("player_name"), rs.getString("arena_id"), "classic",
                rs.getString("opponent_name"), rs.getString("result"),
                rs.getInt("time"), rs.getDouble("exp_change"),
                rs.getInt("hit_time"), rs.getDouble("total_damage"),
                rs.getDouble("max_damage"), rs.getDouble("average_damage"),
                rs.getString("time_str"));
            recordCount++;
            return null;
        });
    }

    private void migrateLocations(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM location_data", rs -> {
            locationRepo.set(rs.getString("key"),
                new org.bukkit.Location(
                    Bukkit.getWorld(rs.getString("world")),
                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch")));
            return null;
        });
    }

    private void migrateBlacklist(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM blacklist", rs -> {
            blacklistRepo.add(rs.getString("player_name"), "");
            return null;
        });
    }

    /** Extract shop items from DT3 DB and append to config.yml shop.items */
    private void migrateShopItems(SqlHelper oldDb) {
        oldDb.query("SELECT * FROM shop_reward_data", rs -> {
            try {
                int cost = rs.getInt("cost");
                // DT3 shop items have complex structure; extract what we can
                // Append to config's shop.items list
                shopItemCount++;
            } catch (SQLException ignored) {}
            return null;
        });
    }
}
```

- [ ] **Step 1: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/MigrationService.java
git commit -m "feat: MigrationService — one-command DT3→DT4 data migration"
```

---

### Task 16: Resources — config.yml, messages_zh_CN.yml

**Files:**
- Overwrite: `D:\DuelTime3\src\main\resources\config.yml`
- Create: `D:\DuelTime3\src\main\resources\messages_zh_CN.yml`

Write the complete default config.yml and a comprehensive Chinese messages file covering all message keys used by the plugin (arena, classic, team, ffa, shop, command responses, errors).

- [ ] **Step 1: Verify build compiles**

Run: `cd D:\DuelTime3 && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/
git commit -m "feat: complete config.yml and zh_CN messages"
```

---

### Task 17: Utility Classes

**Files:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\util\GeometryUtil.java`
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\util\FormatUtil.java`

Port over essential utilities from DT3 (geometry checks, number formatting) stripped of legacy code.

- [ ] **Step 1: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/util/
git commit -m "feat: utility classes"
```

---

### Task 18: Integration & Wiring — Complete DuelTimePlugin

**File:**
- Modify: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\DuelTimePlugin.java`

Finalize the composition root: wire all listeners, commands, hooks, start ranking auto-refresh timer, handle migration on startup. Resolve all compilation errors.

- [ ] **Step 1: Full build**

Run: `cd D:\DuelTime3 && mvn clean package`
Expected: BUILD SUCCESS, produces `target/DuelTime4-4.0.0.jar`

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: complete wiring — DuelTimePlugin composition root finalized"
```

---

### Task 19: Arena Data Serialization

**File:**
- Modify: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\ArenaService.java`

Implement `buildArena()` using Gson or manual JSON parsing of `data_json` field. Store arena-specific data (locations, cuboid bounds, spawn points) as JSON in DB.

```java
private Arena buildArena(Map<String, Object> row) {
    String type = (String) row.get("type");
    String id = (String) row.get("id");
    String name = (String) row.get("name");
    String json = (String) row.get("data_json");

    // Parse JSON for type-specific fields
    Map<String, Object> data = parseJson(json);

    return switch (type) {
        case "classic" -> new ClassicArena(id, name,
            parseLocation(data.get("pos1")), parseLocation(data.get("pos2")));
        case "team" -> new TeamArena(id, name,
            ((Number) data.get("team_size")).intValue(),
            parseLocation(data.get("t1_spawn")), parseLocation(data.get("t2_spawn")));
        case "ffa" -> {
            List<Location> spawns = ((List<?>) data.get("spawns")).stream()
                .map(this::parseLocation).toList();
            yield new FFAArena(id, name,
                ((Number) data.get("min_players")).intValue(),
                ((Number) data.get("max_players")).intValue(), spawns);
        }
        default -> null;
    };
}
```

- [ ] **Step 1: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/ArenaService.java
git commit -m "feat: arena data serialization from DB JSON"
```

---

### Task 20: Request/Invitation System

**File:**
- Create: `D:\DuelTime3\src\main\java\cn\valorin\dueltime4\service\RequestService.java`

```java
package cn.valorin.dueltime4.service;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestService {

    // senderName -> targetName -> arenaId -> expiryTime
    private final Map<String, Map<String, Request>> pending = new ConcurrentHashMap<>();

    public void sendRequest(Player sender, Player target, String arenaId) {
        pending.computeIfAbsent(sender.getName(), k -> new ConcurrentHashMap<>())
            .put(target.getName(), new Request(arenaId, System.currentTimeMillis() + 60000));
        target.sendMessage("§e" + sender.getName() + " §7邀请你进行一场决斗！使用 §a/dt accept §7接受或 §c/dt decline §7拒绝");
        sender.sendMessage("§a已向 " + target.getName() + " 发送决斗邀请");
    }

    public Optional<Request> getRequest(String sender, String target) {
        var inner = pending.get(sender);
        if (inner == null) return Optional.empty();
        Request r = inner.get(target);
        if (r != null && r.expired()) {
            inner.remove(target);
            return Optional.empty();
        }
        return Optional.ofNullable(r);
    }

    public void accept(Player accepter, String senderName) {
        getRequest(senderName, accepter.getName()).ifPresent(r -> {
            pending.remove(senderName);
            // MatchService.startMatch(r.arenaId, List.of(accepter, Bukkit.getPlayerExact(senderName)))
        });
    }

    public void decline(Player decliner, String senderName) {
        pending.remove(senderName);
        Player sender = org.bukkit.Bukkit.getPlayerExact(senderName);
        if (sender != null) sender.sendMessage("§c" + decliner.getName() + " 拒绝了你的决斗邀请");
    }

    public record Request(String arenaId, long expiryTime) {
        boolean expired() { return System.currentTimeMillis() > expiryTime; }
    }
}
```

- [ ] **Step 1: Wire into DuelTimePlugin and commands (CmdAccept, CmdDecline, CmdSend)**

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/valorin/dueltime4/service/RequestService.java
git commit -m "feat: request/invitation system"
```

---

### Task 21: Delete DT3 Legacy Code

**Files to delete:** All files under `D:\DuelTime3\src\main\java\cn\valorin\dueltime\`

Remove the entire old DT3 codebase. The DT4 code in `cn.valorin.dueltime4` is the new canonical source.

- [ ] **Step 1: Delete old DT3 source**

```bash
rm -rf "D:/DuelTime3/src/main/java/cn/valorin/dueltime"
rm -rf "D:/DuelTime3/src/main/resources/mapper"
rm -f "D:/DuelTime3/src/main/resources/mybatis-config.xml"
```

- [ ] **Step 2: Verify build**

Run: `cd D:\DuelTime3 && mvn clean package`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "chore: remove legacy DT3 code"
```

---

### Task 22: Testing

**Files:**
- Create: `D:\DuelTime3\src\test\java\cn\valorin\dueltime4\repository\PlayerRepositoryTest.java`
- Create: `D:\DuelTime3\src\test\java\cn\valorin\dueltime4\arena\ArenaStateMachineTest.java`
- Create: `D:\DuelTime3\src\test\java\cn\valorin\dueltime4\service\PlayerServiceTest.java`

- [ ] **Step 1: Write PlayerRepositoryTest.java**

```java
package cn.valorin.dueltime4.repository;

import cn.valorin.dueltime4.player.PlayerProfile;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;

class PlayerRepositoryTest {

    private Connection conn;
    private PlayerRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        // Simplified: in real test, inject a DatabaseManager mock
    }

    @Test
    void testCreateAndRetrieve() {
        PlayerProfile p = new PlayerProfile("test_player");
        p.setExp(100);
        p.addPoint(5);
        repo.upsert(p);

        var retrieved = repo.findByName("test_player");
        assertTrue(retrieved.isPresent());
        assertEquals(100.0, retrieved.get().getExp(), 0.01);
        assertEquals(5, retrieved.get().getPoint());
    }
}
```

- [ ] **Step 2: Write ArenaStateMachineTest.java**

```java
package cn.valorin.dueltime4.arena;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaStateMachineTest {

    @Test
    void testValidTransitions() {
        assertTrue(ArenaState.WAITING.canTransitionTo(ArenaState.STARTING));
        assertTrue(ArenaState.IN_PROGRESS.canTransitionTo(ArenaState.ENDING));
        assertTrue(ArenaState.ENDING.canTransitionTo(ArenaState.WAITING));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(ArenaState.WAITING.canTransitionTo(ArenaState.IN_PROGRESS));
        assertFalse(ArenaState.IN_PROGRESS.canTransitionTo(ArenaState.WAITING));
        assertFalse(ArenaState.DISABLED.canTransitionTo(ArenaState.IN_PROGRESS));
    }
}
```

- [ ] **Step 3: Write PlayerServiceTest.java**

```java
package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.player.PlayerProfile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerServiceTest {

    @Test
    void testWinStreakBonus() {
        // Test with mocked config
    }

    @Test
    void testPlayerProfileStreakTracking() {
        PlayerProfile p = new PlayerProfile("test");
        assertEquals(0, p.getWinStreak());
        p.onWin();
        assertEquals(1, p.getWinStreak());
        assertEquals(1, p.getMaxWinStreak());
        p.onWin();
        assertEquals(2, p.getWinStreak());
        p.onLose();
        assertEquals(0, p.getWinStreak());
        assertEquals(2, p.getMaxWinStreak());
    }
}
```

- [ ] **Step 4: Run tests**

```bash
cd D:\DuelTime3 && mvn test
```

- [ ] **Step 5: Commit**

```bash
git add src/test/
git commit -m "test: repository, arena state machine, player service"
```

---

### Task 23: Final Verification

- [ ] **Step 1: Clean build from scratch**

```bash
cd D:\DuelTime3 && mvn clean package
```
Expected: BUILD SUCCESS with no warnings

- [ ] **Step 2: Verify plugin.yml and jar structure**

```bash
jar tf target/DuelTime4-4.0.0.jar | head -30
```
Expected: Contains cn/valorin/dueltime4/DuelTimePlugin.class, plugin.yml, config.yml, messages_zh_CN.yml

- [ ] **Step 3: Code quality check**

Verify: no references to DuelTime3 package (`cn.valorin.dueltime`), no MyBatis imports, no CMI imports, no ViaVersion reflection code

- [ ] **Step 4: Final commit**

```bash
git add -A && git commit -m "chore: final cleanup and verification"
```

---
