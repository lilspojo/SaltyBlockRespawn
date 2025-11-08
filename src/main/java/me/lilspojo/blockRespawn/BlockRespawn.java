package me.lilspojo.blockRespawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;



public final class BlockRespawn extends JavaPlugin {

    private Loader loader;

    public Loader getLoader() {
        return loader;
    }

    @Override
    public void onEnable() {

        getLogger().info("Enabling SaltyBlockRespawn...");

        loader = new Loader(this);
        loader.load();

        getServer().getPluginManager().registerEvents(new BlockRespawnListener(this), this);

        getLogger().info("Enabled SaltyBlockRespawn!");

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

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
            loader.load();

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

    }
}
