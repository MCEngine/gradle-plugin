# Publishing

## A plugin's modules

Each module in [`publishedModules`](extension.md) is published by `./gradlew publish`
with:

| Setting | Value | MCStory |
|---|---|---|
| Repository | `GitHubPackages`, `https://maven.pkg.github.com/{gitOrgName}/{gitRepository}` | `https://maven.pkg.github.com/MCIpos/mcstory` |
| Credentials | `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables | |
| `groupId` | `project-group` | `io.github.mcipos` |
| `artifactId` | `${rootProject.name}-${project.name}` | `mcstory-api` |
| `version` | `project-version` | `0.2.1` |
| POM `<url>` | [`pomUrl`](extension.md) | `https://github.com/MCIpos/mcstory` |
| POM `<developers>` | the `developer*` properties | `jetsadawijit` |

The artifactId is qualified with the repository name because every plugin in a group
has an `api` module: bare module names would make two plugins publish the same
`io.github.mcipos:api`, and a consumer could not depend on both.

The artifactId and POM are set on every Maven publication of the module, including one
the module declares itself, so a module's own `pom { name = … }` is kept.

## The plugin itself

`MCEngine/gradle-plugin` publishes two artifacts to
`https://maven.pkg.github.com/MCEngine/gradle-plugin`:

| Artifact | Coordinates |
|---|---|
| The plugin jar | `io.github.mcengine:gradle-plugin:{version}` |
| The plugin marker | `io.github.mcengine.build-logic:io.github.mcengine.build-logic.gradle.plugin:{version}` |

The marker is what `plugins { id 'io.github.mcengine.build-logic' version '…' }`
resolves; it depends on the jar, which depends on
`com.gradleup.shadow:shadow-gradle-plugin:9.4.1`.
