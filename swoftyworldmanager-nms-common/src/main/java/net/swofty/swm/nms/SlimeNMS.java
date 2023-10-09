package net.swofty.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import net.swofty.swm.api.world.SlimeWorld;
import org.bukkit.World;

public interface SlimeNMS {

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld);
    void generateWorld(SlimeWorld world);

    Object createNMSWorld(SlimeWorld world);

    void addWorldToServerList(Object worldObject);

    SlimeWorld getSlimeWorld(World world);
    byte getWorldVersion();

    default CompoundTag convertChunk(CompoundTag chunkTag) {
        return chunkTag;
    }
}
