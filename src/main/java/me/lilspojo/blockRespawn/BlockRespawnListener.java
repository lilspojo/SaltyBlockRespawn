package me.lilspojo.blockRespawn;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class BlockRespawnListener implements Listener {

    private final BlockRespawn plugin;
    private final CrashProtection crashProtection;
    private final RespawnManager respawnManager;
    private Loader loader;

    public BlockRespawnListener(BlockRespawn plugin, CrashProtection crashProtection, RespawnManager respawnManager){

        this.plugin = plugin;
        this.crashProtection = crashProtection;
        this.respawnManager = respawnManager;

    }

    public boolean isBlockInRegion(Block block, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
        if (manager == null) return false;

        ProtectedRegion region = manager.getRegion(regionName);
        if (region == null) return false;

        BlockVector3 location = BlockVector3.at(block.getX(), block.getY(), block.getZ());
        return region.contains(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){

        if(event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            if (plugin.getConfig().getBoolean("creative-bypass")) return;
        }

        Block block = event.getBlock();
        Material type = block.getType();
        BlockData blockData = block.getBlockData();

        boolean matched = false;

        for (String regionName : plugin.getConfig().getStringList("regions")) {
            if (!isBlockInRegion(block, regionName)) continue;

            FileConfiguration blocks = plugin.getLoader().getRegionConfig(regionName);

            ConfigurationSection blocksSection = blocks.getConfigurationSection("blocks");
            if (blocks.getConfigurationSection("blocks") == null) {
                plugin.getLogger().warning("No 'blocks' section in region config: " + regionName);
                continue;
            }
            if (blocksSection != null){
                for (String key : blocksSection.getKeys(false)) {
                    ConfigurationSection blockGroup = blocksSection.getConfigurationSection(key);
                    if (blockGroup == null) continue;
                    Object typeObj = blockGroup.get("type");
                    List<String> types;

                    if (typeObj instanceof List) {
                        types = ((List<?>) typeObj).stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                    } else if (typeObj != null){
                        types = Collections.singletonList(typeObj.toString());
                    } else {
                        plugin.getLogger().warning("No 'type' defined in block group '" + key + "' for region " + regionName);
                        continue;
                    }

                    // Check is block type is listed.
                    if (!types.contains(type.name())) continue;

                    matched = true;

                    // Read block replacement properties.
                    String replace = blockGroup.getString("replace");
                    int delay = blockGroup.getInt("delay", 0);
                    boolean checkReplacement = blockGroup.getBoolean("check-if-replacement", false);

                    if (replace == null || replace.isEmpty()) {
                        plugin.getLogger().warning("Missing 'replace' value in region " + regionName + " for group " + key);
                        continue;
                    }

                    Material replaceMaterial;
                    try {
                        replaceMaterial = Material.valueOf(replace);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid replace material in region " + regionName + ": " + replace);
                        return;
                    }

                    if (plugin.getConfig().getBoolean("prevent-overwrite", true)){
                        respawnManager.onBlockBrokenAsPrimary(block, type, blockData, replaceMaterial, delay, checkReplacement);
                        // Crash protection is handled within onBlockBrokenAsPrimary.
                    }
                    else{
                        respawnManager.onBlockBrokenNoPrimary(block, type, blockData, replaceMaterial, delay, checkReplacement);
                        crashProtection.AddToCrashProt(block, type, blockData);
                    }
                    break;
                }
            }
            if (!matched && plugin.getConfig().getBoolean("prevent-mining-non-respawnable", true)) {
                event.setCancelled(true);
            }

        }
    }
}