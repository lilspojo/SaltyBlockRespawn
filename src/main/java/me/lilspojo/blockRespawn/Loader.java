package me.lilspojo.blockRespawn;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



public class Loader {

    private final BlockRespawn plugin;

    private File langFile;
    private FileConfiguration langConfig;

    private File regionsFolder;
    private final Map<String, FileConfiguration> regionConfigs = new HashMap<>();

    public Loader(BlockRespawn plugin) {
        this.plugin = plugin;
    }


    public void load() {
        plugin.saveDefaultConfig();
        createLangConfig();
        createRegionsConfig();
        loadRegionConfigs();
        plugin.getLogger().info("Loaded SaltyBlockRespawn configuration.");
    }

    private void createLangConfig() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void createRegionsConfig() {
        regionsFolder = new File(plugin.getDataFolder(), "regions");
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
            plugin.getLogger().info("Created 'regions' folder!");
        }

        File defaultRegionFile = new File(regionsFolder, "regions/example_region.yml");
        if (!defaultRegionFile.exists()) {
            plugin.saveResource("regions/example_region.yml", false);
        }
    }

    private void loadRegionConfigs() {
        if (regionsFolder == null) regionsFolder = new File(plugin.getDataFolder(), "regions");

        regionConfigs.clear();

        for (String regionName : plugin.getConfig().getStringList("regions")) {
            File file = new File(regionsFolder, regionName + ".yml");
            if (!file.exists()) {
                plugin.saveResource("regions/" + regionName + ".yml", false);
            }

            if (!file.exists()) {
                plugin.getLogger().warning("Region config for '" + regionName + "' not found.");
                continue;
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            regionConfigs.put(regionName, cfg);
        }
    }

    private void reloadLangConfig() {
        if (langFile == null) langFile = new File(plugin.getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void reloadRegionConfigs() {
        regionConfigs.clear();
        loadRegionConfigs();
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public FileConfiguration getRegionConfig(String regionName) {
        return regionConfigs.get(regionName);
    }

    public Map<String, FileConfiguration> getAllRegionConfigs() {
        return regionConfigs;
    }

}
