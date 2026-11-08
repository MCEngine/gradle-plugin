# Migrating a plugin

Moves an existing plugin from its copied root `build.gradle` to the convention plugin
without changing what it builds or publishes.

## Before

Every plugin's root `build.gradle` carries the same blocks: `allprojects { }` setting
`group`, `version` and `mavenCentral()`; `subprojects { }` applying `java`, the Java 25
toolchain, and — for `api`, `common` and `bukkit` — `maven-publish` with the
`GitHubPackages` repository, the `${rootProject.name}-${project.name}` artifactId and the
POM; and a root `clean` task.

## Steps

1. Add the plugin repository to `settings.gradle` —
   [Applying the plugin](applying-the-plugin.md), step 1.
2. Replace the root `build.gradle` with the `plugins { }` block and an
   `mcengineBuild { }` block for anything that differs from the defaults:

   | The old root build… | Becomes |
   |---|---|
   | publishes `api`, `common`, `bukkit` | nothing — the default |
   | also publishes `tools` (MCAgents, MCStory) | `publishedModules.add('tools')` |
   | has a POM `url` other than the GitHub repository (MCIdentity's website) | `pomUrl = 'https://mcoriax.github.io/website'` |

3. Remove `version '9.4.1'` from every `id 'com.gradleup.shadow'` in the module builds.
4. Leave the module builds otherwise as they are. A module's own
   `publishing { publications { mavenJava(MavenPublication) { … } } }` keeps working,
   because `maven-publish` is applied before the module's build file runs.
5. Leave `gradle.properties` and the workflows as they are.

## Check the result

```powershell
.\gradlew.bat build; .\gradlew.bat publishToMavenLocal
```

The build passes, the engine jar is in `platforms\engine\build\libs\`, and
`~\.m2\repository\{group path}\` holds one folder per published module. For MCStory:
`mcstory-api`, `mcstory-common`, `mcstory-bukkit` and `mcstory-tools`, each POM with
`<url>https://github.com/MCIpos/mcstory</url>`.

To try an unreleased change to the plugin, replace the GitHub Packages repository in
`settings.gradle` with a local clone and drop the version from the root `plugins { }`
block:

```groovy
pluginManagement {
    includeBuild '../gradle-plugin'
}
```

## What can go wrong

| Message | Cause |
|---|---|
| `Plugin [id: 'io.github.mcengine.build-logic', version: '0.1.0'] was not found` | `settings.gradle` has no plugin repository, or the token cannot read the package. |
| `the plugin is already on the classpath with an unknown version` | A module still names a version for `com.gradleup.shadow`. |
| `Could not find method publishing()` in a module | The module is not in `publishedModules` but its build file declares a publication. Add it to the list. |
| `publishes to GitHub Packages but has no git-org-name` | `gradle.properties` lacks `git-org-name` or `git-repository`. |
| `apply it to the root project, not :api` | The plugin was applied in a module build. Apply it once, in the root. |
