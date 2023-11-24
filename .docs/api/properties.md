## Properties

Property "types" are handled by [SlimeProperty][1] instances. Whilst not allowing to create [SlimeProperty][1] objects, there is a [list of all available properties][2]. Properties and their values are stored in [SlimePropertyMaps][3].


**Example Usage:**
```java
// Create a new and empty property map
SlimePropertyMap properties = new SlimePropertyMap();

properties.setValue(SlimeProperties.DIFFICULTY, "normal");
properties.setValue(SlimeProperties.SPAWN_X, 123);
properties.setValue(SlimeProperties.SPAWN_Y, 112);
properties.setValue(SlimeProperties.SPAWN_Z, 170);
/* Add as many as you like */
```

Properties can be modified after-the-fact aswell assuming it's loaded in the `worlds.yml` file.
```java
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SwoftyWorldManager");
World world = // your world here
WorldsConfig config = plugin.getConfigManager().getWorldConfig();
WorldData worldData = config.getWorlds().get(world.getName());

// Other properties can also be changed, this example is just using the spawn property as an example
worldData.setSpawn(sender.getPlayer().getLocation().getBlockX() + ", " + sender.getPlayer().getLocation().getBlockY() + ", " + sender.getPlayer().getLocation().getBlockZ());

// Save config after the fact
config.save();
```


[1]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperty.java
[2]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperties.java
[3]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimePropertyMap.java
