# Module kronos-maven-plugin

Maven plugin that wires the Kronos compiler plugin into Kotlin compilation for Maven-based projects.

## Usage

Add to `kotlin-maven-plugin` configuration:
```xml
<compilerPlugins>
    <plugin>kronos-maven-plugin</plugin>
</compilerPlugins>
<dependencies>
    <dependency>
        <groupId>com.kotlinorm</groupId>
        <artifactId>kronos-maven-plugin</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## How It Works

Implements `KotlinMavenPluginExtension`. Bundles the `kronos-compiler-plugin` artifact and copies its `META-INF/services` so Maven discovers the `ComponentRegistrar`.
