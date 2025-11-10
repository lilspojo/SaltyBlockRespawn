package me.lilspojo.blockRespawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CrashProtection {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    public CrashProtection(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }



    public void AddToCrashProt(Block block, Material originalMaterial, BlockData originalData) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT OR REPLACE INTO respawn_blocks (world, x, y, z, material, data) VALUES (?, ?, ?, ?, ?, ?)";

            Connection conn = null;
            try {
                conn = databaseManager.getConnection();

                try (PreparedStatement ps = conn.prepareStatement(sql)) {

                    // Set Location Parameters
                    ps.setString(1, block.getWorld().getName());
                    ps.setInt(2, block.getX());
                    ps.setInt(3, block.getY());
                    ps.setInt(4, block.getZ());

                    // Set Block Data Parameters
                    ps.setString(5, originalMaterial.toString());
                    ps.setString(6, originalData.getAsString());

                    ps.executeUpdate();
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add block to crash protection: " + e.getMessage());
            }
        });
    }



    public void RemoveFromCrashProt(Block block) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM respawn_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";

            Connection conn = null; // Declare the connection outside
            try {
                conn = databaseManager.getConnection(); // Get the thread-safe shared connection

                // Use try-with-resources ONLY for the PreparedStatement (ps)
                try (PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, block.getWorld().getName());
                    ps.setInt(2, block.getX());
                    ps.setInt(3, block.getY());
                    ps.setInt(4, block.getZ());

                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove block from crash protection: " + e.getMessage());
            }
        });
    }



    private void clearAllCrashData() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM respawn_blocks";

            Connection conn = null;
            try {
                conn = databaseManager.getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to clear crash protection table: " + e.getMessage());
            }
        });
    }



    public void RunCrashProt() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String selectSql = "SELECT * FROM respawn_blocks";

            Connection conn = null;
            try {
                conn = databaseManager.getConnection();

                try (PreparedStatement selectPs = conn.prepareStatement(selectSql);
                     ResultSet rs = selectPs.executeQuery()) {

                    int count = 0;
                    // Loop through all results in database
                    while (rs.next()) {
                        // Extract data from database
                        String worldName = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        String materialString = rs.getString("material");
                        String dataString = rs.getString("data");
                        count++;

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (plugin.getServer().getWorld(worldName) == null) return;

                            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);

                            try {
                                Material material = Material.valueOf(materialString);
                                BlockData blockData = Bukkit.createBlockData(dataString);
                                loc.getBlock().setType(material);
                                loc.getBlock().setBlockData(blockData);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Could not restore block at " + x + "," + y + "," + z + ": Invalid material or block data string.");
                            }

                        });
                    }

                    if (count > 0) {
                        clearAllCrashData();
                        plugin.getLogger().info("Scheduled " + count + " blocks for restoration from crash protection data.");
                    } else {
                        plugin.getLogger().info("No blocks found in crash data to restore.");
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Error reading crash protection data: " + e.getMessage());
            }
        });
    }
}