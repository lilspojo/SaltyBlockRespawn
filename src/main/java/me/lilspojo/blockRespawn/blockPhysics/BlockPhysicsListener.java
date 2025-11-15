package me.lilspojo.blockRespawn.blockPhysics;

import me.lilspojo.blockRespawn.BlockRespawn;
import me.lilspojo.blockRespawn.blockRespawn.BlockRespawnListener;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

public class BlockPhysicsListener implements Listener {

    private final BlockRespawn plugin;
    private final BlockRespawnListener blockRespawnListener;

    public BlockPhysicsListener(BlockRespawn plugin, BlockRespawnListener blockRespawnListener) {
        this.plugin = plugin;
        this.blockRespawnListener = blockRespawnListener;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPhysics(BlockPhysicsEvent event){
        Block block = event.getBlock();
        for (String regionName : plugin.getConfig().getStringList("regions")){
            boolean blockPhysicsToggle = plugin.getLoader().getRegionConfig(regionName).getBoolean("prevent-block-physics", false);
            if (blockPhysicsToggle){
                if (blockRespawnListener.isBlockInRegion(block, regionName)){
                    event.setCancelled(true);
                }
            }
        }
    }
}
