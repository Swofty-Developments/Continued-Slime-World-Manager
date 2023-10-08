### Releases

SWM releases can be found [here](https://github.com/Swofty-Developments/Continued-Slime-World-Manager/releases).


### How to install SWM

Installing SWM is an easy task. First, download the latest version from the GitHub [releases page](https://github.com/Swofty-Developments/Continued-Slime-World-Manager/releases). Then, follow this step:
1. Place the downloaded `slimeworldmanager-plugin-<version>.jar` file inside your server's plugin folder.
2. Place the `slimeworldmanager-classmodifier-<version>.jar` file inside your server's main directory **(not the plugins folder)**.
3. Modify your server startup command and at this argument before '-jar':
```
-javaagent:slimeworldmanager-classmodifier-<version>.jar
```

That's it! Easy, right?
