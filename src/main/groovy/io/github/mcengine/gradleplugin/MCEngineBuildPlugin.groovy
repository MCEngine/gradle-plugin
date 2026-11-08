package io.github.mcengine.gradleplugin

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.HasConfigurableValue
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * The {@code io.github.mcengine.build-logic} convention plugin: the root
 * {@code build.gradle} every MCEngine-family plugin used to copy.
 *
 * <p>Applied to the root project, it configures every subproject that has a
 * build file of its own (an intermediate folder such as {@code platforms} is
 * skipped):
 * <ul>
 *   <li>{@code group} and {@code version} from {@code project-group} and
 *       {@code project-version}, and {@code mavenCentral()}, on every project;</li>
 *   <li>the {@code java} plugin, the Java toolchain and UTF-8 {@code JavaCompile};</li>
 *   <li>for each module in {@link MCEngineBuildExtension#getPublishedModules()}:
 *       {@code maven-publish}, the {@code GitHubPackages} repository, the
 *       {@code ${rootProject.name}-${project.name}} artifactId and the POM;</li>
 *   <li>{@code com.gradleup.shadow} for each module in
 *       {@link MCEngineBuildExtension#getShadowModules()}.</li>
 * </ul>
 *
 * <p>A subproject is configured just before its own build file runs. By then the
 * root build script has finished, so its {@code mcengineBuild { }} values are
 * known, and a module's own {@code publishing { }} block already sees
 * {@code maven-publish} applied.
 */
@CompileStatic
class MCEngineBuildPlugin implements Plugin<Project> {

    /** The plugin id. */
    static final String ID = 'io.github.mcengine.build-logic'

    /** The name of the GitHub Packages repository every published module gets. */
    static final String REPOSITORY_NAME = 'GitHubPackages'

    /** The publication created for a published module that declares none. */
    static final String PUBLICATION_NAME = 'mavenJava'

    static final String SHADOW_PLUGIN_ID = 'com.gradleup.shadow'

    @Override
    void apply(Project project) {
        if (project != project.rootProject) {
            throw new GradleException("${ID} configures a whole build: apply it to the root project, not ${project.path}")
        }

        MCEngineBuildExtension extension = createExtension(project)

        project.allprojects { Project each -> applyIdentity(each) }
        registerRootClean(project)

        project.subprojects { Project subproject ->
            subproject.beforeEvaluate { configureSubproject(subproject, extension) }
        }
    }

    private static MCEngineBuildExtension createExtension(Project root) {
        MCEngineBuildExtension extension = root.extensions.create(MCEngineBuildExtension.NAME, MCEngineBuildExtension)

        // The lists start with a value rather than a convention, so a root build
        // can both replace them (`publishedModules = [...]`) and extend them
        // (`publishedModules.add('tools')`).
        extension.publishedModules.set(['api', 'common', 'bukkit'])
        extension.shadowModules.set(['engine'])
        extension.javaVersion.convention(25)
        extension.gitOrgName.convention(root.providers.gradleProperty('git-org-name'))
        extension.gitRepository.convention(root.providers.gradleProperty('git-repository'))
        extension.pomUrl.convention(extension.gitOrgName.zip(extension.gitRepository) { String org, String repo ->
            "https://github.com/${org}/${repo}".toString()
        })
        extension.developerId.convention('jetsadawijit')
        extension.developerName.convention('jetsadawijit')
        extension.developerUrl.convention('https://jetsadawijit.github.io')

        // The first subproject reads these; a later change would leave the
        // modules configured before it disagreeing with the ones after.
        List<HasConfigurableValue> values = [extension.publishedModules, extension.shadowModules,
                extension.javaVersion, extension.gitOrgName, extension.gitRepository, extension.pomUrl,
                extension.developerId, extension.developerName, extension.developerUrl] as List<HasConfigurableValue>
        values.each { HasConfigurableValue value -> value.finalizeValueOnRead() }
        extension
    }

    private static void applyIdentity(Project project) {
        Object group = project.findProperty('project-group')
        if (group != null) {
            project.group = group
        }
        Object version = project.findProperty('project-version')
        if (version != null) {
            project.version = version.toString()
        }
        project.repositories.mavenCentral()
    }

    // The root project applies no language plugin, so it has no clean task of
    // its own. Registered late so a root build that still declares one wins.
    private static void registerRootClean(Project root) {
        root.afterEvaluate {
            if (!root.tasks.names.contains('clean')) {
                root.tasks.register('clean', Delete) { Delete task ->
                    task.delete(root.layout.buildDirectory)
                }
            }
        }
    }

    private static void configureSubproject(Project project, MCEngineBuildExtension extension) {
        // Intermediate folders such as `platforms` have no build file and stay bare.
        if (!project.buildFile.exists()) {
            return
        }

        project.pluginManager.apply(JavaPlugin)
        project.extensions.getByType(JavaPluginExtension).toolchain.languageVersion.set(
                extension.javaVersion.map { Integer version -> JavaLanguageVersion.of(version) })
        project.tasks.withType(JavaCompile).configureEach { JavaCompile task ->
            task.options.encoding = 'UTF-8'
        }

        if (matches(project, extension.publishedModules.get())) {
            configurePublishing(project, extension)
        }
        if (matches(project, extension.shadowModules.get())) {
            project.pluginManager.apply(SHADOW_PLUGIN_ID)
        }
    }

    private static boolean matches(Project project, List<String> modules) {
        modules.contains(project.name) || modules.contains(project.path)
    }

    private static void configurePublishing(Project project, MCEngineBuildExtension extension) {
        String org = required(extension.gitOrgName, 'git-org-name', 'gitOrgName')
        String repository = required(extension.gitRepository, 'git-repository', 'gitRepository')
        String artifactId = "${project.rootProject.name}-${project.name}"

        project.pluginManager.apply(MavenPublishPlugin)
        PublishingExtension publishing = project.extensions.getByType(PublishingExtension)

        Provider<String> actor = project.providers.environmentVariable('GITHUB_ACTOR')
        Provider<String> token = project.providers.environmentVariable('GITHUB_TOKEN')
        publishing.repositories.maven { MavenArtifactRepository maven ->
            maven.name = REPOSITORY_NAME
            maven.url = project.uri("https://maven.pkg.github.com/${org}/${repository}")
            maven.credentials { PasswordCredentials credentials ->
                credentials.username = actor.getOrNull()
                credentials.password = token.getOrNull()
            }
        }

        // Qualify the coordinates with the repository name: the bare module name
        // would make every plugin in a group publish a colliding `api`.
        publishing.publications.withType(MavenPublication).configureEach { MavenPublication publication ->
            publication.artifactId = artifactId
            configurePom(publication.pom, extension)
        }

        // A module listed for publishing that declares no publication of its own
        // (a `tools` module, say) still publishes its jar.
        project.afterEvaluate {
            if (publishing.publications.isEmpty()) {
                publishing.publications.create(PUBLICATION_NAME, MavenPublication) { MavenPublication publication ->
                    publication.from(project.components.getByName('java'))
                }
            }
        }
    }

    private static void configurePom(MavenPom pom, MCEngineBuildExtension extension) {
        pom.url.set(extension.pomUrl)
        if (!extension.developerId.isPresent() || extension.developerId.get().isEmpty()) {
            return
        }
        pom.developers { MavenPomDeveloperSpec developers ->
            developers.developer { MavenPomDeveloper developer ->
                developer.id.set(extension.developerId)
                developer.name.set(extension.developerName)
                developer.url.set(extension.developerUrl)
            }
        }
    }

    private static String required(Property<String> value, String gradleProperty, String extensionProperty) {
        if (!value.isPresent() || value.get().trim().isEmpty()) {
            throw new GradleException("${ID} publishes to GitHub Packages but has no ${gradleProperty}: "
                    + "set it in gradle.properties or mcengineBuild.${extensionProperty} in the root build script")
        }
        value.get()
    }
}
