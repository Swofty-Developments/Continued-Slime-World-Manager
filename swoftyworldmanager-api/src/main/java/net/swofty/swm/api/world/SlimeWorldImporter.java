package net.swofty.swm.api.world;

import net.swofty.swm.api.exceptions.InvalidWorldException;

import java.io.File;

public interface SlimeWorldImporter {
    SlimeWorld readFromDirectory(File worldDir) throws InvalidWorldException;
}
