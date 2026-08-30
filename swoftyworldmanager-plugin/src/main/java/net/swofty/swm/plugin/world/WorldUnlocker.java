package net.swofty.swm.plugin.world;

import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.nms.SlimeNMS;
import net.swofty.swm.plugin.SWMPlugin;
import net.swofty.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;

public class WorldUnlocker implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        SlimeNMS nms = SWMPlugin.getInstance().getNms();
        SlimeWorld world = nms.getSlimeWorld(event.getWorld());

        if (world == null || world.isReadOnly() || nms.isUnloading(event.getWorld())) {
            return;
        }

        Runnable saveAwaiter = nms.createSaveAwaiter(event.getWorld());

        Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {
            saveAwaiter.run();
            unlockWorld(world);
        });
    }

    private void unlockWorld(SlimeWorld world) {
        try {
            world.getLoader().unlockWorld(world.getName());
        } catch (IOException ex) {
            Logging.error("Failed to unlock world " + world.getName() + ". Retrying in 5 seconds. Stack trace:");
            ex.printStackTrace();

            Bukkit.getScheduler().runTaskLaterAsynchronously(SWMPlugin.getInstance(), () -> unlockWorld(world), 100);
        } catch (UnknownWorldException ignored) {}
    }
}
