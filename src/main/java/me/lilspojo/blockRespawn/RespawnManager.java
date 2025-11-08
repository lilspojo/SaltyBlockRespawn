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

    private final Map<LocationKey, PendingRespawn> pending = new ConcurrentHashMap<>();

    public RespawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.crashProtection = new CrashProtection(plugin);
    }

    public void onBlockBrokenNoPrimary(Block block, Material originalMaterial, BlockData originalData, Material replaceMaterial, long delay, boolean checkReplacement){
        Bukkit.getScheduler().runTask(plugin, () -> {

            block.setType(replaceMaterial);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!checkReplacement || block.getType()== replaceMaterial) {
                    block.setType(originalMaterial);
                    block.setBlockData(originalData, false);
                    crashProtection.RemoveFromCrashProt(block, originalMaterial, originalData);
                }
            }, delay);

        });
    }

    public void onBlockBrokenAsPrimary(Block block, Material originalMaterial, BlockData originalData, Material replaceMaterial, long delay, boolean checkReplacement) {

        LocationKey key = new LocationKey(block.getLocation());

        synchronized (pending) {
            if (pending.containsKey(key)) {
                Bukkit.getScheduler().runTask(plugin, () -> {

                    block.setType(replaceMaterial);

                    Material primaryType = getPrimaryMaterial(block);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (primaryType != block.getType()){
                            if (block.getLocation().getBlock().getType() == replaceMaterial || !checkReplacement) {
                                block.getLocation().getBlock().setType(originalMaterial);
                                block.getLocation().getBlock().setBlockData(originalData, false);
                            }
                        }
                    }, delay);
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                block.setType(replaceMaterial);
                crashProtection.AddToCrashProt(block, originalMaterial, originalData);
            });

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Remove the entry before doing the respawn so new breaks can be primary.
                synchronized (pending) {
                    pending.remove(key);
                }
                if (block.getLocation().getBlock().getType() == replaceMaterial || !checkReplacement) {
                    block.getLocation().getBlock().setType(originalMaterial);
                    block.getLocation().getBlock().setBlockData(originalData, false);
                    crashProtection.RemoveFromCrashProt(block, originalMaterial, originalData);
                }
            }, delay);

            pending.put(key, new PendingRespawn(task, originalMaterial, replaceMaterial));
        }
    }

    public Material getPrimaryMaterial(Block block) {
        PendingRespawn pr = pending.get(new LocationKey(block.getLocation()));
        return pr != null ? pr.originalMaterial : null;
    }

    public void cancelAll() {
        for (PendingRespawn pendingRespawn : pending.values()) {
            pendingRespawn.task.cancel();
        }
        pending.clear();
    }
}