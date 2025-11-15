package me.lilspojo.blockRespawn.utils;

import org.bukkit.ChatColor;

public class TextColor {
    public class Utils {
        public static String colorize(String message) {
            if (message == null) return "";
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }
}
