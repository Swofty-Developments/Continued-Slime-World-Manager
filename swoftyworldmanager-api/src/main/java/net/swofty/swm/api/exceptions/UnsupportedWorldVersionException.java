package net.swofty.swm.api.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a world was saved from a
 * Minecraft version whose chunk format cannot be
 * loaded on 1.8.8.
 */
@Getter
public class UnsupportedWorldVersionException extends NewerFormatException {

    private final byte worldVersion;

    public UnsupportedWorldVersionException(String world, byte worldVersion) {
        super("World " + world + " was saved from Minecraft 1.13 or newer (world version " + worldVersion + ") and cannot be loaded on 1.8.8");
        this.worldVersion = worldVersion;
    }
}
