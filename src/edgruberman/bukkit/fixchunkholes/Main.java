package edgruberman.bukkit.fixchunkholes;

import java.util.Arrays;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import edgruberman.bukkit.messagemanager.MessageLevel;
import edgruberman.bukkit.messagemanager.MessageManager;

public final class Main extends org.bukkit.plugin.java.JavaPlugin {
    
    private static ConfigurationFile configurationFile;
    private static MessageManager messageManager;
    
    private int radiusDefault;
    private int radiusMaximum;
    
    public void onLoad() {
        Main.configurationFile = new ConfigurationFile(this);
        Main.getConfigurationFile().load();
        
        Main.messageManager = new MessageManager(this);
        Main.getMessageManager().log("Version " + this.getDescription().getVersion());
    }
	
    public void onEnable() {
        this.radiusDefault = this.getConfiguration().getInt("radius.default", this.radiusDefault);
        Main.getMessageManager().log("Default Radius: " + this.radiusDefault, MessageLevel.CONFIG);
        
        this.radiusMaximum = this.getConfiguration().getInt("radius.maximum", this.radiusMaximum);
        Main.getMessageManager().log("Maximum Radius: " + this.radiusMaximum, MessageLevel.CONFIG);
        
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, new PlayerListener(this), Event.Priority.Monitor, this);
        
        Main.getMessageManager().log("Plugin Enabled");
    }
    
    public void onDisable() {
        Main.getMessageManager().log("Plugin Disabled");
    }
    
    static ConfigurationFile getConfigurationFile() {
        return Main.configurationFile;
    }
    
    static MessageManager getMessageManager() {
        return Main.messageManager;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Main.getMessageManager().log(
                ((sender instanceof Player) ? ((Player) sender).getName() : "[CONSOLE]")
                    + " issued command: " + commandLabel + " " + this.join(args)
                , MessageLevel.FINE
        );
        
        if (!(sender instanceof Player)) {
            Main.getMessageManager().respond(sender, "You must be a player to use this command.", MessageLevel.RIGHTS);
            return true;
        }
        
        Integer radius = null;
        if (args.length != 0 && this.isInteger(args[0])) {
            radius = Integer.parseInt(args[0]);
            
            if (radius > this.radiusMaximum) {
                Main.getMessageManager().respond(sender, "Radius \"" + radius + "\" too large; Maximum allowed is " + this.radiusMaximum + ".", MessageLevel.SEVERE);
                return true;
            }
        }
        
        int refreshed = 0;
        Player player = (Player) sender;
        
        // Refresh centered at chunk player is standing in.
        refreshed += this.refreshChunk(player.getLocation().getBlock(), radius);
        
        // Check if player is pointing at a chunk to refresh.
        // Do not refresh chunk player is pointing at if already refreshed because they were standing in it.
        Block block = player.getTargetBlock(null, 100);
        if (block != null && !block.getChunk().equals(player.getLocation().getBlock().getChunk())) 
            refreshed += this.refreshChunk(block, radius);
        
        Main.getMessageManager().respond(sender, "Refreshed " + refreshed + " chunk" + (refreshed == 1 ? "" : "s") + ".", MessageLevel.STATUS);
        
        return true;
    }
    
    protected int refreshChunk(Block block) {
        return this.refreshChunk(block, this.radiusDefault);
    }
    
    protected int refreshChunk(Block block, Integer radius) {
        int refreshed = 0;
        
        if (block == null || block.getChunk() == null) return refreshed;
        
        // Refresh chunk at center of request first.
        this.refreshChunk(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        refreshed++; 
        
        if (radius == null) radius = this.radiusDefault;
        if (radius <= 0) return refreshed;
        
        // Refresh chunks spirally outward.
        for ( int i = 1; i <= radius; i++ ) {
            // north row, west to east
            for ( int dZ = i; dZ >= -i; dZ-- ) {
                this.refreshChunk(block.getWorld(), block.getChunk().getX() - i, block.getChunk().getZ() + dZ);
                refreshed++;
            }
            
            // east row, north to south
            for ( int dX = -i + 1; dX <= i; dX++ ) {
                this.refreshChunk(block.getWorld(), block.getChunk().getX() + dX, block.getChunk().getZ() - i);
                refreshed++;
            }
            
            // south row, east to west
            for ( int dZ = -i + 1; dZ <= i; dZ++ ) {
                this.refreshChunk(block.getWorld(), block.getChunk().getX() + i, block.getChunk().getZ() + dZ);
                refreshed++;
            }
            
            // west row, south to north
            for ( int dX = i - 1; dX >= -i + 1; dX-- ) {
                this.refreshChunk(block.getWorld(), block.getChunk().getX() + dX, block.getChunk().getZ() + i);
                refreshed++;
            }
        }
        
        return refreshed;
    }
    
    protected void refreshChunk(World world, int chunkX, int chunkZ) {
        Main.getMessageManager().log("Refreshing chunk in \"" + world.getName() + "\" at cX:" + chunkX + " cZ:" + chunkZ, MessageLevel.FINE);
        world.refreshChunk(chunkX, chunkZ);
    }
    
    private boolean isInteger(String s) {   
        try {   
            Integer.parseInt(s);   
            return true;   
        }   
        catch(Exception e) {   
            return false;   
        }   
    }
    
    private String join(String[] s) {
        return this.join(Arrays.asList(s), " ");
    }
    
    private String join(List<String> list, String delim) {
        if (list == null || list.isEmpty()) return "";
     
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append(s + delim);
        sb.delete(sb.length() - delim.length(), sb.length());
        
        return sb.toString();
    }
}
