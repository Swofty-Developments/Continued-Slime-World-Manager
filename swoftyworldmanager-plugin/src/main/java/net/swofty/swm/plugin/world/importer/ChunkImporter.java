package net.swofty.swm.plugin.world.importer;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.swofty.swm.api.utils.MathUtility;
import net.swofty.swm.api.utils.NibbleArray;
import net.swofty.swm.api.world.SlimeChunk;
import net.swofty.swm.api.world.SlimeChunkSection;
import net.swofty.swm.nms.craft.CraftSlimeChunk;
import net.swofty.swm.nms.craft.CraftSlimeChunkSection;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@AllArgsConstructor
public class ChunkImporter {
    private static final int SECTOR_SIZE = 4096;
    private final File worldDir;

    static List<SlimeChunk> loadChunks(File file) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file.toPath());
        List<ChunkEntry> chunks = getChunkEntries(regionByteArray);

        List<SlimeChunk> loadedChunks = chunks.stream().map((entry) -> {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset(), entry.getPaddedSize()));

                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();

                if (!globalMap.containsKey("Level")) {
                    throw new RuntimeException("Missing Level tag?");
                }

                CompoundTag levelCompound = (CompoundTag) globalMap.get("Level");

                return readChunk(levelCompound);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());

        return loadedChunks;
    }

    private static List<ChunkEntry> getChunkEntries(byte[] regionByteArray) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));

        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }
        return chunks;
    }

    private static SlimeChunk readChunk(CompoundTag compound) {
        int chunkX = compound.getAsIntTag("xPos").get().getValue();
        int chunkZ = compound.getAsIntTag("zPos").get().getValue();
        Optional<String> status = compound.getStringValue("Status");

        if (status.isPresent() && !status.get().equals("postprocessed") && !status.get().startsWith("full")) {
            // It's a protochunk
            return null;
        }

        int[] biomes;
        Tag biomesTag = compound.getValue().get("Biomes");

        if (biomesTag instanceof IntArrayTag) {
            biomes = ((IntArrayTag) biomesTag).getValue();
        } else if (biomesTag instanceof ByteArrayTag) {
            byte[] byteBiomes = ((ByteArrayTag) biomesTag).getValue();
            biomes = MathUtility.toIntArray(byteBiomes);
        } else {
            biomes = null;
        }

        CompoundTag heightMapsCompound = new CompoundTag("", new CompoundMap());
        int[] heightMap = compound.getIntArrayValue("HeightMap").orElse(new int[256]);
        heightMapsCompound.getValue().put("heightMap", new IntArrayTag("heightMap", heightMap));

        List<CompoundTag> tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("TileEntities")
                .orElse(new ListTag<>("TileEntities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
        List<CompoundTag> entities = ((ListTag<CompoundTag>) compound.getAsListTag("Entities")
                .orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
        ListTag<CompoundTag> sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("Sections").get();
        SlimeChunkSection[] sectionArray = new SlimeChunkSection[16];

        for (CompoundTag sectionTag : sectionsTag.getValue()) {
            int index = sectionTag.getByteValue("Y").get();

            byte[] blocks = sectionTag.getByteArrayValue("Blocks").orElse(null);
            NibbleArray dataArray;
            ListTag<CompoundTag> paletteTag;
            long[] blockStatesArray;

            dataArray = new NibbleArray(sectionTag.getByteArrayValue("Data").get());

            if (MathUtility.isEmpty(blocks)) { // Just skip it
                continue;
            }

            paletteTag = null;
            blockStatesArray = null;

            NibbleArray blockLightArray = sectionTag.getValue().containsKey("BlockLight") ? new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get()) : null;
            NibbleArray skyLightArray = sectionTag.getValue().containsKey("SkyLight") ? new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get()) : null;

            sectionArray[index] = new CraftSlimeChunkSection(blocks, dataArray, paletteTag, blockStatesArray, blockLightArray, skyLightArray);
        }

        for (SlimeChunkSection section : sectionArray) {
            if (section != null) { // Chunk isn't empty
                return new CraftSlimeChunk(null, chunkX, chunkZ, sectionArray, heightMapsCompound, biomes, tileEntities, entities);
            }
        }

        // Chunk is empty
        return null;
    }

    @Getter
    @RequiredArgsConstructor
    private static class ChunkEntry {
        private final int offset;
        private final int paddedSize;
    }
}
