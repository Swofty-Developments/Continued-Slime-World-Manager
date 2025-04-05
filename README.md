![](https://cdn.discordapp.com/attachments/1122145806066126899/1162312766242160640/image.png?ex=6686639e&is=6685121e&hm=4cb2d8c11923a30fd976746fd60eb6332639befc3ea0b2cbf4d224e5c089470a&)
# Continued Slime World Manager

[<img src="https://discordapp.com/assets/e4923594e694a21542a489471ecffa50.svg" alt="" height="55" />](https://discord.gg/paper)

Continued Slime World Manager is a 1.8.x variant of Slime World Manager which is intended to continue on with the development and maintenance. Its goal is to provide server administrators with an easy-to-use tool to load worlds faster and save space.

#### This only supports 1.8?
Correct, this is at the core of this continuations design. For versions 1.17 and above we highly recommend [Paul19988's Advanced Slime World Manager](https://github.com/Paul19988/Advanced-Slime-World-Manager), which is maintained by in large by Paul, a newly hired Hypixel administrator as of mid 2023.

#### Releases

Releases are auto deployed on push onto the GitHub releases page which can be found [here](https://github.com/Swofty-Developments/Continued-Slime-World-Manager/releases). Updates are also periodically sent within my discord server located at [discord.gg/paper](discord.gg/paper).

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
