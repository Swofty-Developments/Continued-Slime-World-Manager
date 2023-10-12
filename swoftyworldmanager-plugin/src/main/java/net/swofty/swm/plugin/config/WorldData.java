package net.swofty.swm.plugin.config;

import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import lombok.Data;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.bukkit.Difficulty;
import org.bukkit.World;

@Data
@ConfigSerializable
public class WorldData {

    @Setting("source")
    private String dataSource = "file";

    @Setting("spawn")
    private String spawn = "0, 255, 0";

    @Setting("difficulty")
    private String difficulty = "peaceful";

    @Setting("allowMonsters")
    private boolean allowMonsters = true;
    @Setting("allowAnimals")
    private boolean allowAnimals = true;

    @Setting("pvp")
    private boolean pvp = true;

    @Setting("environment")
    private String environment = "NORMAL";
    @Setting("worldType")
    private String worldType = "DEFAULT";

    @Setting("loadOnStartup")
    private boolean loadOnStartup = true;
    @Setting("readOnly")
    private boolean readOnly = false;

    public SlimePropertyMap toPropertyMap() {
        try {
            Enum.valueOf(Difficulty.class, this.difficulty.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown difficulty '" + this.difficulty + "'");
        }

        String[] spawnLocationSplit = spawn.split(", ");

        double spawnX, spawnY, spawnZ;

        try {
            spawnX = Double.parseDouble(spawnLocationSplit[0]);
            spawnY = Double.parseDouble(spawnLocationSplit[1]);
            spawnZ = Double.parseDouble(spawnLocationSplit[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("invalid spawn location '" + this.spawn + "'");
        }

        String environment = this.environment;

        try {
            Enum.valueOf(World.Environment.class, environment.toUpperCase());
        } catch (IllegalArgumentException ex) {
            try {
                int envId = Integer.parseInt(environment);

                if (envId < -1 || envId > 1) {
                    throw new NumberFormatException(environment);
                }

                environment = World.Environment.getEnvironment(envId).name();
            } catch (NumberFormatException ex2) {
                throw new IllegalArgumentException("unknown environment '" + this.environment + "'");
            }
        }

        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, (int) spawnX);
        propertyMap.setValue(SlimeProperties.SPAWN_Y, (int) spawnY);
        propertyMap.setValue(SlimeProperties.SPAWN_Z, (int) spawnZ);

        propertyMap.setValue(SlimeProperties.DIFFICULTY, difficulty);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, allowMonsters);
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, allowAnimals);
        propertyMap.setValue(SlimeProperties.PVP, pvp);
        propertyMap.setValue(SlimeProperties.ENVIRONMENT, environment);
        propertyMap.setValue(SlimeProperties.WORLD_TYPE, worldType);
        propertyMap.setValue(SlimeProperties.LOAD_ON_STARTUP, loadOnStartup);

        return propertyMap;
    }
}
