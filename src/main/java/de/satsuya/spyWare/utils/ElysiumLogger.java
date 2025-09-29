package de.satsuya.spyWare.utils;

import de.satsuya.spyWare.SpyWare;
import de.satsuya.spyWare.loaders.ConfigLoader;

import java.util.logging.Level;

public class ElysiumLogger {
    public static void log(String message) {
        SpyWare.getInstance().getLogger().info("[INFO] " + message);
    }

    public static void error(String message) {
        SpyWare.getInstance().getLogger().severe("[ERROR] " + message);
    }

    public static void debug(String message) {
        if (!isDebugEnabled()) return;
        SpyWare.getInstance().getLogger().log(Level.FINEST,"[DEBUG] " + message);
    }

    public static void chat(String message) {
        SpyWare.getInstance().getLogger().info(message);
    }

    private static boolean isDebugEnabled() {
        // This could be replaced with a configuration check
        return ConfigLoader.configData.getBoolean("debug");
    }
}

