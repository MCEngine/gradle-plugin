# What the plugin configures

`io.github.mcengine.build-logic` is applied once, to the root project of a plugin build.
It then configures the root and every subproject.

## Every project

| Setting | Value |
|---|---|
| `group` | `project-group` from `gradle.properties`, when set |
| `version` | `project-version` from `gradle.properties`, when set |
| Repositories | `mavenCentral()` |

The root project also gets a `clean` task that deletes its `build/` folder, unless the
root build declares one itself.

## Every subproject with a build file

A subproject without a `build.gradle` (an intermediate folder such as `platforms`) is
left bare. The others get:

* the `java` plugin;
* the Java toolchain, 25 unless [`javaVersion`](extension.md) says otherwise;
* `UTF-8` encoding for every `JavaCompile` task.

## Published modules

A subproject whose name or path is in [`publishedModules`](extension.md) (default `api`,
`common`, `bukkit`) also gets `maven-publish` and the settings in
[Publishing](publishing.md). If it declares no publication of its own — MCStory's
`tools` — the plugin adds `mavenJava` from its `java` component.

## Shaded modules

A subproject whose name or path is in [`shadowModules`](extension.md) (default `engine`)
gets `com.gradleup.shadow` applied. Shadow 9.4.1 is a dependency of the plugin, so it is
on the build classpath for every module; any other module can apply it with
`id 'com.gradleup.shadow'` and no version.

## When it happens

Each subproject is configured just before its own build file runs. By then the root
build script has finished, so its `mcengineBuild { }` values are known, and a module's
build file already sees `java` and `maven-publish` applied and can configure them.

Module lists match a subproject by name (`engine`) or by full path
(`:platforms:engine`).
