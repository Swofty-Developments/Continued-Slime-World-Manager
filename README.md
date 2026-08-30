# Continued Slime World Manager

[<img src="https://discordapp.com/assets/e4923594e694a21542a489471ecffa50.svg" alt="" height="55" />](https://discord.gg/paper)

Continued Slime World Manager is a 1.8.x variant of Slime World Manager which is intended to continue on with the development and maintenance. Its goal is to provide server administrators with an easy-to-use tool to load worlds faster and save space.

#### This only supports 1.8?
Correct, this is at the core of this continuations design. For versions 1.17 and above we highly recommend [Paul19988's Advanced Slime World Manager](https://github.com/Paul19988/Advanced-Slime-World-Manager), which is maintained by in large by Paul, a newly hired Hypixel administrator as of mid 2023.

#### Releases

Releases are auto deployed on push onto the GitHub releases page which can be found [here](https://github.com/Swofty-Developments/Continued-Slime-World-Manager/releases). Updates are also periodically sent within my discord server located at [discord.gg/paper](discord.gg/paper).

## Performance

| World size | Anvil (full load) | Grinderwolf SWM v1 (1.1.4) | SWM v3 (3.1.1) | CSWM v5 (5.0.1) |
|---|---|---|---|---|
| 625 chunks | 434 ms · 3.8 MB | 29 ms · 2.0 MB | 20 ms · 2.0 MB | 17 ms · 1.3 MB |
| 1,024 chunks | 402 ms · 7.4 MB | 41 ms · 4.0 MB | 37 ms · 4.0 MB | 33 ms · 2.6 MB |
| 2,304 chunks | 568 ms · 9.9 MB | 54 ms · 4.5 MB | 55 ms · 4.5 MB | 50 ms · 3.0 MB |
| 4,096 chunks | 1,153 ms · 19.7 MB | 114 ms · 9.8 MB | 114 ms · 9.6 MB | 104 ms · 6.3 MB |

## Using CSWM in your plugin

#### Maven
```  
<dependencies>  
  <dependency>  
    <groupId>net.swofty</groupId>  
    <artifactId>swoftyworldmanager-api</artifactId>  
    <version>INSERT LATEST VERSION HERE</version>  
  </dependency>  
</dependencies>  
```  

#### Gradle
```
dependencies {
    implementation 'net.swofty:swoftyworldmanager-api:INSERT LATEST VERSION HERE'
}
```

#### Javadocs

Javadocs can be found [here](https://swofty-developments.github.io/Continued-Slime-World-Manager/apidocs/).

## Wiki Overview
* Plugin Usage
   * [Installing Continued Slime World Manager](.docs/usage/install.md)
   * [Using Continued Slime World Manager](.docs/usage/using.md)
   * [Commands and permissions](.docs/usage/commands-and-permissions.md)
* Configuration
   * [Setting up the data sources](.docs/config/setup-data-sources.md)
   * [Converting traditional worlds into the SRF](.docs/config/convert-world-to-srf.md)
   * [Configuring worlds](.docs/config/configure-world.md)
   * [Async world generation](.docs/config/async-world-generation.md)
* CSWM API
   * [Getting started](.docs/api/setup-dev.md)
   * [World Properties](.docs/api/properties.md)
   * [Loading a world](.docs/api/load-world.md)
   * [Migrating a world](.docs/api/migrate-world.md)
   * [Importing a world](.docs/api/import-world.md)
   * [Unload a world](.docs/api/unload-world.md)
   * [Events](.docs/api/events.md)
   * [Using other data sources](.docs/api/use-data-source.md)
* [FAQ](.docs/faq.md)

## Credits

Thanks to:
* All the contributors who helped this project by adding features to SWM.
* [Minikloon](https://twitter.com/Minikloon) and all the [Hypixel](https://twitter.com/HypixelNetwork) team for developing the SRF.
* Myself and any other contributors which can be viewed on this Git page.
