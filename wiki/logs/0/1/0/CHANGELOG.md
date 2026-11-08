# Changelog — 0.1.0

**Released:** 2026-11-08

The first release of `io.github.mcengine.build-logic`, the convention plugin that
replaces the root `build.gradle` every MCEngine-family plugin copies.

## Added

- `MCEngineBuildPlugin`: applied to a root project, gives every subproject with a build
  file the `java` plugin, the Java 25 toolchain, UTF-8 `JavaCompile`, `mavenCentral()`,
  and `group`/`version` from `project-group`/`project-version` (#2).
- `mcengineBuild { }`: `publishedModules` (default `api`, `common`, `bukkit`) choose the
  modules published to `GitHubPackages` as `${rootProject.name}-${project.name}`, with a
  POM URL from `git-org-name`/`git-repository`; `shadowModules` (default `engine`),
  `javaVersion`, `pomUrl` and developer fields (#2).
- `com.gradleup.shadow` 9.4.1 on the build classpath, applied to `shadowModules` (#2).
- README usage guide with MCStory publishing `tools` (#3); Publish and Release workflow
  callers (#4).

## Upgrading a plugin

Apply `id 'io.github.mcengine.build-logic' version '0.1.0'` in the root `build.gradle`
in place of its `allprojects`/`subprojects` blocks, add the plugin's GitHub Packages
repository to `pluginManagement`, and drop the version from every
`id 'com.gradleup.shadow'`. See the README.
