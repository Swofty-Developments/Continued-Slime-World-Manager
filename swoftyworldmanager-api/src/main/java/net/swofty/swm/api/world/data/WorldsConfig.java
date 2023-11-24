package net.swofty.swm.api.world.data;

import java.util.Map;

public interface WorldsConfig {
    /**
     * Saves WorldConfig to the YAML
     */
    void save();

    Map<String, WorldData> getWorlds();
}
