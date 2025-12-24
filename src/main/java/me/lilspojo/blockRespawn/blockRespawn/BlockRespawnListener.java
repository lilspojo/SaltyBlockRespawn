package me.lilspojo.blockRespawn.blockRespawn;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import me.lilspojo.blockRespawn.BlockRespawn;
import me.lilspojo.blockRespawn.loader.BlockRule;
import me.lilspojo.blockRespawn.loader.Loader;
import me.lilspojo.blockRespawn.loader.RegionSettings;
import me.lilspojo.blockRespawn.nexo.NexoInstalledChecker;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import me.lilspojo.saltyForge.SaltyForge;

import java.util.List;
import java.util.Map;

public class BlockRespawnListener implements Listener {

    private final BlockRespawn plugin;
    private final RespawnManager respawnManager;

    private final Loader loader;
    private final String nexoPrefix = "nexo:";

    public final boolean isNexoInstalled;
    private final List<String> configuredRegions;
    private final RegionContainer container;

    private final Map<String, RegionSettings> cachedRegionSettings;

    public BlockRespawnListener(BlockRespawn plugin, RespawnManager respawnManager) {
        this.plugin = plugin;
        this.respawnManager = respawnManager;
        this.loader = plugin.getLoader();
        this.configuredRegions = plugin.getConfig().getStringList("regions");
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        NexoInstalledChecker nexo = new NexoInstalledChecker();
        this.isNexoInstalled = nexo.isNexoInstalled(plugin);

        // Fetch settings from loader
        this.cachedRegionSettings = loader.getCachedRegionSettings();
    }

    // Check if block is in region; if so return true
    public boolean isBlockInRegion(Block block, String regionName) {
        RegionManager manager = container.get(BukkitAdapter.adapt(block.getWorld()));
        if (manager == null) return false;

        ProtectedRegion region = manager.getRegion(regionName);
        if (region == null) return false;

        BlockVector3 location = BlockVector3.at(block.getX(), block.getY(), block.getZ());
        return region.contains(location);
    }

    // Block break listener & handling
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // If creative-bypass is true & gamemode creative, quit task
        if (plugin.getConfig().getBoolean("creative-bypass", true) && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();
        BlockData blockData = block.getBlockData();

        // Loop through configured regions
        for (String regionName : configuredRegions) {

            if (!isBlockInRegion(block, regionName)) continue;

            // Get region settings
            RegionSettings settings = cachedRegionSettings.get(regionName);

            if (settings == null) {
                plugin.getLogger().warning("Could not find cached settings for region: " + regionName);
                continue;
            }

            // Check for block match
            BlockRule matchedRule = null;

            for (BlockRule rule : settings.rules) {
                if (isBlockMatchingRule(block, rule)) {
                    matchedRule = rule;
                    break;
                }
            }

            // Block respawning
            if (matchedRule != null) {
                executeRespawnAction(block, blockType, blockData, matchedRule);

                if (matchedRule.dropCustom) {
                    String dropString = matchedRule.dropType;
                    ItemStack item = null;
                    if (dropString.contains("saltyforge") && Bukkit.getPluginManager().getPlugin("SaltyForge") != null && Bukkit.getPluginManager().getPlugin("SaltyForge").isEnabled()) {
                        SaltyForge forge = (SaltyForge) Bukkit.getPluginManager().getPlugin("SaltyForge");

                        String itemId = dropString.split(":")[1];
                        item = forge.getItem().getItemStack(itemId);

                        event.setDropItems(false);
                        if (item != null) {
                            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                            block.getWorld().dropItemNaturally(loc, item);
                        } else {
                            plugin.getLogger().warning("Configured drop '" + dropString + "' does not exist.");
                        }
                    } else {
                        if (dropString.contains("saltyforge")){
                            plugin.getLogger().warning("SaltyForge must be present to use SaltyForge item types.");
                            return;
                        }

                        Material itemMaterial = Material.matchMaterial(dropString);
                        if (itemMaterial != null) {
                            item = new ItemStack(itemMaterial);
                        } else {
                            plugin.getLogger().warning("Configured drop '" + dropString + "' does not exist.");
                        }

                        event.setDropItems(false);
                        if (item != null) {
                            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                            block.getWorld().dropItemNaturally(loc, item);
                        } else {
                            plugin.getLogger().warning("Configured drop '" + dropString + "' does not exist.");
                        }
                    }
                }
                return;
            }

            // Block protection
            if (settings.preventMiningNonRespawnable) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isBlockMatchingRule(Block block, BlockRule rule) {
        // Nexo block match
        if (rule.isNexo) {
            if (!isNexoInstalled) return false;
            if (NexoBlocks.isCustomBlock(block)) {
                CustomBlockMechanic mech = NexoBlocks.customBlockMechanic(block.getLocation());
                return mech != null && rule.nexoId.equals(mech.getItemID());
            }
            return false;
        }

        // Vanilla block match
        if (rule.material == block.getType()) {
            if (rule.blockData != null) {
                return block.getBlockData().matches(rule.blockData);
            }
            return true;
        }

        return false;
    }

    private void executeRespawnAction(Block block, Material blockType, BlockData blockData, BlockRule rule) {
        // Replace material
        Material replaceMaterial = rule.replaceMaterial;

        // Replace string (full)
        String replaceString;

        // Replace string (block data only)
        String replaceDataString;

        if (rule.replaceIsNexo) {
            replaceString = nexoPrefix + rule.replaceNexoId;
            replaceDataString = "";
            // Set to null for Nexo as not utilized
            replaceMaterial = null;
        } else {
            // If replaceBlockData is set, reconstruct full string
            if (rule.replaceBlockData != null) {
                // Reconstruct replace string
                replaceString = rule.replaceBlockData.getAsString();

                // Extract replace block data
                String fullData = rule.replaceBlockData.getAsString(false);
                int bracketIndex = fullData.indexOf('[');
                int endIndex = fullData.indexOf(']');

                if (bracketIndex != -1 && endIndex != -1) {
                    replaceDataString = fullData.substring(bracketIndex + 1, endIndex);
                } else {
                    replaceDataString = "";
                }
            } else {
                replaceString = rule.replaceMaterial.name();
                replaceDataString = "";
            }
        }

        boolean preventOverwrite = plugin.getConfig().getBoolean("prevent-overwrite", true);

        if (preventOverwrite) {
            respawnManager.onBlockBrokenAsPrimary(
                    block,
                    blockType,
                    blockData,
                    replaceMaterial,
                    rule.delay,
                    rule.checkReplacement,
                    replaceString,
                    replaceDataString
            );
        } else {
            respawnManager.onBlockBrokenNoPrimary(
                    block,
                    blockType,
                    blockData,
                    replaceMaterial,
                    rule.delay,
                    rule.checkReplacement,
                    replaceString,
                    replaceDataString
            );
        }
    }
}