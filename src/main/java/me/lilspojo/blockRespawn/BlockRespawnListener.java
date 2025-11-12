package me.lilspojo.blockRespawn;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;

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
    private final RespawnManager respawnManager;
    private final NexoBlockChecker nexoBlockChecker;
    private final String nexoPrefix = "nexo:";

    public BlockRespawnListener(BlockRespawn plugin, RespawnManager respawnManager, NexoBlockChecker nexoBlockChecker){
        this.plugin = plugin;
        this.respawnManager = respawnManager;
        this.nexoBlockChecker = nexoBlockChecker;
    }
    // Check if block is in region; if so return true
    public boolean isBlockInRegion(Block block, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
        if (manager == null) return false;

        ProtectedRegion region = manager.getRegion(regionName);
        if (region == null) return false;

        BlockVector3 location = BlockVector3.at(block.getX(), block.getY(), block.getZ());
        return region.contains(location);
    }
    // Block break listener & handling
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){
        // If creative-bypass is true & gamemode creative, quit task
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            if (plugin.getConfig().getBoolean("creative-bypass")) return;
        }
        // Fetch block info
        Block block = event.getBlock();
        Material type = block.getType();
        BlockData blockData = block.getBlockData();
        // Baseline for block prot
        boolean isRespawnable = false;
        // Get region list
        for (String regionName : plugin.getConfig().getStringList("regions")) {
            // If block isn't in listed regions, quit task
            if (!isBlockInRegion(block, regionName)) continue;
            // Moving the config checks to outside onBlockBreak soon:tm:
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

                    boolean typeMatched = false;
                    for (String configuredType : types) {
                        if (configuredType == null) continue;
                        configuredType = configuredType.trim();

                        // Check if type is nexo
                        if (configuredType.toLowerCase().startsWith(nexoPrefix)) {
                            String expectedNexoId = configuredType.substring(nexoPrefix.length());

                            // Check if the broken block is nexo
                            if (NexoBlocks.isCustomBlock(block)) {
                                // Check is broken nexo is type
                                CustomBlockMechanic mech = NexoBlocks.customBlockMechanic(block.getLocation());
                                if (mech != null && expectedNexoId.equals(mech.getItemID())) {
                                    typeMatched = true;
                                    break;
                                }
                            }
                        } else {
                            // vanilla material type
                            try {
                                if (configuredType.equalsIgnoreCase(type.name())) {
                                    typeMatched = true;
                                    break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (!typeMatched) continue;
                    // Prevent block prot; is respawnable
                    isRespawnable = true;

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
                        if (replace.startsWith(nexoPrefix)) {
                            String nexoBlockId = replace.substring(nexoPrefix.length());
                            nexoBlockChecker.isNexoBlock(nexoBlockId);
                            replaceMaterial = null;
                        } else {
                            replaceMaterial = Material.valueOf(replace);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid replace material in region " + regionName + ": " + replace);
                        return;
                    }
                    // If prevent-overwrite is true in config.yml, set & check primary blocks
                    if (plugin.getConfig().getBoolean("prevent-overwrite", true)){
                        respawnManager.onBlockBrokenAsPrimary(block, type, blockData, replaceMaterial, delay, checkReplacement, replace);
                    }
                    // If prevent-overwrite is false in config.yml, treat all block respawns equal
                    else{
                        respawnManager.onBlockBrokenNoPrimary(block, type, blockData, replaceMaterial, delay, checkReplacement, replace);
                    }
                    break;
                }
            } else {
                plugin.getLogger().warning("Could not find 'blocks' section for region " + regionName);
                return;
            }
            // Block prot
            if (!isRespawnable && plugin.getLoader().getRegionConfig(regionName).getBoolean("prevent-mining-non-respawnable", true)) {
                event.setCancelled(true);
            }

        }
    }
}