package me.lilspojo.blockRespawn.nexo;

import me.lilspojo.blockRespawn.BlockRespawn;

import static org.bukkit.Bukkit.getServer;

public class NexoInstalledChecker {

    public boolean isNexoInstalled(BlockRespawn plugin){
        if (getServer().getPluginManager().getPlugin("Nexo") != null){
            return true;
        } else {
            return false;
        }
    }
}
