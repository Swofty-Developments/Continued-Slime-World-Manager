package net.swofty.swm.nms.custom;

import com.flowpowered.nbt.CompoundTag;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.nms.NMSUtil;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;

import java.util.Optional;

@Getter
public class CustomWorldData extends WorldData {

    private final CraftSlimeWorld world;
    private final WorldType type;

    CustomWorldData(CraftSlimeWorld world) {
        this.world = world;
        this.type = WorldType.getType(world.getPropertyMap().getValue(SlimeProperties.WORLD_TYPE).toUpperCase());
        this.setGameType(WorldSettings.EnumGamemode.NOT_SET);

        CompoundTag extraData = world.getExtraData();
        Optional<CompoundTag> gameRules = extraData.getAsCompoundTag("gamerules");
        gameRules.ifPresent(compoundTag -> this.x().a((NBTTagCompound) NMSUtil.convertTag(compoundTag)));
    }

    @Override
    public String getName() {
        return world.getName();
    }
}
