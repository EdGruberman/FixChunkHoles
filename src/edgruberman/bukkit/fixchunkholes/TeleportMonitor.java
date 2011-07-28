package edgruberman.bukkit.fixchunkholes;

import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportMonitor extends org.bukkit.event.player.PlayerListener {
    
    private Main plugin;
    
    public TeleportMonitor(Main plugin) {
        this.plugin = plugin;
        
        plugin.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, this, Event.Priority.Monitor, plugin);
    }
    
    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        
        // Only automatically refresh chunk if moving more than 1 chunk in a teleport.
        // This helps to avoid chunk refreshes for things like entering bed and minecarts even just across a chunk boundary.
        if (
                   Math.abs(event.getFrom().getBlock().getChunk().getX() - event.getTo().getBlock().getChunk().getX()) <= 1
                && Math.abs(event.getFrom().getBlock().getChunk().getZ() - event.getTo().getBlock().getChunk().getZ()) <= 1
        ) return;
        
        this.plugin.refreshChunk(event.getTo().getBlock());
    }
}