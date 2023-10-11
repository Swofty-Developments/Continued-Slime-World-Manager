package net.swofty.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import net.swofty.swm.api.world.SlimeChunk;
import net.swofty.swm.api.world.SlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;

    @Setter
    private final SlimeChunkSection[] sections;
    private final CompoundTag heightMaps;
    private final int[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;
}
