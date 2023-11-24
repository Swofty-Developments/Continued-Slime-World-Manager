package net.swofty.swm.plugin.loader.loaders;

import net.swofty.swm.api.exceptions.UnknownWorldException;
import net.swofty.swm.api.exceptions.WorldInUseException;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.plugin.SWMPlugin;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".swofty");

    private final Map<String, RandomAccessFile> worldFiles = new HashMap<>();
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
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.computeIfAbsent(worldName, (world) -> {
            try {
                return new RandomAccessFile(new File(worldDir, worldName + ".swofty"), "rw");
            } catch (FileNotFoundException ex) {
                return null; // This is never going to happen as we've just checked if the world exists
            }
        });

        if (!readOnly) {
            FileChannel channel = file.getChannel();

            try {
                if (channel.tryLock() == null) {
                    throw new WorldInUseException(worldName);
                }
            } catch (OverlappingFileLockException ex) {
                throw new WorldInUseException(worldName);
            }
        }

        if (file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        byte[] serializedWorld = new byte[(int) file.length()];
        file.seek(0); // Make sure we're at the start of the file
        file.readFully(serializedWorld);

        return serializedWorld;
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(worldDir, worldName + ".swofty").exists();
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        String[] worlds = worldDir.list(WORLD_FILE_FILTER);

        if(worlds == null) {
            throw new NotDirectoryException(worldDir.getPath());
        }

        return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        RandomAccessFile worldFile = worldFiles.get(worldName);
        boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(worldDir, worldName + ".swofty"), "rw");
        }

        worldFile.seek(0); // Make sure we're at the start of the file
        worldFile.setLength(0); // Delete old data
        worldFile.write(serializedWorld);

        if (lock) {
            FileChannel channel = worldFile.getChannel();

            try {
                channel.tryLock();
            } catch (OverlappingFileLockException ignored) {

            }
        }

        if (tempFile) {
            worldFile.close();
        }
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.remove(worldName);

        if (file != null) {
            file.close();
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException {
        RandomAccessFile file = worldFiles.get(worldName);
        boolean closeOnFinish = false;

        if (file == null) {
            file = new RandomAccessFile(new File(worldDir, worldName + ".swofty"), "rw");
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

        new File(worldDir, worldName + ".swofty").delete();
    }
}
