package net.swofty.swm.plugin.world.importer;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import net.swofty.swm.api.exceptions.InvalidWorldException;
import net.swofty.swm.api.world.SlimeChunk;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.SlimeWorldImporter;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ImporterImpl implements SlimeWorldImporter {
    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^(?:map_([0-9]*).dat)$");

    @Override
    public SlimeWorld readFromDirectory(File worldDir) throws InvalidWorldException {
        File levelFile = new File(worldDir, "level.dat");
        File regionDir = new File(worldDir, "region");
        File dataDir = new File(worldDir, "data");

        if (!levelFile.exists() || !levelFile.isFile()
                || !regionDir.exists() || !regionDir.isDirectory()
                || !dataDir.exists() || !dataDir.isDirectory()) {
            throw new InvalidWorldException(worldDir);
        }

        System.out.println("Reading world from path " + worldDir.getPath());

        /**
         * Step 1: Read level.dat for world data such as gamerules
         */
        CompoundMap extraData = new CompoundMap();
        LevelImporter.LevelData data;
        try {
            data = new LevelImporter(levelFile).readLevelData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!data.getGameRules().isEmpty()) {
            CompoundMap gamerules = new CompoundMap();
            data.getGameRules().forEach((rule, value) -> gamerules.put(rule, new StringTag(rule, value)));

            extraData.put("gamerules", new CompoundTag("gamerules", gamerules));
        }

        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, data.getSpawnX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, data.getSpawnY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, data.getSpawnZ());

        System.out.println("Importing chunks from path " + regionDir.getPath());

        /**
         * Step 2: Chunks
         */
        Map<Long, SlimeChunk> chunks = new HashMap<>();

        for (File file : regionDir.listFiles((dir, name) -> name.endsWith(".mca"))) {
            try {
                chunks.putAll(ChunkImporter.loadChunks(file).stream().collect(
                        Collectors.toMap((chunk) -> ((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()), (chunk) -> chunk)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (chunks.isEmpty()) {
            throw new InvalidWorldException(worldDir);
        }

        System.out.println("Importing maps from path " + dataDir.getPath());

        /**
         * Step 3: Load WorldMaps
         */
        List<CompoundTag> maps = new ArrayList<>();
        for (File mapFile : dataDir.listFiles((dir, name) -> MAP_FILE_PATTERN.matcher(name).matches())) {
            try {
                maps.add(new MapImporter(mapFile).loadMap());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new CraftSlimeWorld(null, worldDir.getName(), chunks, new CompoundTag("", extraData),
                maps, propertyMap, false, true);
    }
}
