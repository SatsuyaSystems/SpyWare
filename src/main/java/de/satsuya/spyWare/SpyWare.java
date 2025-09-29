package de.satsuya.spyWare;

import de.satsuya.spyWare.loaders.ConfigLoader;
import de.satsuya.spyWare.loaders.EventLoader;
import de.satsuya.spyWare.ws.WebSocketClientRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class SpyWare extends JavaPlugin {
    private static SpyWare instance;
    public static WebSocketClientRunnable wsClient;

    public static SpyWare getInstance() {
        return instance;
    }


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        try {
            ConfigLoader.setupConfig();
            ConfigLoader.loadConfig();

            // Starten des asynchronen WebSocket-Clients beim Plugin-Start
            String wsUri = ConfigLoader.configData.getString("ws"); // URI anpassen!
            wsClient = new WebSocketClientRunnable(this, wsUri);

            // Da der WebSocket-Client intern bereits asynchron ist,
            // starten wir das Runnable nur einmal (oder gar nicht, wenn keine Bukkit-Threads nötig sind).
            // Wir verwenden es hier hauptsächlich, um die Bukkit-Instanz zu übergeben.
            // Der eigentliche Verbindungsaufbau ist ASYNCHRON und BLOCKIERT NICHT den Haupt-Thread.
            wsClient.runTaskAsynchronously(this);

            EventLoader.loadEvents(this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
