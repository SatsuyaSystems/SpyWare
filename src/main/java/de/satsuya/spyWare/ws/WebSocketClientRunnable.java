package de.satsuya.spyWare.ws;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class WebSocketClientRunnable extends BukkitRunnable implements WebSocket.Listener {

    private final JavaPlugin plugin;
    private final String uri;
    private WebSocket webSocket;
    private static final long RECONNECT_DELAY_TICKS = 20L * 5; // 5 seconds delay (20 ticks/second)
    private BukkitTask reconnectTask;

    public WebSocketClientRunnable(JavaPlugin plugin, String uri) {
        this.plugin = plugin;
        this.uri = uri;
    }

    @Override
    public void run() {
        plugin.getLogger().info("⏳ Attempting to establish WebSocket connection to " + uri + "...");

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(uri), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    plugin.getLogger().info("✅ WebSocket connection successfully established.");
                    // Erfolgreiche Verbindung: Task abbrechen/zurücksetzen und Zählversuche löschen
                    if (reconnectTask != null) {
                        reconnectTask.cancel();
                        reconnectTask = null;
                    }
                    // Hinzufügen: Wenn Sie eine Backoff-Logik haben, setzen Sie den Zähler hier zurück
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("❌ Error establishing WebSocket connection: " + ex.getMessage());

                    // 💥 WICHTIGE ÄNDERUNG: Setze den Task auf null, damit scheduleReconnect() einen neuen Task planen kann.
                    this.reconnectTask = null;

                    scheduleReconnect();
                    return null;
                });
    }

    // --- Implementation of the WebSocket.Listener Interface (runs in Java HTTP Client threads) ---

    @Override
    public void onOpen(WebSocket webSocket) {
        plugin.getLogger().info("🔗 WebSocket connection opened.");
        // Must be called to receive messages (backpressure mechanism)
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        // IMPORTANT: Switch back to the Bukkit main thread for interacting with players/APIs
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Message from server: " + message);
                // Further Bukkit interactions here...
            }
        }.runTask(plugin);

        // Request the next message
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        plugin.getLogger().warning("🔌 WebSocket closed. Code: " + statusCode + ", Reason: " + reason);
        this.webSocket = null; // Reset the reference

        // Schedule a reconnect attempt
        scheduleReconnect();

        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        plugin.getLogger().severe("⚠️ WebSocket error: " + error.getMessage());
        // If the error doesn't automatically trigger onClose, we need to manually schedule the reconnect.
        if (this.webSocket != null) {
            this.webSocket.abort(); // Force close
            this.webSocket = null;
        }

        // Schedule a reconnect attempt
        scheduleReconnect();
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        // Handling of binary data (optional)
        webSocket.request(1);
        return null;
    }

    // --- Public method for sending messages (must be called from a safe thread) ---

    public void sendMessage(String message) {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            // Sending is asynchronous, returns a CompletableFuture
            webSocket.sendText(message, true);
        } else {
            plugin.getLogger().warning("Cannot send message: WebSocket is not connected.");
        }
    }

    // --- Method to close the connection ---

    public void close() {
        // Cancel the reconnect task if active
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }

        if (webSocket != null) {
            // Close the connection gracefully with status code 1000 (NORMAL_CLOSURE)
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin Shutdown")
                    .thenRun(() -> plugin.getLogger().info("WebSocket client shut down."))
                    .exceptionally(ex -> {
                        plugin.getLogger().severe("Error closing WebSocket: " + ex.getMessage());
                        webSocket.abort(); // Force close on error
                        return null;
                    });
        }
    }

    // --- Reconnect Logic Method ---

    /**
     * Schedules a reconnect attempt after a specified delay
     * in the Bukkit scheduler.
     */

    private int reconnectAttempts = 0;
    private static final long INITIAL_RECONNECT_DELAY = 5; // Sekunden
    private static final long MAX_RECONNECT_DELAY = 60; // Sekunden

    private void scheduleReconnect() {
        if (reconnectTask == null || reconnectTask.isCancelled()) {

            reconnectAttempts++;

            // Berechne die exponentielle Verzögerung: 5, 10, 20, 40, 60, 60, ... Sekunden
            long currentDelaySeconds = Math.min(
                    INITIAL_RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempts - 1),
                    MAX_RECONNECT_DELAY
            );
            long delayTicks = currentDelaySeconds * 20L;

            plugin.getLogger().info("🔄 Scheduling automatic reconnect attempt in " + currentDelaySeconds + " seconds (Attempt: " + reconnectAttempts + ")...");

            reconnectTask = new BukkitRunnable() {
                @Override
                public void run() {
                    WebSocketClientRunnable.this.run();
                }
            }.runTaskLater(plugin, delayTicks);
        }
    }
}