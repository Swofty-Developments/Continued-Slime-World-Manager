## Building custom source

### Building

To build SWM, execute the following command int the project root:

```
mvn clean install
```

## Using the API

If your plugin wants to use Slime World Manager add the following in your pom.xml

### Maven
```xml
<dependency>
    <groupId>net.swofty</groupId>
    <artifactId>swoftyworldmanager-api</artifactId>
    <version>(insert latest version here)</version>
    <scope>provided</scope>
</dependency>
```