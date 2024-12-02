package net.swofty.swm.plugin.world;

import net.swofty.swm.plugin.SWMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class DefaultLevelEvents implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = Bukkit.getWorlds().get(0);
        // Check if the default world is a Slime World
        if (!SWMPlugin.getInstance().getConfigManager().getWorldConfig().hasWorld(world.getName()))
            return;

        Location playersLocation = player.getLocation();
        playersLocation.setWorld(world);
        player.teleport(playersLocation);
    }
}
