package dev.oumaimaa.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.oumaimaa.KawaiiAdPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Manages SQLite connections and database operations using HikariCP for pooling.
 */
public final class DatabaseManager {

    private static final String COOLDOWNS_TABLE = "ad_cooldowns";
    private static final String REVIEW_TABLE = "ad_review_queue";
    private final KawaiiAdPlugin plugin;
    private HikariDataSource dataSource;

    /**
     * Constructs the DatabaseManager and initializes the Hikari Connection Pool.
     *
     * @param plugin The main plugin instance.
     */
    public DatabaseManager(final KawaiiAdPlugin plugin) {
        this.plugin = plugin;
        setupDatabaseFile();
        initializePool();
        initializeDatabaseTables();
    }

    private void setupDatabaseFile() {
        final File dataFolder = new File(plugin.getDataFolder(), "cooldowns.db");
        if (!dataFolder.exists()) {
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create database file: " + e.getMessage());
            }
        }
    }

    private void initializePool() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "cooldowns.db").getAbsolutePath());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("KawaiiAD-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    private void initializeDatabaseTables() {
        final String createCooldownsSQL = "CREATE TABLE IF NOT EXISTS " + COOLDOWNS_TABLE + " ("
                + "uuid TEXT PRIMARY KEY,"
                + "last_ad_time INTEGER NOT NULL"
                + ");";

        final String createReviewSQL = "CREATE TABLE IF NOT EXISTS " + REVIEW_TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "submitter_uuid TEXT NOT NULL,"
                + "message TEXT NOT NULL,"
                + "submission_time INTEGER NOT NULL"
                + ");";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement cooldownStmt = conn.prepareStatement(createCooldownsSQL);
             PreparedStatement reviewStmt = conn.prepareStatement(createReviewSQL)) {
            cooldownStmt.execute();
            reviewStmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database table initialization error: " + e.getMessage());
        }
    }

    /**
     * Closes the Hikari Connection Pool safely on shutdown.
     */
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP pool closed successfully.");
        }
    }

    public OptionalLong loadCooldown(final @NotNull UUID uuid) {
        final String sql = "SELECT last_ad_time FROM " + COOLDOWNS_TABLE + " WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return OptionalLong.of(rs.getLong("last_ad_time"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load cooldown for " + uuid + ": " + e.getMessage());
        }
        return OptionalLong.empty();
    }

    public void saveCooldown(final UUID uuid, final long timestamp) {
        final String sql = "INSERT OR REPLACE INTO " + COOLDOWNS_TABLE + " (uuid, last_ad_time) VALUES (?, ?)";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, uuid.toString());
                pstmt.setLong(2, timestamp);
                pstmt.executeUpdate();
                if (plugin.getAdsConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("DEBUG: Cooldown saved for " + uuid + " at " + timestamp);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save cooldown for " + uuid + ": " + e.getMessage());
            }
        });
    }

    /**
     * Inserts a new ad into the review queue.
     */
    public void queueAdForReview(final UUID submitter, final String message) {
        final String sql = "INSERT INTO " + REVIEW_TABLE + " (submitter_uuid, message, submission_time) VALUES (?, ?, ?)";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, submitter.toString());
                pstmt.setString(2, message);
                pstmt.setLong(3, System.currentTimeMillis());
                pstmt.executeUpdate();

                if (plugin.getAdsConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("DEBUG: Ad queued for review by " + submitter);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to queue ad for review: " + e.getMessage());
            }
        });
    }
}