package net.swofty.swm.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import net.swofty.swm.api.utils.NibbleArray;

/**
 * In-memory representation of a SRF chunk section.
 */
public interface SlimeChunkSection {

    /**
     * Returns all the blocks of the chunk section
     *
     * @return A <code>byte[]</code> with all the blocks of a chunk section.
     */
    byte[] getBlocks();

    /**
     * Returns the data of all the blocks of the chunk section
     *
     * @return A {@link NibbleArray} containing all the blocks of a chunk section.
     */
    NibbleArray getData();

    /**
     * Returns the block light data.
     *
     * @return A {@link NibbleArray} with the block light data.
     */
    NibbleArray getBlockLight();

    /**
     * Returns the sky light data.
     *
     * @return A {@link NibbleArray} containing the sky light data.
     */
    NibbleArray getSkyLight();
}
