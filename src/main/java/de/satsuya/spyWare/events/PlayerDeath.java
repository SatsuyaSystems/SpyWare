package de.satsuya.spyWare.events;

import de.satsuya.spyWare.SpyWare;
import de.satsuya.spyWare.loaders.ConfigLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {

        // --- 1. Extract Data from the Event ---

        Player victim = event.getEntity();
        String victimName = victim.getName();

        // Get Location (Rounding coordinates for cleaner output)
        Location loc = victim.getLocation();
        String worldName = loc.getWorld().getName();
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());

        // Determine the Killer/Death Source
        String killerIdentifier = "Environment/Mob"; // Default death cause

        Player killer = victim.getKiller();
        if (killer != null) {
            // Killer was a player
            killerIdentifier = killer.getName();
        } else {
            // Killer was a mob, explosion, fall, etc.
            // We use the death message for context, escaping quotes if necessary.
            String deathMessage = event.getDeathMessage();
            if (deathMessage != null) {
                // Try to strip the victim's name for a cleaner cause description
                // Example: "Notch was slain by a Zombie" -> "slain by a Zombie"
                killerIdentifier = deathMessage
                        .replace(victimName, "")
                        .trim()
                        .replaceAll("\"", "\\\""); // Escape quotes in the string
            }
        }

        // --- 2. Build the Custom JSON Payload ---

        // Using String.format to construct the JSON string cleanly.
        String jsonPayload = String.format(
                "{" +
                        "\"api_key\": \"" + ConfigLoader.configData.getString("api-key") + "\"," +
                        "\"externalId\": \"" + ConfigLoader.configData.getString("externalId") + "\"," +
                        "\"event_type\": \"PLAYER_DEATH\"," +
                        "\"victim_name\": \"%s\"," +
                        "\"killer_source\": \"%s\"," +
                        "\"location\": {" +
                        "\"world\": \"%s\"," +
                        "\"x\": %d," +
                        "\"y\": %d," +
                        "\"z\": %d" +
                        "}" +
                        "}",
                victimName, killerIdentifier, worldName, x, y, z
        );

        // --- 3. Send the Message ---
        SpyWare.wsClient.sendMessage(jsonPayload);
    }
}
