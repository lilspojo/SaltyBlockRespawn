package me.lilspojo.blockRespawn;

import me.lilspojo.blockRespawn.blockPhysics.BlockPhysicsListener;
import me.lilspojo.blockRespawn.blockRespawn.BlockRespawnListener;
import me.lilspojo.blockRespawn.commands.Reload;
import me.lilspojo.blockRespawn.blockRespawn.RespawnManager;
import me.lilspojo.blockRespawn.crashProtection.CrashProtection;
import me.lilspojo.blockRespawn.database.DatabaseManager;
import me.lilspojo.blockRespawn.loader.Loader;
import me.lilspojo.blockRespawn.nexo.NexoBlockChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockRespawn extends JavaPlugin {

    private Loader loader;
    private Reload reloadCommand;
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

        reloadCommand = new Reload(this);
        this.getCommand("saltyblockrespawn").setExecutor(reloadCommand);

        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        CrashProtection crashProtection = new CrashProtection(this, databaseManager);
        crashProtection.runCrashProt();

        nexoBlockChecker = new NexoBlockChecker();

        respawnManager = new RespawnManager(this, crashProtection, nexoBlockChecker);
        blockRespawnListener = new BlockRespawnListener(this, respawnManager, nexoBlockChecker);

        getServer().getPluginManager().registerEvents(new BlockRespawnListener(this, respawnManager, nexoBlockChecker), this);
        getServer().getPluginManager().registerEvents(new BlockPhysicsListener(this, blockRespawnListener), this);

        getLogger().info("Enabled SaltyBlockRespawn!");
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
