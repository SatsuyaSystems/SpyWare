package de.satsuya.spyWare.events;

import de.satsuya.spyWare.SpyWare;
import de.satsuya.spyWare.utils.EventSerialize;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerDeath implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        SpyWare.wsClient.sendMessage(EventSerialize.serialize(event).toJSONString());
    }

}
