# Module kronos-maven-plugin

Connects the Kronos compiler plugin to Kotlin compilation in Maven projects.

## Usage

Configure `kotlin-maven-plugin` in the project's `<build><plugins>` section:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>2.4.21</version>
    <extensions>true</extensions>
    <configuration>
        <args>
            <arg>-Xcollection-literals</arg>
            <arg>-Xcontext-sensitive-resolution</arg>
        </args>
        <compilerPlugins>
            <plugin>kronos-maven-plugin</plugin>
        </compilerPlugins>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-maven-plugin</artifactId>
            <version>0.4.0</version>
        </dependency>
    </dependencies>
</plugin>
```

Collection literals enable DSL forms such as `select { [it.id, it.name] }`.

## How It Works

The Plexus component `kronos-maven-plugin` implements `KotlinMavenPluginExtension`.
Its published POM includes `kronos-compiler-plugin` as a transitive dependency, and its
`META-INF/services` descriptors expose `CompilerPluginRegistrar` and `CommandLineProcessor`.

For each Maven compilation, the extension derives a generated-provider identity from the
project group, artifact and execution ID, passes the matching compiler options, and writes
`META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider` to the main or test output
directory. Empty source compilations remove the corresponding stale service descriptor.
