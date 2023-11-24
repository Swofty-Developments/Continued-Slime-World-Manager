package net.swofty.swm.plugin;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import net.swofty.swm.api.SlimePlugin;
import net.swofty.swm.api.events.PostGenerateWorldEvent;
import net.swofty.swm.api.events.PreGenerateWorldEvent;
import net.swofty.swm.api.exceptions.*;
import net.swofty.swm.api.loaders.SlimeLoader;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.data.WorldData;
import net.swofty.swm.api.world.data.WorldsConfig;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import net.swofty.swm.nms.SlimeNMS;
import net.swofty.swm.plugin.commands.CommandLoader;
import net.swofty.swm.plugin.commands.SWMCommand;
import net.swofty.swm.plugin.loader.LoaderUtils;
import net.swofty.swm.plugin.log.Logging;
import net.swofty.swm.plugin.world.WorldUnlocker;
import net.swofty.swm.plugin.world.importer.WorldImporter;
import lombok.Getter;
import net.swofty.swm.plugin.config.ConfigManager;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.*;
import org.bukkit.command.CommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Getter
public class SWMPlugin extends JavaPlugin implements SlimePlugin {

    @Getter
    private static SWMPlugin instance;
    private SlimeNMS nms;
    private CommandLoader cl;

    public CommandMap commandMap;

    private final List<SlimeWorld> toGenerate = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService worldGeneratorService = Executors.newFixedThreadPool(1);

