package me.lilspojo.blockRespawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public final class BlockRespawn extends JavaPlugin {

    private Loader loader;
    private RespawnManager respawnManager;

    public Loader getLoader() {
        return loader;
    }
    public RespawnManager getRespawnManager() {
        return respawnManager;
    }

    @Override
    public void onEnable() {

        getLogger().info("Enabling SaltyBlockRespawn...");

        loader = new Loader(this);
        loader.load();

        respawnManager = new RespawnManager(this);

        getServer().getPluginManager().registerEvents(new BlockRespawnListener(this), this);
        CrashProtection crashProtection = new CrashProtection(this);
        crashProtection.RunCrashProt();
        getLogger().info("Enabled SaltyBlockRespawn!");

    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(command.getName().equalsIgnoreCase("saltyblockrespawn")){

            if(args.length == 0){
                sender.sendMessage(loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload."));
                return true;
            }
            if(args[0].equalsIgnoreCase("reload")){
                if(!sender.hasPermission("saltyblockrespawn.reload")){
                    if(loader.getLangConfig().getString("no-permission-reload") != null){
                        sender.sendMessage(loader.getLangConfig().getString("no-permission-reload", "You do not have permission to perform this command!"));
                    }
                    return true;
                }
            }

            loader = new Loader(this);
            loader.reload();


            sender.sendMessage(loader.getLangConfig().getString("reload", "SaltyBlockRespawn configuration files reloaded!"));
            return true;

        }
        sender.sendMessage(loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload."));
        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Disabling SaltyBlockRespawn...");
        // Clean up pending respawn tasks.
        if (respawnManager != null) respawnManager.cancelAll();

    }
}
