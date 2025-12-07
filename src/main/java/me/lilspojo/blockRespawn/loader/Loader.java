package me.lilspojo.blockRespawn.loader;

import me.lilspojo.blockRespawn.BlockRespawn;
import me.lilspojo.blockRespawn.nexo.NexoInstalledChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Loader {

    private final BlockRespawn plugin;

    private File langFile;
    private FileConfiguration langConfig;

    private File regionsFolder;
    private final Map<String, FileConfiguration> regionConfigs = new HashMap<>();

    private final NexoInstalledChecker nexo = new NexoInstalledChecker();

    public Loader(BlockRespawn plugin) {
        this.plugin = plugin;
    }


    // Load all configs
    public void load() {
        plugin.saveDefaultConfig();
        createLangConfig();
        createRegionsConfig();
        loadRegionConfigs();

        if (nexo.isNexoInstalled(plugin)) {
            plugin.getLogger().info("Nexo detected, Enabling Nexo block support.");
        }
        plugin.getLogger().info("Loaded SaltyBlockRespawn configuration.");
    }


    // Reload all configs
    public void reload() {
        plugin.reloadConfig();
        reloadLangConfig();
        createRegionsConfig();
        loadRegionConfigs();
        plugin.getLogger().info("Loaded SaltyBlockRespawn configuration.");
    }


    // Create & load lang.yml
    private void createLangConfig() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }


    // Create & load example_region.yml
    private void createRegionsConfig() {
        regionsFolder = new File(plugin.getDataFolder(), "regions");
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();

            plugin.saveResource("regions/example_region.yml", false);

            plugin.getLogger().info("Created 'regions' folder!");
        }
    }


    // Load all region configs
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
                        plugin.getLogger().severe("Could not find 'regions/example_region.yml' template in JAR. Cannot create file for '" + regionName + "'. Please contact the developer.");
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
        compileRegionSettings();
    }


    private Map<String, RegionSettings> cachedRegionSettings = new HashMap<>();

    public void compileRegionSettings() {

        cachedRegionSettings.clear();

        for (String region : regionConfigs.keySet()) {

            FileConfiguration cfg = regionConfigs.get(region);
            RegionSettings settings = new RegionSettings();

            settings.preventMiningNonRespawnable = cfg.getBoolean("prevent-mining-non-respawnable", true);
            settings.preventBlockPhysics = cfg.getBoolean("prevent-block-physics", false);

            ConfigurationSection blocks = cfg.getConfigurationSection("blocks");

            if (blocks != null) {

                for (String key : blocks.getKeys(false)) {

                    ConfigurationSection group = blocks.getConfigurationSection(key);
                    if (group == null) continue;

                    List<BlockRule> rules = parseBlockRule(group);
                    settings.rules.addAll(rules);
                }
            }
            cachedRegionSettings.put(region, settings);
        }
    }


    private List<BlockRule> parseBlockRule(ConfigurationSection configurationSection) {

        List<BlockRule> rules = new ArrayList<>();

        Object typeObj = configurationSection.get("type");

        List<String> types;

        if (typeObj instanceof List<?> list) {
            types = list.stream().map(Object::toString).toList();
        } else {
            types = Collections.singletonList(typeObj.toString());
        }
        // Type data
        for (String typeString : types) {
            typeString = typeString.trim();

            BlockRule rule = new BlockRule();

            // If nexo type
            if (typeString.startsWith("nexo:")) {
                rule.isNexo = true;
                rule.nexoId = typeString.substring("nexo:".length());
            }
            // If block data defined
            else if (typeString.contains("[")) {
                try {
                    String materialString = typeString.substring(0, typeString.indexOf('['));
                    String properties = typeString.substring(typeString.indexOf('['));
                    Material material = Material.valueOf(materialString.toUpperCase());
                    rule.material = material;
                    rule.blockData = Bukkit.createBlockData(material, properties);
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid blockdata: " + typeString);
                }
            }
            // If normal type
            else {
                rule.material = (Material.valueOf(typeString.toUpperCase()));
            }
            String replaceString = configurationSection.getString("replace");

            // If replace nexo
            if (replaceString.startsWith("nexo:")) {
                rule.replaceIsNexo = true;
                rule.replaceNexoId = replaceString.substring("nexo:".length());
            }
            // If replace block data defined
            else if (replaceString.contains("[")) {
                String materialString = replaceString.substring(0, replaceString.indexOf('['));
                String properties = replaceString.substring(replaceString.indexOf('['));
                Material material = Material.valueOf(materialString.toUpperCase());
                rule.replaceMaterial = material;
                rule.replaceBlockData = Bukkit.createBlockData(material, properties);
            }
            // If replace normal type
            else {
                rule.replaceMaterial = Material.valueOf(replaceString.toUpperCase());
            }

            rule.delay = configurationSection.getInt("delay", 20);
            rule.checkReplacement = configurationSection.getBoolean("check-if-replacement", false);

            rules.add(rule);
        }
        return rules;
    }


    // Reload lang.yml
    private void reloadLangConfig() {
        if (langFile == null) langFile = new File(plugin.getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }


    // Fetch lang.yml
    public FileConfiguration getLangConfig() {
        return langConfig;
    }
    // Fetch regions configs
    public FileConfiguration getRegionConfig(String regionName) {
        return regionConfigs.get(regionName);
    }
    // Fetch region settings
    public Map<String, RegionSettings> getCachedRegionSettings() {
        return cachedRegionSettings;
    }


    // Copier for auto region config creation -- copies example_region.yml
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
}
