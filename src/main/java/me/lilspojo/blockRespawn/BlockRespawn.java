package me.lilspojo.blockRespawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import org.jetbrains.annotations.NotNull;

public final class BlockRespawn extends JavaPlugin {

    private Loader loader;
    private RespawnManager respawnManager;
    private DatabaseManager databaseManager;
    private BlockRespawnListener blockRespawnListener;
    private NexoBlockChecker nexoBlockChecker;

    public Loader getLoader() {
        return loader;
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

        nexoBlockChecker = new NexoBlockChecker();

        respawnManager = new RespawnManager(this, crashProtection, nexoBlockChecker);
        blockRespawnListener = new BlockRespawnListener(this, respawnManager, nexoBlockChecker);

        getServer().getPluginManager().registerEvents(new BlockRespawnListener(this, respawnManager, nexoBlockChecker), this);
        getServer().getPluginManager().registerEvents(new BlockPhysicsListener(this, blockRespawnListener), this);

        getLogger().info("Enabled SaltyBlockRespawn!");
    }
    // Reload command
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("saltyblockrespawn")){
            return false;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")){
            String commandUsageMsg = loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload.");
            sender.sendMessage(Utils.colorize(commandUsageMsg));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")){
            if (!sender.hasPermission("saltyblockrespawn.reload")){
                String noPermissionReloadMsg = loader.getLangConfig().getString("no-permission-reload", "You do not have permission to perform this command!");
                sender.sendMessage(Utils.colorize(noPermissionReloadMsg));
                return true;
            }
            // Reload
            loader.reload();
            String reloadMsg = loader.getLangConfig().getString("reload", "SaltyBlockRespawn configuration files reloaded!");
            sender.sendMessage(Utils.colorize(reloadMsg));
            return true;
        }
        // Unrecognized arguments
        String commandUsageMsg = loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload.");
        sender.sendMessage(Utils.colorize(commandUsageMsg));
        return true;
    }

    public class Utils {
        public static String colorize(String message) {
            if (message == null) return "";
            return ChatColor.translateAlternateColorCodes('&', message);
        }
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
