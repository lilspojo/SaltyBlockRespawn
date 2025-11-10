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
    public void reload() {
        plugin.reloadConfig();
        reloadLangConfig();
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

            plugin.saveResource("regions/example_region.yml", false);

            plugin.getLogger().info("Created 'regions' folder!");
        }
    }

    private void loadRegionConfigs() {
        if (regionsFolder == null) regionsFolder = new File(plugin.getDataFolder(), "regions");

        regionConfigs.clear();

        for (String regionName : plugin.getConfig().getStringList("regions")) {
            File file = new File(regionsFolder, regionName + ".yml");

            if (!file.exists()) {
                plugin.getLogger().warning("Region config for '" + regionName + "' not found, creating one...");
                try {
                    file.getParentFile().mkdirs();

                    if (plugin.getResource("regions/example_region.yml") != null) {

                        copyResourceAs(plugin, file);
                    } else {
                        plugin.getLogger().severe("Could not find 'regions/example_region.yml' template in JAR. Cannot create file for '" + regionName + "'.");
                        continue;
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to create region config for '" + regionName + "': " + e.getMessage());
                    continue;
                }
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            regionConfigs.put(regionName, cfg);
        }
    }

    private void reloadLangConfig() {
        if (langFile == null) langFile = new File(plugin.getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public FileConfiguration getRegionConfig(String regionName) {
        return regionConfigs.get(regionName);
    }

    private void copyResourceAs(BlockRespawn plugin, File destinationFile) throws java.io.IOException {
        java.io.InputStream inputStream = plugin.getResource("regions/example_region.yml");
        if (inputStream == null) {
            throw new java.io.IOException("Resource '" + "regions/example_region.yml" + "' not found in JAR. Please contact the developer.");
        }

        destinationFile.getParentFile().mkdirs();

        try (java.io.OutputStream outputStream = new java.io.FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            // Ensure the input stream is closed
            try { inputStream.close(); } catch (java.io.IOException ignored) { }
        }
    }

    public Map<String, FileConfiguration> getAllRegionConfigs() {
        return regionConfigs;
    }

}
