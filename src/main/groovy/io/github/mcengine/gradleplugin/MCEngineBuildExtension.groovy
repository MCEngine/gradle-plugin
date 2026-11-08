package io.github.mcengine.gradleplugin

import groovy.transform.CompileStatic
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * The {@code mcengineBuild { }} block a plugin's root build script uses to tune
 * the conventions {@link MCEngineBuildPlugin} applies.
 *
 * <p>Every value has a convention, so a root build that only applies the plugin
 * gets the family defaults. Values are read once, when the first subproject is
 * configured, and are final from then on: set them in the root build script.
 *
 * <p>Module lists match a subproject by its name ({@code engine}) or by its full
 * path ({@code :platforms:engine}).
 */
@CompileStatic
abstract class MCEngineBuildExtension {

    /** The name of the extension in the root project. */
    static final String NAME = 'mcengineBuild'

    /** The modules published to GitHub Packages; {@code api}, {@code common} and {@code bukkit} by default. */
    abstract ListProperty<String> getPublishedModules()

    /** The modules {@code com.gradleup.shadow} is applied to; {@code engine} by default. */
    abstract ListProperty<String> getShadowModules()

    /** The Java toolchain every module compiles with; 25 by default. */
    abstract Property<Integer> getJavaVersion()

    /** The GitHub organization; {@code git-org-name} from {@code gradle.properties} by default. */
    abstract Property<String> getGitOrgName()

    /** The GitHub repository; {@code git-repository} from {@code gradle.properties} by default. */
    abstract Property<String> getGitRepository()

    /** The POM {@code <url>}; {@code https://github.com/{gitOrgName}/{gitRepository}} by default. */
    abstract Property<String> getPomUrl()

    /** The POM developer id; empty leaves the {@code <developers>} block out. */
    abstract Property<String> getDeveloperId()

    /** The POM developer name. */
    abstract Property<String> getDeveloperName()

    /** The POM developer URL. */
    abstract Property<String> getDeveloperUrl()
}
