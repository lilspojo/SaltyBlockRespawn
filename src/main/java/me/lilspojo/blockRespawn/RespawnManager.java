package me.lilspojo.blockRespawn;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RespawnManager {

    private final JavaPlugin plugin;
    private final CrashProtection crashProtection;

    private final Map<LocationKey, PendingRespawn> pending = new ConcurrentHashMap<>();

    public RespawnManager(JavaPlugin plugin, CrashProtection crashProtection) {
        this.plugin = plugin;
        this.crashProtection = crashProtection;
    }

    private static class PendingRespawn {
        final BukkitTask task;
        final Material originalMaterial;
        final Material temporaryMaterial;

        PendingRespawn(BukkitTask task, Material originalMaterial, Material temporaryMaterial) {
            this.task = task;
            this.originalMaterial = originalMaterial;
            this.temporaryMaterial = temporaryMaterial;
        }
    }
    // When prevent-overwrite config is false
    public void onBlockBrokenNoPrimary(Block block, Material originalMaterial, BlockData originalData, Material replaceMaterial, long delay, boolean checkReplacement){
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Set the immediate replacement block & add to crash prot DB
            block.setType(replaceMaterial);
            crashProtection.AddToCrashProt(block, originalMaterial, originalData);
            // Respawn block + block data + remove from crash prot DB
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!checkReplacement || block.getType()== replaceMaterial) {
                    block.setType(originalMaterial);
                    block.setBlockData(originalData, false);
                    crashProtection.RemoveFromCrashProt(block);
                }
            }, delay /* Ticks */);

        });
    }
    // when prevent-overwrite config is true
    public void onBlockBrokenAsPrimary(Block block, Material originalMaterial, BlockData originalData, Material replaceMaterial, long delay, boolean checkReplacement) {

        LocationKey key = new LocationKey(block.getLocation());

        synchronized (pending) {
            // If primary block already exists -- No crash prot for non-primary
            if (pending.containsKey(key)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Set the immediate replacement block
                    block.setType(replaceMaterial);
                    // Get primary block & save for replace task
                    Material primaryType = getPrimaryMaterial(block);
                    // Respawn block + block data IFF primary block hasn't respawned
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (primaryType != block.getType()){
                            if (block.getLocation().getBlock().getType() == replaceMaterial || !checkReplacement) {
                                block.getLocation().getBlock().setType(originalMaterial);
                                block.getLocation().getBlock().setBlockData(originalData, false);
                            }
                        }
                    }, delay /* Ticks */);
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Set the immediate replacement block & add to crash prot DB
                block.setType(replaceMaterial);
                crashProtection.AddToCrashProt(block, originalMaterial, originalData);
            });

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Remove the entry before respawn so new breaks can be primary
                synchronized (pending) {
                    pending.remove(key);
                }
                // Respawn block + block data + remove from crash prot DB
                if (block.getLocation().getBlock().getType() == replaceMaterial || !checkReplacement) {
                    block.getLocation().getBlock().setType(originalMaterial);
                    block.getLocation().getBlock().setBlockData(originalData, false);
                    crashProtection.RemoveFromCrashProt(block);
                }
            }, delay /* Ticks */);
            pending.put(key, new PendingRespawn(task, originalMaterial, replaceMaterial));
        }
    }
    // Getter for material of primary block
    public Material getPrimaryMaterial(Block block) {
        PendingRespawn pr = pending.get(new LocationKey(block.getLocation()));
        return pr != null ? pr.originalMaterial : null;
    }
    // Cancel all pending respawn tasks
    public void cancelAll() {
        for (PendingRespawn pendingRespawn : pending.values()) {
            pendingRespawn.task.cancel();
        }
        pending.clear();
    }
}