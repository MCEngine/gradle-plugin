# Applying the plugin

How a plugin repository applies `io.github.mcengine.build-logic`. MCStory is the worked
example; every MCEngine-family plugin has the same layout (`api`, `common`, `tools`,
`bukkit`, `platforms/*`).

## 1. Add the plugin repository to `settings.gradle`

The plugin is published to GitHub Packages, which needs a token even to read. Name that
repository in `pluginManagement`, beside the Gradle Plugin Portal (where Shadow's own
dependencies come from):

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

`pluginManagement { }` must be the first block in the file.

## 2. Replace the root `build.gradle`

The whole root build:

```groovy
plugins {
    id 'io.github.mcengine.build-logic' version '0.1.0'
}

mcengineBuild {
    publishedModules.add('tools')
}
```

`publishedModules` starts as `['api', 'common', 'bukkit']`. `add` extends it; `=`
replaces it (`publishedModules = ['api', 'common', 'bukkit', 'tools']` is the same
thing). Every other setting is in [the extension reference](../information/extension.md).

## 3. Keep `gradle.properties`

The plugin reads the names every plugin already has:

```properties
project-group=io.github.mcipos
git-org-name=MCIpos
git-repository=mcstory
project-version=0.2.1
```

## 4. Drop the Shadow version from module builds

The plugin puts Shadow 9.4.1 on the build classpath, and Gradle refuses a version for a
plugin that is already there (*"the plugin is already on the classpath with an unknown
version"*). In `platforms/engine` and every platform module that shades:

```groovy
plugins {
    id 'com.gradleup.shadow'
}
```

## 5. Give the token access

* **CI.** The shared `MCEngine/actions` Publish and Release workflows set
  `GITHUB_ACTOR` and `GITHUB_TOKEN` for every step. `MCEngine/gradle-plugin` is
  public, so any repository's workflow token can read the plugin package.
* **Local builds.** Use a personal token with `read:packages`. In Windows
  PowerShell 5:

  ```powershell
  $env:GITHUB_ACTOR = "your-login"; $env:GITHUB_TOKEN = "ghp_..."; .\gradlew.bat build
  ```

Moving an existing plugin over step by step, and checking the result, is in
[Migrating a plugin](migrating-a-plugin.md).
