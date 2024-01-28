package net.swofty.swm.plugin.world.importer;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;

@AllArgsConstructor
public class MapImporter {
    private final File mapFile;

    CompoundTag loadMap() throws IOException {
        String fileName = mapFile.getName();
        int mapId = Integer.parseInt(fileName.substring(4, fileName.length() - 4));
        CompoundTag tag;

        try (NBTInputStream nbtStream = new NBTInputStream(Files.newInputStream(mapFile.toPath()),
                NBTInputStream.GZIP_COMPRESSION, ByteOrder.BIG_ENDIAN)) {
            tag = nbtStream.readTag().getAsCompoundTag().flatMap(asCompoundTag -> {
                return asCompoundTag.getAsCompoundTag("data");
            }).get();
        }

        tag.getValue().put("id", new IntTag("id", mapId));

        return tag;
    }
}
