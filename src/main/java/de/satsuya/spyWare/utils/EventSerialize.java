package de.satsuya.spyWare.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.json.simple.JSONObject;

import java.lang.reflect.Field;

public class EventSerialize {
    public static JSONObject serialize(Event event) {
        JSONObject json = new JSONObject();

        json.put("event_type", event.getEventName());
        json.put("event_timestamp", System.currentTimeMillis());

        // Durchläuft die Felder des Event-Objekts
        for (Field field : event.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(event);

                Object serializedValue = simplifyObject(value);

                if (serializedValue != null) {
                    json.put(field.getName(), serializedValue);
                }

            } catch (IllegalAccessException e) {
                ElysiumLogger.error("Fehler beim Zugriff auf Event-Feld: " + field.getName());
            }
        }
        return json;
    }

    /**
     * Konvertiert komplexe Minecraft-Objekte in JSON-fähige JSONObject/primitive Typen.
     */
    private static Object simplifyObject(Object obj) {
        if (obj == null) {
            return null;
        }

        // --- Spezifische Serialisierung für Bukkit/Spigot-Objekte ---

        if (obj instanceof Player) {
            Player player = (Player) obj;
            JSONObject map = new JSONObject();
            map.put("name", player.getName());
            map.put("uuid", player.getUniqueId().toString());
            map.put("location", simplifyObject(player.getLocation()));
            // Füge hier weitere Player-Daten hinzu, die du benötigst (Health, Level, etc.)
            return map;
        }

        if (obj instanceof Location) {
            Location loc = (Location) obj;
            JSONObject map = new JSONObject();
            map.put("world", loc.getWorld().getName());
            map.put("x", loc.getX());
            map.put("y", loc.getY());
            map.put("z", loc.getZ());
            map.put("pitch", loc.getPitch());
            map.put("yaw", loc.getYaw());
            return map;
        }

        // TODO: Füge hier weitere komplexe Typen hinzu, z.B. für ItemStack, Block, Entity
        // if (obj instanceof ItemStack) { ... }

        // --- Standardbehandlung ---

        // Primitive Typen und Strings werden direkt von JSONObject.put() unterstützt
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return obj;
        }

        // Fallback: Wenn der Typ nicht bekannt ist, verwende die toString()-Methode
        // VORSICHT: Dies kann große und nicht standardisierte Strings erzeugen
        return obj.toString();
    }
}
