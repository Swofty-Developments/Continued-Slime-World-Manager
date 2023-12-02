
## FAQ

* Which Spigot versions is this compatible with?

Currently, SWM can run on any Spigot version from 1.8.8

* Can I override the default world?

Yes, you can! However, that requires doing some extra steps. Take a look at the [Installing Slime World Manager](usage/install.md) page.

* Is SWM compatible with Multiverse-Core?

Multiverse-Core detects SWM worlds as unloaded, as it cannot find the world directory, and then just ignores them. Although there should be no issues, MV commands won't work with SWM worlds.

* It appears that my reflections aren't working after installing, what happened?

Follow the instructions found on this Spigot post in regards to relocation; https://www.spigotmc.org/threads/issue-with-reflections-library-and-multiple-plugins.397903/

* What's the world size limit?

The Slime Region Format can handle up a 46340x4630 chunk area. That's the maximum size that SWM can _theoretically_ handle, given enough memory. However, having a world so big is not recommended at all.

There's not an specific value that you shouldn't exceed _- except for the theoretical limit, of course_. SWM keeps a copy of all the chunks loaded in memory until the world is unloaded, so the more chunks you have, the bigger the ram usage is. How far you want to go depends on how much ram you are willing to let SWM use. Moreover, the ram usage per chunk isn't a constant value, as it depends on the actual data stored in the chunk.
