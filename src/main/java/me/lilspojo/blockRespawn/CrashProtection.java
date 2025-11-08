package me.lilspojo.blockRespawn;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class CrashProtection {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public CrashProtection(JavaPlugin plugin) {
        this.plugin = plugin;

        // File in plugin's folder
        dataFile = new File(plugin.getDataFolder(), "crash-protection.data");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs(); // create folder if missing
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void AddToCrashProt(Block block, Material originalMaterial, BlockData originalData){

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        String key = getLocationKey(block);
        dataConfig.set(key + ".material", originalMaterial.toString());
        dataConfig.set(key + ".data", originalData.getAsString());
        save();

    }

    public void RemoveFromCrashProt(Block block, Material originalMaterial, BlockData originalData){

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        String key = getLocationKey(block);
        dataConfig.set(key, null); // remove entry
        save();

    }

    private String getLocationKey(Block block) {
        return block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private void save() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void RunCrashProt() {
        for (String key : dataConfig.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length != 4) continue;

            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (plugin.getServer().getWorld(worldName) == null) return;

                Block block = plugin.getServer().getWorld(worldName).getBlockAt(x, y, z);

                Material material = Material.valueOf(dataConfig.getString(key + ".material"));
                BlockData blockData = Bukkit.createBlockData(dataConfig.getString(key + ".data"));

                block.setType(material);
                block.setBlockData(blockData);

                dataConfig.set(key, null);
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
