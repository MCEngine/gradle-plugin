---
name: gradle-convention-plugin
description: Task record for MCEngine/gradle-plugin 0.1.0 — the io.github.mcengine.build-logic convention plugin that replaces each plugin's duplicated root build.gradle.
---

# Gradle convention plugin

**Status:** in progress

## Goal

Move the root `build.gradle` every MCEngine-family plugin copies (MCIdentity, MCStory,
...) into one convention plugin, `io.github.mcengine.build-logic`, so a plugin's root
build applies it instead of carrying the logic.

## Objective

* Every subproject with a build file gets `java`, the Java 25 toolchain, UTF-8
  `JavaCompile`, `mavenCentral()`, and `group`/`version` from `project-group` and
  `project-version`.
* `mcengineBuild { publishedModules = [...] }` (default `api`, `common`, `bukkit`)
  chooses the modules that get `maven-publish`, the `GitHubPackages` repository
  (`GITHUB_ACTOR`/`GITHUB_TOKEN`), `artifactId = ${rootProject.name}-${project.name}`
  and a POM URL built from `git-org-name` and `git-repository`.
* `com.gradleup.shadow` 9.4.1 is on the plugin's classpath and applied to
  `shadowModules` (default `engine`).
* Classes live in `io.github.mcengine.gradleplugin`.
* TestKit functional tests cover the above; `./gradlew build` passes.
* The README shows MCStory applying it with `tools` published.
* Publish and Release green on 0.1.0.

## Decisions

The owner was away and asked for the recommended option at every fork.

* **Groovy** with `@CompileStatic`, compiled for Java 17 so any Gradle 9 daemon loads it.
* **Coordinates:** group `io.github.mcengine`, artifact `gradle-plugin`; plugin id
  `io.github.mcengine.build-logic`.
* **Timing:** publishing is configured in each subproject's `beforeEvaluate`, after the
  root script set `mcengineBuild { }` and before the module's own build file runs.
* **Shadow:** exposed and auto-applied; a consumer drops the version from its own
  `id 'com.gradleup.shadow'`.
* **Scope:** only this repository changes; no plugin is migrated.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Plan and task record | `plan/gradle-convention-plugin` | |
| 2 | Convention plugin, build, functional tests | `feat/convention-plugin` | |
| 3 | README usage guide | `docs/readme` | |
| 4 | Publish and Release workflow callers | `ci/workflows` | |
| 5 | Release 0.1.0 | `release/0.1.0` | |

## Progress

### Task 1 — plan/gradle-convention-plugin

Added `.gitignore` (keeps `.agents/plans/` out of history), the memory index and this
record.

### Task 2 — feat/convention-plugin

Added the Gradle 9.5.0 wrapper, `build.gradle` (`java-gradle-plugin` + Groovy, Java 17
bytecode, `shadow-gradle-plugin:9.4.1` as `implementation`, GitHub Packages publishing of
the jar and plugin marker), `MCEngineBuildPlugin`, `MCEngineBuildExtension`, and ten
TestKit tests in `src/functionalTest`.

* The module lists are set, not conventions, so `publishedModules.add('tools')` extends
  the default; with a convention, `add` dropped `api`, `common` and `bukkit`.
* Shadow resolves a module's Java compiler while configuring, so tests pin the toolchain
  to the running JVM except where they only read the default.
* A copy of MCStory 0.2.1 with its root `build.gradle` replaced by the plugin and the
  shadow versions removed built, tested and ran `publishToMavenLocal` for
  `mcstory-{api,common,bukkit,tools}` with the configuration cache on.
* Consumers must drop `version '9.4.1'` from every `id 'com.gradleup.shadow'`: Gradle
  refuses a version for a plugin already on the classpath.

### Task 3 — docs/readme

Rewrote `README.md`: what the plugin configures, MCStory's `settings.gradle`, root
`build.gradle` (`publishedModules.add('tools')`), the shadow-version change, the
extension reference, and how to build and try it locally.
