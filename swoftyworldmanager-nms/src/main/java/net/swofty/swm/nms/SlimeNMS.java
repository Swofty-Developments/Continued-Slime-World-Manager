package net.swofty.swm.nms;

import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.swofty.swm.nms.custom.CustomDataManager;
import net.swofty.swm.nms.custom.CustomWorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SWM");

    private final Set<Integer> pendingDimensions = ConcurrentHashMap.newKeySet();

    private WorldServer defaultWorld;
    private WorldServer defaultNetherWorld;
    private WorldServer defaultEndWorld;

    public SlimeNMS() {
        try {
            CraftCLSMBridge.initialize(this);
        }  catch (NoClassDefFoundError ex) {
            LOGGER.error("Failed to find ClassModifier classes. Are you sure you installed it correctly?");
            System.exit(1);
        }
    }

    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            World.Environment env = World.Environment.valueOf(normalWorld.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase());

            if (env != World.Environment.NORMAL) {
                LOGGER.warn("The environment for the default world must always be 'NORMAL'.");
            }

            defaultWorld = createDefaultWorld(normalWorld, 0);
        }

        if (netherWorld != null) {
            World.Environment env = World.Environment.valueOf(netherWorld.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase());
            defaultNetherWorld = createDefaultWorld(netherWorld, env.getId());
        }

        if (endWorld != null) {
            World.Environment env = World.Environment.valueOf(endWorld.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase());
            defaultEndWorld = createDefaultWorld(endWorld, env.getId());
        }
    }

    private CustomWorldServer createDefaultWorld(SlimeWorld world, int dimension) {
        CustomWorldServer worldServer = new CustomWorldServer((CraftSlimeWorld) world, new CustomDataManager(world), dimension);

        worldServer.setReady(true);
        MinecraftServer.getServer().server.addWorld(worldServer.getWorld());

        return worldServer;
    }

    public int reserveDimension() {
        MinecraftServer mcServer = MinecraftServer.getServer();
        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET;
        boolean used = true;

        while (used) {
            used = pendingDimensions.contains(dimension);

            if (!used) {
                for (WorldServer server : mcServer.worlds) {
                    if (server.dimension == dimension) {
                        used = true;
                        break;
                    }
                }
            }

            if (used) {
                dimension++;
            }
        }

        pendingDimensions.add(dimension);

        return dimension;
    }

    public void releaseDimension(int dimension) {
        pendingDimensions.remove(dimension);
    }

    public Object createNMSWorld(SlimeWorld world, int dimension) {
        return new CustomWorldServer((CraftSlimeWorld) world, new CustomDataManager(world), dimension);
    }

    public void addWorldToServerList(Object worldObject) {
        if (!(worldObject instanceof WorldServer)) {
            throw new IllegalArgumentException("World object must be an instance of WorldServer!");
        }

        CustomWorldServer server = (CustomWorldServer) worldObject;
        String worldName = server.getWorldData().getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        server.setReady(true);
        MinecraftServer mcServer = MinecraftServer.getServer();

        mcServer.server.addWorld(server.getWorld());
        mcServer.worlds.add(server);
        releaseDimension(server.dimension);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        LOGGER.info("World " + worldName + " loaded.");
    }

    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();

        return worldServer.getSlimeWorld();
    }

    public boolean isDefaultWorld(World world) {
        Object handle = ((CraftWorld) world).getHandle();

        return handle == defaultWorld || handle == defaultNetherWorld || handle == defaultEndWorld;
    }

    public boolean isUnloading(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        return craftWorld.getHandle() instanceof CustomWorldServer && ((CustomWorldServer) craftWorld.getHandle()).isUnloading();
    }

    public Runnable createSaveAwaiter(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return () -> { };
        }

        CustomWorldServer server = (CustomWorldServer) craftWorld.getHandle();

        return server::awaitPendingSave;
    }
}
