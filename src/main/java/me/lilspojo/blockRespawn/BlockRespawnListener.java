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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.configuration.file.FileConfiguration;


public class BlockRespawnListener implements Listener {

    private final BlockRespawn plugin;

    public BlockRespawnListener(BlockRespawn plugin){

        this.plugin = plugin;

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
            if (!event.getPlayer().hasPermission("saltyblockrespawn.creativerespawn")) {
                return;
            }
        }

        Block block = event.getBlock();
        Material type = block.getType();

        for (String regionName : plugin.getConfig().getStringList("regions")) {
            if (!isBlockInRegion(block, regionName)) continue;

            FileConfiguration blocks = plugin.getLoader().getRegionConfig(regionName);
            if (blocks == null || !blocks.contains("blocks." + type.name())) continue;

            String replace = blocks.getString("blocks." + type.name() + ".replace", "AIR");
            int delay = blocks.getInt("blocks." + type.name() + ".delay");

            Material replaceMaterial;
            try {
                replaceMaterial = Material.valueOf(replace);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid replace material in region " + regionName + ": " + replace);
                return;
            }

            boolean checkReplacement = blocks.getBoolean("blocks." + type.name() + ".check-if-replacement", false);

            if (plugin.getConfig().getBoolean("prevent-overwrite", true)){
                plugin.getRespawnManager().onBlockBrokenAsPrimary(block, type, replaceMaterial, delay, checkReplacement);
            }
            else{
                plugin.getRespawnManager().onBlockBrokenNoPrimary(block, type, replaceMaterial, delay, checkReplacement);
            }
            break;
        }

    }

}