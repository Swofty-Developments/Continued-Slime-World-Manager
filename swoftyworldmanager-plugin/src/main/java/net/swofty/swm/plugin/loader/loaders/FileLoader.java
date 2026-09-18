package net.swofty.swm.plugin.loader.loaders;

import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.plugin.log.Logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FileLoader implements SlimeLoader {

    private static final String EXTENSION = ".swofty";
    private static final String LEGACY_EXTENSION = ".slime";
    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(EXTENSION) || name.endsWith(LEGACY_EXTENSION);

    private final Map<String, RandomAccessFile> worldFiles = new HashMap<>();
    private final Set<String> legacyWorlds = new HashSet<>();
    private final File worldDir;

    public FileLoader(File worldDir) {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            Logging.warning("A file named '" + worldDir.getName() + "' has been deleted, as this is the name used for the worlds directory.");
            worldDir.delete();
        }

        worldDir.mkdirs();
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        File worldFile = findWorldFile(worldName);

        if (worldFile == null) {
            throw new UnknownWorldException(worldName);
        }

        if (readOnly) {
            try (RandomAccessFile file = new RandomAccessFile(worldFile, "r")) {
                return readWorld(file);
            }
        }

        RandomAccessFile file = worldFiles.computeIfAbsent(worldName, (world) -> {
            try {
                return new RandomAccessFile(worldFile, "rw");
            } catch (FileNotFoundException ex) {
                return null;
            }
        });

        if (file == null) {
            throw new UnknownWorldException(worldName);
        }

        if (isLegacy(worldFile)) {
            legacyWorlds.add(worldName);
        }

        FileChannel channel = file.getChannel();

        try {
            if (channel.tryLock() == null) {
                throw new WorldInUseException(worldName);
            }
        } catch (OverlappingFileLockException ex) {
            throw new WorldInUseException(worldName);
        }

        return readWorld(file);
    }

    private byte[] readWorld(RandomAccessFile file) throws IOException {
        if (file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        byte[] serializedWorld = new byte[(int) file.length()];
        file.seek(0);
        file.readFully(serializedWorld);

        return serializedWorld;
    }

    private File findWorldFile(String worldName) {
        File worldFile = new File(worldDir, worldName + EXTENSION);

        if (worldFile.exists()) {
            return worldFile;
        }

        File legacyFile = new File(worldDir, worldName + LEGACY_EXTENSION);

        return legacyFile.exists() ? legacyFile : null;
    }

    private static boolean isLegacy(File worldFile) {
        return worldFile.getName().endsWith(LEGACY_EXTENSION);
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    @Override
    public boolean worldExists(String worldName) {
        return findWorldFile(worldName) != null;
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        String[] worlds = worldDir.list(WORLD_FILE_FILTER);

        if(worlds == null) {
            throw new NotDirectoryException(worldDir.getPath());
        }

        return Arrays.stream(worlds).map(FileLoader::stripExtension).distinct().collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        RandomAccessFile worldFile = worldFiles.get(worldName);
        boolean tracked = worldFile != null;

        if (tracked && legacyWorlds.remove(worldName)) {
            // The world was loaded from a legacy file, which is left untouched. Move the lock over to the new file.
            worldFile.close();
            worldFile = new RandomAccessFile(new File(worldDir, worldName + EXTENSION), "rw");
            tryLock(worldFile);
            worldFiles.put(worldName, worldFile);
        }

        if (!tracked) {
            worldFile = new RandomAccessFile(new File(worldDir, worldName + EXTENSION), "rw");
        }

        worldFile.seek(0);
        worldFile.setLength(0);
        worldFile.write(serializedWorld);

        if (!tracked) {
            if (lock) {
                tryLock(worldFile);
                worldFiles.put(worldName, worldFile);
            } else {
                worldFile.close();
            }
        }
    }

    private static void tryLock(RandomAccessFile worldFile) throws IOException {
        try {
            worldFile.getChannel().tryLock();
        } catch (OverlappingFileLockException ignored) {

        }
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.remove(worldName);
        legacyWorlds.remove(worldName);

        if (file != null) {
            file.close();
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException {
        RandomAccessFile file = worldFiles.get(worldName);
        boolean closeOnFinish = false;

        if (file == null) {
            File worldFile = findWorldFile(worldName);

            if (worldFile == null) {
                throw new UnknownWorldException(worldName);
            }

            file = new RandomAccessFile(worldFile, "rw");
            closeOnFinish = true;
        }

        FileChannel channel = file.getChannel();

        try {
            FileLock fileLock = channel.tryLock();

            if (fileLock != null) {
                fileLock.release();
                // If we got here, it means we acquired the lock, so the file wasn't locked.
                return false;
            }
            // If we couldn't acquire the lock, it means the file is locked by another process.
        } catch (OverlappingFileLockException ignored) {
            // This exception is thrown if we already hold a lock on this file in this JVM,
            // which means the file is locked (by us).
            return true;
        } finally {
            if (closeOnFinish) {
                file.close();
            }
        }

        // If we couldn't acquire the lock and no exception was thrown,
        // we assume that the file is locked by another process.
        return true;
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        new File(worldDir, worldName + EXTENSION).delete();
        new File(worldDir, worldName + LEGACY_EXTENSION).delete();
    }

    @Override
    public void close() throws IOException {
        for (RandomAccessFile file : worldFiles.values()) {
            file.close();
        }

        worldFiles.clear();
        legacyWorlds.clear();
    }
}