    @Override
    public void onLoad() {
        /*
            Static abuse?!?! Nah I'm meming, we all know this is the easiest way to do it
            Credit: @swofty
         */
        instance = this;

        /*
            Initialize config files
         */
        try {
            ConfigManager.initialize();
        } catch (NullPointerException | IOException | ObjectMappingException ex) {
            Logging.error("Failed to load config files:");
            ex.printStackTrace();
            return;
        }
        LoaderUtils.registerLoaders();

        /*
            Initialize NMS bridge
         */
        try {
            nms = getNMSBridge();
        } catch (InvalidVersionException ex) {
            Logging.error(ex.getMessage());
            return;
        }

        /*
            Initialize commands
         */
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            commandMap = (CommandMap) f.get(Bukkit.getServer());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        cl = new CommandLoader();
        SWMCommand.register();

        Reflections reflection = new Reflections("net.swofty.swm.plugin.commands.subtypes");
        for(Class<? extends SWMCommand> l:reflection.getSubTypesOf(SWMCommand.class)) {
            try {
                SWMCommand command = l.newInstance();
                cl.register(command);
            } catch (InstantiationException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }

        /*
            Load worlds
         */
        List<String> erroredWorlds = loadWorlds();
        try {
            Properties props = new Properties();

            props.load(new FileInputStream("server.properties"));
            String defaultWorldName = props.getProperty("level-name");

            if (erroredWorlds.contains(defaultWorldName)) {
                Logging.error("Shutting down server, as the default world could not be loaded.");
                System.exit(1);
            } else if (getServer().getAllowNether() && erroredWorlds.contains(defaultWorldName + "_nether")) {
                Logging.error("Shutting down server, as the default nether world could not be loaded.");
                System.exit(1);
            } else if (getServer().getAllowEnd() && erroredWorlds.contains(defaultWorldName + "_the_end")) {
                Logging.error("Shutting down server, as the default end world could not be loaded.");
                System.exit(1);
            }

            Map<String, SlimeWorld> loadedWorlds = getSlimeWorlds();

            SlimeWorld defaultWorld = loadedWorlds.get(defaultWorldName);
            SlimeWorld netherWorld = getServer().getAllowNether() ? loadedWorlds.get(defaultWorldName + "_nether") : null;
            SlimeWorld endWorld = getServer().getAllowEnd() ? loadedWorlds.get(defaultWorldName + "_the_end") : null;

            nms.setDefaultWorlds(defaultWorld, netherWorld, endWorld);
        } catch (IOException ex) {
            Logging.error("Failed to retrieve default world name:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (nms == null) {
            this.setEnabled(false);
            return;
        }

        getServer().getPluginManager().registerEvents(new WorldUnlocker(), this);

        toGenerate.forEach(this::generateWorld);
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().stream()
                .map(world -> getNms().getSlimeWorld(world))
                .filter(Objects::nonNull)
                .forEach(world -> {
                    world.unloadWorld(true);
                    try {
                        world.getLoader().unlockWorld(world.getName());
                    } catch (UnknownWorldException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private SlimeNMS getNMSBridge() throws InvalidVersionException {
        return new SlimeNMS();
    }

    private List<String> loadWorlds() {
        List<String> erroredWorlds = new ArrayList<>();
        WorldsConfig config = getConfigManager().getWorldConfig();

        for (Map.Entry<String, WorldData> entry : config.getWorlds().entrySet()) {
            String worldName = entry.getKey();
            WorldData worldData = entry.getValue();

            if (worldData.isLoadOnStartup()) {
                try {
                    SlimeLoader loader = getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource() + "");
                    }

                    SlimePropertyMap propertyMap = worldData.toPropertyMap();

                    SlimeWorld world = loadWorld(loader, worldName, worldData.isReadOnly(), propertyMap);
                    toGenerate.add(world);
                } catch (IllegalArgumentException | UnknownWorldException | NewerFormatException |
                         WorldInUseException |
                         CorruptedWorldException | IOException ex) {
                    String message;

                    if (ex instanceof IllegalArgumentException) {
                        message = ex.getMessage();
                    } else if (ex instanceof UnknownWorldException) {
                        message = "world does not exist, are you sure you've set the correct data source?";
                    } else if (ex instanceof NewerFormatException) {
                        message = "world is serialized in a newer Slime Format version (" + ex.getMessage() + ") that SWM does not understand.";
                    } else if (ex instanceof WorldInUseException) {
                        message = "world is in use! If you think this is a mistake, please wait some time and try again.";
                    } else if (ex instanceof CorruptedWorldException) {
                        message = "world seems to be corrupted.";
                    } else {
                        message = "";

                        ex.printStackTrace();
                    }

                    Logging.error("Failed to load world " + worldName + (message.isEmpty() ? "." : ": " + message));
                    erroredWorlds.add(worldName);
                }
            }
        }

        config.save();
        return erroredWorlds;
    }

    @Override
    public net.swofty.swm.api.world.data.ConfigManager getConfigManager() {
        return new ConfigManager();
    }

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws UnknownWorldException, IOException,
            CorruptedWorldException, NewerFormatException, WorldInUseException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "Properties cannot be null");

        long start = System.currentTimeMillis();

        Logging.info("Loading world " + worldName + ".");
        byte[] serializedWorld = loader.loadWorld(worldName, readOnly);
        CraftSlimeWorld world;

        try {
            world = LoaderUtils.deserializeWorld(loader, worldName, serializedWorld, propertyMap, readOnly);
        } catch (Exception ex) {
            if (!readOnly) { // Unlock the world as we're not using it
                loader.unlockWorld(worldName);
            }
            throw ex;
        }

        Logging.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms.");

        return world;
    }

    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws WorldAlreadyExistsException, IOException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "Properties cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        Logging.info("Creating empty world " + worldName + ".");
        long start = System.currentTimeMillis();
        CraftSlimeWorld world = new CraftSlimeWorld(loader, worldName, new HashMap<>(), new CompoundTag("",
                new CompoundMap()), new ArrayList<>(), propertyMap, readOnly, !readOnly);
        loader.saveWorld(worldName, world.serialize(), !readOnly);

        Logging.info("World " + worldName + " created (in-memory) in " + (System.currentTimeMillis() - start) + "ms.");

        return world;
    }

    @Override
    public CompletableFuture<Void> generateWorld(SlimeWorld world) {
        Bukkit.getPluginManager().callEvent(new PreGenerateWorldEvent(world));
        Objects.requireNonNull(world, "SlimeWorld cannot be null");

        if (!world.isReadOnly() && !world.isLocked()) {
            throw new IllegalArgumentException("This world cannot be loaded, as it has not been locked.");
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        /*
        Async World Generation
         */
        worldGeneratorService.submit(() -> {
            Object nmsWorld = nms.createNMSWorld(world);
            Bukkit.getScheduler().runTask(this, () -> {
                nms.addWorldToServerList(nmsWorld);
                Bukkit.getPluginManager().callEvent(new PostGenerateWorldEvent(world));
                future.complete(null);
            });
        });

        return future;
    }

    @Override
    public void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException,
            WorldInUseException, WorldAlreadyExistsException, UnknownWorldException {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(currentLoader, "Current loader cannot be null");
        Objects.requireNonNull(newLoader, "New loader cannot be null");

        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldName);

        boolean leaveLock = false;

        if (bukkitWorld != null) {
            // Make sure the loaded world really is a SlimeWorld and not a normal Bukkit world
            CraftSlimeWorld slimeWorld = (CraftSlimeWorld) SWMPlugin.getInstance().getNms().getSlimeWorld(bukkitWorld);

            if (slimeWorld != null && currentLoader.equals(slimeWorld.getLoader())) {
                slimeWorld.setLoader(newLoader);

                if (!slimeWorld.isReadOnly()) { // We have to manually unlock the world so no WorldInUseException is thrown
                    currentLoader.unlockWorld(worldName);
                    leaveLock = true;
                }
            }
        }

        byte[] serializedWorld = currentLoader.loadWorld(worldName, false);

        newLoader.saveWorld(worldName, serializedWorld, leaveLock);
        currentLoader.deleteWorld(worldName);
    }

    @Override
    public SlimeLoader getLoader(String dataSource) {
        Objects.requireNonNull(dataSource, "Data source cannot be null");

        return LoaderUtils.getLoader(dataSource);
    }

    @Override
    public void registerLoader(String dataSource, SlimeLoader loader) {
        Objects.requireNonNull(dataSource, "Data source cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        LoaderUtils.registerLoader(dataSource, loader);
    }

    @Override
    public void importWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException,
            InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        Objects.requireNonNull(worldDir, "World directory cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldDir.getName());

        if (bukkitWorld != null && nms.getSlimeWorld(bukkitWorld) == null) {
            throw new WorldLoadedException(worldDir.getName());
        }

        CraftSlimeWorld world = WorldImporter.readFromDirectory(worldDir);

        byte[] serializedWorld;

        try {
            serializedWorld = world.serialize();
        } catch (IndexOutOfBoundsException ex) {
            throw new WorldTooBigException(worldDir.getName());
        }

        loader.saveWorld(worldName, serializedWorld, false);
    }

    @Override
    public Map<String, SlimeWorld> getSlimeWorlds() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world != null && SWMPlugin.getInstance().getNms().getSlimeWorld(world) != null)
                .collect(Collectors.toMap(
                        World::getName,
                        world -> SWMPlugin.getInstance().getNms().getSlimeWorld(world)
                ));
    }
}
