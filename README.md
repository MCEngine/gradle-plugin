# MCEngine Gradle Plugin

`io.github.mcengine.build-logic` is the convention plugin behind every MCEngine-family
Minecraft plugin build. A plugin's root `build.gradle` applies it instead of copying the
same `allprojects { }` / `subprojects { }` block between repositories: the Java 25
toolchain, UTF-8, `mavenCentral()`, GitHub Packages publishing of the modules it names,
and Shadow 9.4.1 for the engine jar.

```groovy
plugins {
    id 'io.github.mcengine.build-logic' version '0.1.0'
}

mcengineBuild {
    publishedModules.add('tools')
}
```

## Documentation

- [Applying the plugin](wiki/guides/applying-the-plugin.md),
  [migrating a plugin](wiki/guides/migrating-a-plugin.md) and
  [developing the plugin](wiki/guides/developing.md)
- [What it configures](wiki/information/conventions.md),
  [the `mcengineBuild { }` extension](wiki/information/extension.md) and
  [publishing](wiki/information/publishing.md)
- [Release notes](wiki/logs)
