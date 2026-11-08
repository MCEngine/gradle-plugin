# The `mcengineBuild { }` extension

Set in the root `build.gradle`, after the `plugins { }` block. Values are read when the
first subproject is configured and are final from then on, so a module build cannot
change them.

| Property | Type | Default |
|---|---|---|
| `publishedModules` | `List<String>` | `['api', 'common', 'bukkit']` |
| `shadowModules` | `List<String>` | `['engine']` |
| `javaVersion` | `Integer` | `25` |
| `gitOrgName` | `String` | `git-org-name` from `gradle.properties` |
| `gitRepository` | `String` | `git-repository` from `gradle.properties` |
| `pomUrl` | `String` | `https://github.com/{gitOrgName}/{gitRepository}` |
| `developerId` | `String` | `jetsadawijit`; `''` leaves `<developers>` out of the POM |
| `developerName` | `String` | `jetsadawijit` |
| `developerUrl` | `String` | `https://jetsadawijit.github.io` |

The two lists start with a value, so both forms work:

```groovy
mcengineBuild {
    publishedModules.add('tools')                              // extend the default
    publishedModules = ['api', 'common', 'bukkit', 'tools']    // or replace it
}
```

## Examples

MCIdentity, whose POM points at its website:

```groovy
mcengineBuild {
    pomUrl = 'https://mcoriax.github.io/website'
}
```

A plugin that publishes only its API and shades nothing in the engine automatically:

```groovy
mcengineBuild {
    publishedModules = ['api']
    shadowModules = []
}
```

## Errors

| Message | Cause |
|---|---|
| `publishes to GitHub Packages but has no git-org-name` (or `git-repository`) | A published module exists and neither `gradle.properties` nor the extension names the repository. |
| `configures a whole build: apply it to the root project, not :api` | The plugin was applied in a module build. |
