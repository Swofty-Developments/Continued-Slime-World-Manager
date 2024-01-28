package net.swofty.swm.clsm;

public interface CLSMBridge {

    // Array containing the normal world, the nether and the end
    Object[] getDefaultWorlds();

    boolean isCustomWorld(Object world);

    default boolean skipWorldAdd(Object world) {
        return false; // If true, the world won't be added to the bukkit world list
    }
}
