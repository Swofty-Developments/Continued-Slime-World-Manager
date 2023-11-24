package net.swofty.swm.plugin.config;

import com.google.common.reflect.TypeToken;
import net.swofty.swm.plugin.log.Logging;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class WorldsConfig implements net.swofty.swm.api.world.data.WorldsConfig {

    @Setting("worlds")
    private Map<String, net.swofty.swm.api.world.data.WorldData> worlds = new HashMap<>();

    @Override
    public void save() {
        try {
            ConfigManager.getWorldConfigLoader().save(ConfigManager.getWorldConfigLoader().createEmptyNode().setValue(TypeToken.of(WorldsConfig.class), this));
        } catch (IOException | ObjectMappingException ex) {
            Logging.error("Failed to save worlds config file:");
            ex.printStackTrace();
        }
    }

    @Override
    public Map<String, net.swofty.swm.api.world.data.WorldData> getWorlds() {
        return worlds;
    }
}
