package net.swofty.swm.plugin.world.importer;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.swofty.swm.api.exceptions.InvalidWorldException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class LevelImporter {
    private final File worldDir;

    public LevelData readLevelData() throws IOException, InvalidWorldException {
        Optional<CompoundTag> tag;

        try (NBTInputStream nbtStream = new NBTInputStream(Files.newInputStream(worldDir.toPath()))) {
            tag = nbtStream.readTag().getAsCompoundTag();
        }

        if (tag.isPresent()) {
            Optional<CompoundTag> dataTag = tag.get().getAsCompoundTag("Data");

            if (dataTag.isPresent()) {
                // Game rules
                Map<String, String> gameRules = new HashMap<>();
                Optional<CompoundTag> rulesList = dataTag.get().getAsCompoundTag("GameRules");

                rulesList.ifPresent(compoundTag -> compoundTag.getValue().forEach((ruleName, ruleTag) ->
                        gameRules.put(ruleName, ruleTag.getAsStringTag().get().getValue())));

                int spawnX = dataTag.get().getIntValue("SpawnX").orElse(0);
                int spawnY = dataTag.get().getIntValue("SpawnY").orElse(255);
                int spawnZ = dataTag.get().getIntValue("SpawnZ").orElse(0);

                return new LevelData(gameRules, spawnX, spawnY, spawnZ);
            }
        }

        throw new InvalidWorldException(worldDir.getParentFile());
    }

    @Getter
    @RequiredArgsConstructor
    public static class LevelData {
        private final Map<String, String> gameRules;

        private final int spawnX;
        private final int spawnY;
        private final int spawnZ;
    }
}
