package me.lilspojo.blockRespawn;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final String databasePath;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/blockdata.db";
    }
    // Open SQLite DB connection & return it
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(databasePath);
        }
        return connection;
    }
    // Initialize SQLite DB
    public void initializeDatabase() {
        try (Connection conn = getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS respawn_blocks (" +
                    "world TEXT NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "material TEXT NOT NULL," +
                    "data TEXT NOT NULL," +
                    "UNIQUE (world, x, y, z)" + // Ensure no duplicate blocks stored
                    ");";

            try (var statement = conn.createStatement()) {
                statement.execute(sql);
            }
            plugin.getLogger().info("Database initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize database: " + e.getMessage());
        }
    }
    // Close the SQLite DB connection
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
}