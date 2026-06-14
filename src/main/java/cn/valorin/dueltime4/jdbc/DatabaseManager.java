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
        this.sqlite = "sqlite".equalsIgnoreCase(config.getDbType());

        HikariConfig hikariConfig = new HikariConfig();
        if (sqlite) {
            File dbFile = new File(config.getPlugin().getDataFolder(), "dueltime.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        } else {
            hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getDbMysqlHost() + ":" + config.getDbMysqlPort()
                + "/" + config.getDbMysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true");
            hikariConfig.setUsername(config.getDbMysqlUsername());
            hikariConfig.setPassword(config.getDbMysqlPassword());
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

    /** Returns SQLite-compatible upsert: "ON CONFLICT(col) DO UPDATE SET ..." */
    public String upsertSql(String conflictCol, String... columns) {
        if (sqlite) {
            StringBuilder sb = new StringBuilder("ON CONFLICT(").append(conflictCol).append(") DO UPDATE SET ");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(columns[i]).append(" = excluded.").append(columns[i]);
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder("ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(columns[i]).append(" = VALUES(").append(columns[i]).append(")");
            }
            return sb.toString();
        }
    }

    /** DDL column type for a text primary key */
    public String pkType() { return sqlite ? "TEXT PRIMARY KEY" : "VARCHAR(36) PRIMARY KEY"; }

    /** DDL clause for auto-incrementing integer primary key */
    public String autoInc() { return sqlite ? "AUTOINCREMENT" : "AUTO_INCREMENT"; }

    public void close() { dataSource.close(); }
}
