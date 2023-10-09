package net.swofty.swm.nms;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import net.swofty.swm.api.utils.NibbleArray;
import net.swofty.swm.api.world.SlimeChunkSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CraftSlimeChunkSection implements SlimeChunkSection {

    // Pre 1.13 block data
    private final byte[] blocks;
    private final NibbleArray data;

    // Post 1.13 block data
    private final ListTag<CompoundTag> palette;
    private final long[] blockStates;

    private final NibbleArray blockLight;
    private final NibbleArray skyLight;
}
