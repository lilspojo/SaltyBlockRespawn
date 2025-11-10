package me.lilspojo.blockRespawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public final class BlockRespawn extends JavaPlugin {

    private Loader loader;
    private RespawnManager respawnManager;
    private DatabaseManager databaseManager;
    private CrashProtection crashProtection;

    public Loader getLoader() {
        return loader;
    }
    public RespawnManager getRespawnManager() {
        return respawnManager;
    }
    public CrashProtection getCrashProtection() {
        return crashProtection;
    }

    @Override
    public void onEnable() {

        getLogger().info("Enabling SaltyBlockRespawn...");

        loader = new Loader(this);
        loader.load();

        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        CrashProtection crashProtection = new CrashProtection(this, databaseManager);
        crashProtection.RunCrashProt();

        respawnManager = new RespawnManager(this, crashProtection);

        getServer().getPluginManager().registerEvents(new BlockRespawnListener(this, crashProtection, respawnManager), this);

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
        getLogger().info("Disabling SaltyBlockRespawn...");
        // Close sql database connection.
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        // Clean up pending respawn tasks.
        if (respawnManager != null) {
            respawnManager.cancelAll();
        }
        getLogger().info("Disabled SaltyBlockRespawn!");
    }
}
