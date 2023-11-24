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


[1]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperty.java
[2]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperties.java
[3]: ../../swoftyworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimePropertyMap.java
