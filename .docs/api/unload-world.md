## Unloading a world

Given that you have your SlimeWorld object, you can unload it using the following method:
```java
slimeworld.unloadWorld(true);
```
The boolean parameter indicates whether the world should be saved or not. If you want to save the world, set it to true. Otherwise, set it to false.

There is also an optional second parameter that allows you to manually set a 'fallback' world. This is the world that players will be teleported to if they are in the world that is being unloaded. If you don't set this parameter, the fallback world will be the default world.

```java
slimeworld.unloadWorld(true, "world_the_end");
```
