package me.lilspojo.blockRespawn.commands;

import me.lilspojo.blockRespawn.BlockRespawn;
import me.lilspojo.blockRespawn.loader.Loader;
import me.lilspojo.blockRespawn.utils.TextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.jetbrains.annotations.NotNull;

public class Reload implements CommandExecutor {

    private final Loader loader;

    public Reload(BlockRespawn plugin) {
        this.loader = plugin.getLoader();
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("saltyblockrespawn")){
            return false;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")){
            String commandUsageMsg = loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload.");
            sender.sendMessage(TextColor.Utils.colorize(commandUsageMsg));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")){
            if (!sender.hasPermission("saltyblockrespawn.reload")){
                String noPermissionReloadMsg = loader.getLangConfig().getString("no-permission-reload", "You do not have permission to perform this command!");
                sender.sendMessage(TextColor.Utils.colorize(noPermissionReloadMsg));
                return true;
            }
            // Reload
            loader.reload();
            String reloadMsg = loader.getLangConfig().getString("reload", "SaltyBlockRespawn configuration files reloaded!");
            sender.sendMessage(TextColor.Utils.colorize(reloadMsg));
            return true;
        }
        // Unrecognized arguments
        String commandUsageMsg = loader.getLangConfig().getString("command-usage", "Usage: /saltyblockrespawn reload.");
        sender.sendMessage(TextColor.Utils.colorize(commandUsageMsg));
        return true;
    }


}