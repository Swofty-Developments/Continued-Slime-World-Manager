package net.swofty.swm.nms.custom;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomWorldServer extends WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("SWM World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());

    @Getter
    private final CraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();

    private volatile CompletableFuture<Void> pendingSave = CompletableFuture.completedFuture(null);

    @Getter
    @Setter
    private boolean ready = false;

    @Getter
    @Setter
    private volatile boolean unloading = false;

    public CustomWorldServer(CraftSlimeWorld world, IDataManager dataManager, int dimension) {
        super(MinecraftServer.getServer(), dataManager, dataManager.getWorldData(), dimension, MinecraftServer.getServer().methodProfiler,
                World.Environment.valueOf(world.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT).toUpperCase()), null);

        b();
        this.slimeWorld = world;
        this.tracker = new EntityTracker(this);
        addIWorldAccess(new WorldManager(MinecraftServer.getServer(), this));

        SlimePropertyMap propertyMap = world.getPropertyMap();

        worldData.setDifficulty(EnumDifficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        worldData.setSpawn(new BlockPosition(propertyMap.getValue(SlimeProperties.SPAWN_X), propertyMap.getValue(SlimeProperties.SPAWN_Y), propertyMap.getValue(SlimeProperties.SPAWN_Z)));
        super.setSpawnFlags(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);
    }

    @Override
    public void save(boolean forceSave, IProgressUpdate progressUpdate) throws ExceptionWorldConflict {
        if (slimeWorld.isReadOnly()) {
            return;
        }

        super.save(forceSave, progressUpdate);

        if (unloading || MinecraftServer.getServer().isStopped()) {
            save();

            if (MinecraftServer.getServer().isStopped()) {
                try {
                    slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                } catch (IOException ex) {
                    LOGGER.error("Failed to unlock the world " + slimeWorld.getName() + ". Please unlock it manually by using the command /swm manualunlock. Stack trace:");

                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }
            }
        } else {
            pendingSave = CompletableFuture.runAsync(() -> {
                if (!unloading) {
                    save();
                }
            }, WORLD_SAVER_SERVICE);
        }
    }

    public void awaitPendingSave() {
        pendingSave.join();
    }

    private void save() {
        synchronized (saveLock) {
            try {
                LOGGER.info("Saving world " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();
                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);
                LOGGER.info("World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
