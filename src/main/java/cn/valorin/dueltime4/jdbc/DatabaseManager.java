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
    private final boolean sqlite;

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
                try {
                    db.raw().rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    db.raw().setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection is dead anyway, propagate original exception
                }
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

    public boolean isSqlite() { return sqlite; }

    public void close() { dataSource.close(); }
}
