package net.swofty.swm.nms.custom;

import com.flowpowered.nbt.CompoundTag;
import net.swofty.swm.api.utils.NibbleArray;
import net.swofty.swm.api.world.SlimeChunk;
import net.swofty.swm.api.world.SlimeChunkSection;
import net.swofty.swm.nms.NMSSlimeChunk;
import net.swofty.swm.nms.NMSUtil;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@RequiredArgsConstructor
public class CustomChunkLoader implements IChunkLoader {

    private static final Logger LOGGER = LogManager.getLogger("SWM Chunk Loader");

    private final CraftSlimeWorld world;

    void loadAllChunks(CustomWorldServer server) {
        for (SlimeChunk chunk : new ArrayList<>(world.getChunks().values())) {
            Chunk nmsChunk = createChunk(server, chunk);
            NMSSlimeChunk slimeChunk = new NMSSlimeChunk(nmsChunk);
            world.updateChunk(slimeChunk);

            // Manually call Java garbage collector
            System.gc();
        }
    }

    private Chunk createChunk(CustomWorldServer server, SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        LOGGER.debug("Loading chunk (" + x + ", " + z + ") on world " + world.getName());

        Chunk nmsChunk = new Chunk(server, x, z);

        nmsChunk.d(true);
        nmsChunk.e(true);

        // Height map
        CompoundTag heightMapsCompound = chunk.getHeightMaps();
        int[] heightMap = heightMapsCompound.getIntArrayValue("heightMap").get();

        nmsChunk.a(heightMap);

        // Load chunk sections
        LOGGER.debug("Loading chunk sections for chunk (" + x + ", " + z + ") on world " + world.getName());
        ChunkSection[] sections = new ChunkSection[16];

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                ChunkSection section = new ChunkSection(sectionId << 4, true);
                NibbleArray data = slimeSection.getData();

                byte[] blocks = slimeSection.getBlocks();
                char[] blockIds = new char[blocks.length];

                for (int id = 0; id < blocks.length; id++) {
                    int blockId = blocks[id] & 255;
                    int blockData = data.get(id);
                    int packed = blockId << 4 | blockData;

                    if (Block.d.a(packed) == null) {
                        Block block = Block.getById(blockId);

                        if (block != null) {
                            try {
                                blockData = block.toLegacyData(block.fromLegacyData(blockData));
                            } catch (Exception ex) {
                                blockData = block.toLegacyData(block.getBlockData());
                            }

                            packed = blockId << 4 | blockData;
                        }
                    }

                    blockIds[id] = (char) packed;
                }

                LOGGER.debug("ChunkSection #" + sectionId + " - Chunk (" + x + ", " + z + ") - World " + world.getName() + ":");
                LOGGER.debug("Blocks:");
                LOGGER.debug(blockIds);
                LOGGER.debug("Block light array:");
                LOGGER.debug(slimeSection.getBlockLight() != null ? slimeSection.getBlockLight().getBacking() : "Not present");
                LOGGER.debug("Sky light array:");
                LOGGER.debug(slimeSection.getSkyLight() != null ? slimeSection.getSkyLight().getBacking() : "Not present");

                section.a(blockIds);

                if (slimeSection.getBlockLight() != null) {
                    section.a(NMSUtil.convertArray(slimeSection.getBlockLight()));
                }

                if (slimeSection.getSkyLight() != null) {
                    section.b(NMSUtil.convertArray(slimeSection.getSkyLight()));
                }

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        nmsChunk.a(sections);

        // Biomes
        nmsChunk.a(toByteArray(chunk.getBiomes()));

        // Load tile entities
        LOGGER.debug("Loading tile entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        List<CompoundTag> tileEntities = chunk.getTileEntities();
        int loadedEntities = 0;

        if (tileEntities != null) {
            for (CompoundTag tag : tileEntities) {
                TileEntity entity = TileEntity.c((NBTTagCompound) NMSUtil.convertTag(tag));

                if (entity != null) {
                    nmsChunk.a(entity);
                    loadedEntities++;
                }
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " tile entities for chunk (" + x + ", " + z + ") on world " + world.getName());

        // Load entities
        LOGGER.debug("Loading entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        List<CompoundTag> entities = chunk.getEntities();
        loadedEntities = 0;

        if (entities != null) {
            for (CompoundTag tag : entities) {
                Entity entity = EntityTypes.a((NBTTagCompound) NMSUtil.convertTag(tag), server);
                nmsChunk.g(true);

                if (entity != null) {
                    nmsChunk.a(entity);
                    Entity entity1 = entity;

                    for (CompoundTag ridingTag = tag; ridingTag.getValue().containsKey("Riding"); ridingTag = (CompoundTag) ridingTag.getValue().get("Riding")) {
                        Entity entity2 = EntityTypes.a((NBTTagCompound) NMSUtil.convertTag(ridingTag.getValue().get("Riding")), server);

                        if (entity2 != null) {
                            nmsChunk.a(entity2);
                            entity1.mount(entity2);

                            entity1 = entity2;
                            loadedEntities++;
                        }
                    }

                    loadedEntities++;
                }
            }
        }

        LOGGER.debug("Loaded " + loadedEntities + " entities for chunk (" + x + ", " + z + ") on world " + world.getName());
        LOGGER.debug("Loaded chunk (" + x + ", " + z + ") on world " + world.getName());

        return nmsChunk;
    }

    private byte[] toByteArray(int[] ints) {
        ByteBuffer buf = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.asIntBuffer().put(ints);

        return buf.array();
    }

    // Load chunk
    @Override
    public Chunk a(World nmsWorld, int x, int z) {
        SlimeChunk slimeChunk = world.getChunk(x, z);
        Chunk chunk;

        if (slimeChunk == null) {
            chunk = new Chunk(nmsWorld, x, z);

            chunk.d(true);
            chunk.e(true);
        } else if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk();
        } else { // All SlimeChunk objects should be converted to NMSSlimeChunks when loading the world
            throw new IllegalStateException("Chunk (" + x + ", " + z + ") has not been converted to a NMSSlimeChunk object!");
        }

        return chunk;
    }

    // Save chunk
    @Override
    public void a(World world, Chunk chunk) {
        SlimeChunk slimeChunk = this.world.getChunk(chunk.locX, chunk.locZ);

        if (slimeChunk instanceof NMSSlimeChunk) { // In case somehow the chunk object changes (might happen for some reason)
            ((NMSSlimeChunk) slimeChunk).setChunk(chunk);
        } else {
            this.world.updateChunk(new NMSSlimeChunk(chunk));
        }
    }


    // Save all chunks
    @Override
    public void b() {

    }

    // Does literally nothing
    @Override
    public void b(World world, Chunk chunk) { }

    // Does literally nothing
    @Override
    public void a() {  }
}
