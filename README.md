# MCEngine Gradle Plugin

`io.github.mcengine.build-logic` is the convention plugin behind every MCEngine-family
Minecraft plugin build. A plugin's root `build.gradle` applies it instead of copying the
same `allprojects { }` / `subprojects { }` block from repository to repository.

Applied to the root project, it gives each subproject that has a build file of its own
(an intermediate folder such as `platforms` is skipped):

| What | Value |
|---|---|
| `group` / `version` | `project-group` / `project-version` from `gradle.properties` |
| Repositories | `mavenCentral()` on every project |
| Java | the `java` plugin, the Java **25** toolchain, UTF-8 for every `JavaCompile` |
| Publishing | for each module in `publishedModules` (default `api`, `common`, `bukkit`): `maven-publish`, the `GitHubPackages` repository with `GITHUB_ACTOR` / `GITHUB_TOKEN`, `artifactId = ${rootProject.name}-${project.name}`, and a POM `<url>` of `https://github.com/{git-org-name}/{git-repository}` |
| Shadow | `com.gradleup.shadow` **9.4.1** on the build classpath, applied to each module in `shadowModules` (default `engine`) |
| Root `clean` | registered on the root project when the root build declares none |

The plugin classes live in `io.github.mcengine.gradleplugin`
(`MCEngineBuildPlugin`, `MCEngineBuildExtension`).

## Using it: MCStory

MCStory publishes `tools` as well as the usual three modules. These are the only files
that change.

### `settings.gradle`

The plugin is published to GitHub Packages, which needs a token even to read, so
`pluginManagement` names that repository beside the Gradle Plugin Portal (where
`com.gradleup.shadow` itself comes from).

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'MCEngineGradlePlugin'
            url = 'https://maven.pkg.github.com/MCEngine/gradle-plugin'
            credentials {
                username = System.getenv('GITHUB_ACTOR')
                password = System.getenv('GITHUB_TOKEN')
            }
        }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'mcstory'

include 'api'
include 'common'
include 'tools'
include 'bukkit'
include 'platforms:foliamc'
include 'platforms:papermc'
include 'platforms:spigotmc'
include 'platforms:engine'
```

### `build.gradle` (root)

The whole root build. `publishedModules` starts as `['api', 'common', 'bukkit']`;
`add` extends it, `=` replaces it.

```groovy
plugins {
    id 'io.github.mcengine.build-logic' version '0.1.0'
}

mcengineBuild {
    publishedModules.add('tools')
    // or, the same thing spelled out:
    // publishedModules = ['api', 'common', 'bukkit', 'tools']
}
```

### `gradle.properties`

Unchanged — the plugin reads the names every plugin already uses:

```properties
project-group=io.github.mcipos
git-org-name=MCIpos
git-repository=mcstory
project-version=0.2.1
```

### Module build files

* **Drop the version from every `id 'com.gradleup.shadow'`** — in `platforms/engine`
  and in each platform module that shades. The plugin already put Shadow 9.4.1 on the
  build classpath, and Gradle refuses a version for a plugin that is already there:

  ```groovy
  plugins {
      id 'com.gradleup.shadow'
  }
  ```

  `engine` gets Shadow applied whether or not it says so; the `plugins { }` line is only
  needed where a module is not in `shadowModules`.
* A module's own `publishing { publications { mavenJava(MavenPublication) { … } } }`
  keeps working: `maven-publish` is applied before the module's build file runs, and the
  plugin sets the artifactId and POM on every publication.
* A published module that declares no publication — MCStory's `tools` — gets a
  `mavenJava` publication of its `java` component.
* Remove the root-level `allprojects { }`, `subprojects { }` and `clean` task; the plugin
  now owns them.

### Publishing

Nothing changes in CI: `./gradlew publish` publishes `mcstory-api`, `mcstory-common`,
`mcstory-bukkit` and `mcstory-tools` to `https://maven.pkg.github.com/MCIpos/mcstory`.
The shared `MCEngine/actions` Publish workflow already sets `GITHUB_ACTOR` and
`GITHUB_TOKEN` for every step, so the same token resolves this plugin. A local build
needs a token with `read:packages`; in Windows PowerShell 5:

```powershell
$env:GITHUB_ACTOR = "your-login"; $env:GITHUB_TOKEN = "ghp_..."; .\gradlew.bat build
```

## The `mcengineBuild { }` extension

Set values in the root build script. They are read when the first subproject is
configured and are final from then on.

| Property | Type | Default |
|---|---|---|
| `publishedModules` | `List<String>` | `['api', 'common', 'bukkit']` |
| `shadowModules` | `List<String>` | `['engine']` |
| `javaVersion` | `Integer` | `25` |
| `gitOrgName` | `String` | `git-org-name` from `gradle.properties` |
| `gitRepository` | `String` | `git-repository` from `gradle.properties` |
| `pomUrl` | `String` | `https://github.com/{gitOrgName}/{gitRepository}` |
| `developerId` | `String` | `jetsadawijit`; `''` leaves `<developers>` out |
| `developerName` | `String` | `jetsadawijit` |
| `developerUrl` | `String` | `https://jetsadawijit.github.io` |

A module list matches a subproject by name (`engine`) or by full path
(`:platforms:engine`). For example, MCIdentity's POM points at its website:

```groovy
mcengineBuild {
    pomUrl = 'https://mcoriax.github.io/website'
}
```

The build fails with a message naming the missing property when a published module has
no `git-org-name` or `git-repository`, and when the plugin is applied anywhere but the
root project.

## Building this repository

```powershell
.\gradlew.bat build
```

`build` runs the TestKit functional tests in `src/functionalTest`, which build an
MCStory-shaped sample project with the plugin applied. To try a change in a real plugin
before publishing it, point that plugin's `settings.gradle` at a local clone:

```groovy
pluginManagement {
    includeBuild '../gradle-plugin'
}
```

and apply `id 'io.github.mcengine.build-logic'` without a version.

The plugin is published as `io.github.mcengine:gradle-plugin` plus the plugin marker
`io.github.mcengine.build-logic:io.github.mcengine.build-logic.gradle.plugin`, when a
`release/{version}` pull request merges into `master`.
