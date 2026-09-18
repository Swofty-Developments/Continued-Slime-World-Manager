package net.swofty.swm.plugin.loader.loaders;

import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.plugin.loader.LegacyFormatTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class FileLoaderTest {

    private static final byte[] MODERN = { 1, 2, 3 };
    private static final byte[] REWRITTEN = { 4, 5, 6, 7 };

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File worldDir;
    private byte[] legacy;
    private FileLoader loader;

    @Before
    public void setUp() throws IOException {
        worldDir = folder.newFolder("slime_worlds");
        legacy = LegacyFormatTest.fixture("limbo-v9.slime");

        Files.write(file("legacy.slime").toPath(), legacy);
        Files.write(file("modern.swofty").toPath(), MODERN);

        loader = new FileLoader(worldDir);
    }

    @Test
    public void listsAndFindsLegacyFiles() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList("legacy", "modern")), new HashSet<>(loader.listWorlds()));
        assertTrue(loader.worldExists("legacy"));
        assertTrue(loader.worldExists("modern"));
        assertFalse(loader.worldExists("missing"));
    }

    @Test
    public void loadsLegacyFiles() throws Exception {
        assertArrayEquals(legacy, loader.loadWorld("legacy", true));
        assertArrayEquals(legacy, loader.loadWorld("legacy", false));
        assertTrue(loader.isWorldLocked("legacy"));
    }

    @Test
    public void prefersTheCurrentExtensionWhenBothExist() throws Exception {
        Files.write(file("legacy.swofty").toPath(), MODERN);

        assertEquals(new HashSet<>(Arrays.asList("legacy", "modern")), new HashSet<>(loader.listWorlds()));
        assertArrayEquals(MODERN, loader.loadWorld("legacy", true));
    }

    @Test
    public void savingRewritesUnderTheCurrentExtensionAndKeepsTheLegacyFile() throws Exception {
        loader.loadWorld("legacy", false);
        loader.saveWorld("legacy", REWRITTEN, true);

        assertArrayEquals(legacy, Files.readAllBytes(file("legacy.slime").toPath()));
        assertArrayEquals(REWRITTEN, Files.readAllBytes(file("legacy.swofty").toPath()));
        assertArrayEquals(REWRITTEN, loader.loadWorld("legacy", true));
        assertTrue(loader.isWorldLocked("legacy"));
        assertTrue(isUnlocked(file("legacy.slime")));

        try {
            loader.loadWorld("legacy", false);
            fail();
        } catch (WorldInUseException ignored) {
        }

        loader.unlockWorld("legacy");

        assertFalse(loader.isWorldLocked("legacy"));
        assertArrayEquals(REWRITTEN, loader.loadWorld("legacy", false));
    }

    @Test
    public void savingWithoutALockKeepsTheLockHeldWhileLoaded() throws Exception {
        loader.loadWorld("legacy", false);
        loader.saveWorld("legacy", REWRITTEN, false);

        assertTrue(loader.isWorldLocked("legacy"));
        assertArrayEquals(REWRITTEN, Files.readAllBytes(file("legacy.swofty").toPath()));
    }

    @Test
    public void deletesBothFiles() throws Exception {
        Files.write(file("legacy.swofty").toPath(), MODERN);
        loader.deleteWorld("legacy");

        assertFalse(file("legacy.slime").exists());
        assertFalse(file("legacy.swofty").exists());
        assertFalse(loader.worldExists("legacy"));
    }

    @Test(expected = UnknownWorldException.class)
    public void unknownWorldsAreReported() throws Exception {
        loader.loadWorld("missing", false);
    }

    private File file(String name) {
        return new File(worldDir, name);
    }

    private static boolean isUnlocked(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            return raf.getChannel().tryLock() != null;
        }
    }
}
