# Developing the plugin

Building, testing and releasing `MCEngine/gradle-plugin` itself.

## Build and test

```powershell
.\gradlew.bat build
```

`build` compiles the Groovy sources in `src/main/groovy` and runs the TestKit
functional tests in `src/functionalTest`. Each test writes an MCStory-shaped sample
build (`api`, `common`, `tools`, `bukkit`, `platforms:spigotmc`, `platforms:engine`) to a
temporary folder, applies the plugin, and reads back what each module ended up with.

The tests run on any JDK Gradle 9 supports. Shadow resolves a module's Java compiler
while it is configured, so tests that apply Shadow pin the toolchain to the JVM running
them; only the test that checks the default leaves it at 25.

## Layout

| Path | Holds |
|---|---|
| `build.gradle` | The plugin build: `java-gradle-plugin`, Groovy, Java 17 bytecode, Shadow 9.4.1 as a dependency, publishing. |
| `src/main/groovy/io/github/mcengine/gradleplugin/` | `MCEngineBuildPlugin` and `MCEngineBuildExtension`. |
| `src/functionalTest/groovy/` | The TestKit tests. |
| `gradle.properties` | `project-group`, `git-org-name`, `git-repository`, `project-version`. |

The plugin is compiled for Java 17 because it runs inside the consumer's Gradle daemon,
not on the Java 25 toolchain it configures.

## Release

A release ships only when a `release/{version}` pull request that sets
`project-version` merges into `master`. The Publish workflow then builds, runs the
tests, publishes the jar and plugin marker to GitHub Packages and tags `v{version}`;
Release attaches `build/libs/gradle-plugin-{version}.jar` to a GitHub release with
`wiki/logs/{Major}/{Minor}/{Patch}/CHANGELOG.md` as the notes. Both are thin callers of
the `MCEngine/actions` workflows.
