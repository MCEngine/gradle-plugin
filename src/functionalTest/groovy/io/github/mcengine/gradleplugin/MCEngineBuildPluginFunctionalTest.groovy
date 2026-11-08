package io.github.mcengine.gradleplugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Runs real builds of an MCStory-shaped plugin — api, common, tools, bukkit and
 * platforms:{spigotmc,engine} — with the convention plugin applied, and reads
 * back what each subproject ended up with.
 */
class MCEngineBuildPluginFunctionalTest {

    // Prints one line per subproject once every project is configured, so a
    // test can assert on the result without compiling anything (no Java 25
    // toolchain is needed to run these tests).
    private static final String PROBE = '''
gradle.projectsEvaluated {
    subprojects.each { p ->
        def line = "PROBE ${p.path}"
        if (p.plugins.hasPlugin('java')) {
            line += " java=${p.java.toolchain.languageVersion.get()}"
            line += " encoding=${p.tasks.named('compileJava').get().options.encoding}"
        } else {
            line += ' java=none'
        }
        line += " group=${p.group} version=${p.version}"
        def publishing = p.extensions.findByName('publishing')
        if (publishing != null) {
            line += ' artifacts=' + publishing.publications.collect { it.artifactId }.sort().join(',')
            line += ' repo=' + publishing.repositories.findByName('GitHubPackages')?.url
        } else {
            line += ' artifacts=none'
        }
        line += " shadow=${p.plugins.hasPlugin('com.gradleup.shadow')}"
        line += " repos=${p.repositories.collect { it.name }.join(',')}"
        println line
    }
}
'''

    @TempDir
    File projectDir

    @BeforeEach
    void writeSampleBuild() {
        write('settings.gradle', '''
rootProject.name = 'mcstory'
include 'api', 'common', 'tools', 'bukkit', 'platforms:spigotmc', 'platforms:engine'
''')
        write('gradle.properties', '''
project-group=io.github.mcipos
git-org-name=MCIpos
git-repository=mcstory
project-version=0.2.1
''')
        // api declares its own publication, as every plugin's api does today.
        write('api/build.gradle', '''
plugins { id 'java-library' }
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            pom { name = "${rootProject.name}-${project.name}" }
        }
    }
}
''')
        write('common/build.gradle', "plugins { id 'java-library' }\n")
        write('bukkit/build.gradle', "plugins { id 'java-library' }\n")
        write('tools/build.gradle', "plugins { id 'java-library' }\n")
        write('platforms/spigotmc/build.gradle', '')
        // The engine module names shadow without a version: the convention
        // plugin already put it on the build classpath.
        write('platforms/engine/build.gradle', "plugins { id 'com.gradleup.shadow' }\njar { enabled = false }\n")
    }

    // Shadow resolves the module's Java compiler while it is configured, so a
    // build that applies it needs the toolchain installed. Tests pin the
    // toolchain to the JVM running them; only the defaults test leaves 25.
    private static final int RUNNING_JAVA = Runtime.version().feature()

    @Test
    void shadowIsAppliedToTheEngineByDefault() {
        writeRoot('')

        Map<String, String> probe = probe(run('help', ':platforms:engine:tasks', '--all'))

        assertTrue(probe[':platforms:engine'].contains('shadow=true'), probe[':platforms:engine'])
        assertTrue(probe[':platforms:spigotmc'].contains('shadow=false'), probe[':platforms:spigotmc'])
        assertTrue(probe[':api'].contains('shadow=false'), probe[':api'])
    }

    @Test
    void defaultsMatchTheFamilyRootBuild() {
        // The engine applies shadow itself; without a Java 25 toolchain on the
        // machine it cannot be configured, so it stays a bare module here.
        write('platforms/engine/build.gradle', '')
        writeRoot('', true, false)

        Map<String, String> probe = probe(run('help'))

        ['api', 'common', 'bukkit'].each { String module ->
            String line = probe[":${module}"]
            assertTrue(line.contains('java=25'), line)
            assertTrue(line.contains('encoding=UTF-8'), line)
            assertTrue(line.contains('group=io.github.mcipos version=0.2.1'), line)
            assertTrue(line.contains("artifacts=mcstory-${module} "), line)
            assertTrue(line.contains('repo=https://maven.pkg.github.com/MCIpos/mcstory'), line)
            assertTrue(line.contains('repos=MavenRepo'), line)
        }
        assertTrue(probe[':tools'].contains('java=25 encoding=UTF-8'), probe[':tools'])
        assertTrue(probe[':tools'].contains('artifacts=none'), probe[':tools'])
        assertTrue(probe[':platforms:spigotmc'].contains('artifacts=none shadow=false'), probe[':platforms:spigotmc'])
        assertTrue(probe[':platforms:engine'].contains('java=25'), probe[':platforms:engine'])
        assertTrue(probe[':platforms:engine'].contains('artifacts=none'), probe[':platforms:engine'])
        // The intermediate folder has no build file and gets no java plugin.
        assertTrue(probe[':platforms'].contains('java=none'), probe[':platforms'])
    }

    @Test
    void publishedModulesAddsTools() {
        writeRoot('''
mcengineBuild {
    publishedModules = ['api', 'common', 'bukkit', 'tools']
}
''')

        Map<String, String> probe = probe(run('help'))

        // tools declares no publication, so the plugin creates mavenJava for it.
        assertTrue(probe[':tools'].contains('artifacts=mcstory-tools '), probe[':tools'])
        assertTrue(probe[':api'].contains('artifacts=mcstory-api '), probe[':api'])
    }

    @Test
    void publishedModulesCanDropAModule() {
        writeRoot('''
mcengineBuild {
    publishedModules = ['api']
}
''')

        Map<String, String> probe = probe(run('help'))

        assertTrue(probe[':api'].contains('artifacts=mcstory-api '), probe[':api'])
        assertTrue(probe[':common'].contains('artifacts=none'), probe[':common'])
        assertTrue(probe[':bukkit'].contains('artifacts=none'), probe[':bukkit'])
    }

    @Test
    void generatedPomCarriesQualifiedArtifactIdAndRepositoryUrl() {
        writeRoot('''
mcengineBuild {
    publishedModules.add('tools')
}
''')

        run('--configuration-cache', ':api:generatePomFileForMavenJavaPublication',
                ':tools:generatePomFileForMavenJavaPublication')

        String api = new File(projectDir, 'api/build/publications/mavenJava/pom-default.xml').text
        assertTrue(api.contains('<groupId>io.github.mcipos</groupId>'), api)
        assertTrue(api.contains('<artifactId>mcstory-api</artifactId>'), api)
        assertTrue(api.contains('<version>0.2.1</version>'), api)
        assertTrue(api.contains('<name>mcstory-api</name>'), api)
        assertTrue(api.contains('<url>https://github.com/MCIpos/mcstory</url>'), api)
        assertTrue(api.contains('<id>jetsadawijit</id>'), api)

        String tools = new File(projectDir, 'tools/build/publications/mavenJava/pom-default.xml').text
        assertTrue(tools.contains('<artifactId>mcstory-tools</artifactId>'), tools)
    }

    @Test
    void extensionOverridesTheJavaVersion() {
        // No shadow here: shadow would need a Java 17 toolchain installed.
        write('platforms/engine/build.gradle', '')
        writeRoot('''
mcengineBuild {
    javaVersion = 17
    shadowModules = []
}
''')

        Map<String, String> probe = probe(run('help'))

        assertTrue(probe[':api'].contains('java=17'), probe[':api'])
        assertTrue(probe[':platforms:engine'].contains('java=17'), probe[':platforms:engine'])
    }

    @Test
    void extensionOverridesThePom() {
        writeRoot('''
mcengineBuild {
    pomUrl = 'https://mcoriax.github.io/website'
    developerId = ''
}
''')

        run(':api:generatePomFileForMavenJavaPublication')

        String api = new File(projectDir, 'api/build/publications/mavenJava/pom-default.xml').text
        assertTrue(api.contains('<url>https://mcoriax.github.io/website</url>'), api)
        assertTrue(!api.contains('<developers>'), api)
    }

    @Test
    void rootGetsACleanTask() {
        writeRoot('')

        BuildResult result = run('clean')

        assertEquals(org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE, result.task(':clean').outcome)
    }

    @Test
    void failsWhenAppliedToASubproject() {
        writeRoot('', false)
        write('api/build.gradle', "plugins { id 'io.github.mcengine.build-logic' }\n")

        BuildResult result = runner('help').buildAndFail()

        assertTrue(result.output.contains('apply it to the root project, not :api'), result.output)
    }

    @Test
    void failsWithoutTheRepositoryProperties() {
        write('gradle.properties', 'project-group=io.github.mcipos\nproject-version=0.2.1\n')
        writeRoot('')

        BuildResult result = runner('help').buildAndFail()

        assertTrue(result.output.contains('has no git-org-name'), result.output)
    }

    private void writeRoot(String body, boolean apply = true, boolean pinJava = true) {
        String plugins = apply ? "plugins { id 'io.github.mcengine.build-logic' }\n" : ''
        String java = pinJava && apply ? "mcengineBuild { javaVersion = ${RUNNING_JAVA} }\n" : ''
        if (!pinJava) {
            // Without a pinned toolchain, keep shadow off: it would need Java 25 installed.
            java = "mcengineBuild { shadowModules = [] }\n"
        }
        write('build.gradle', plugins + java + body + PROBE)
    }

    private BuildResult run(String... arguments) {
        runner(arguments).build()
    }

    private GradleRunner runner(String... arguments) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments((arguments as List<String>) + ['--stacktrace'])
                .forwardOutput()
    }

    private static Map<String, String> probe(BuildResult result) {
        Map<String, String> lines = [:]
        result.output.readLines().findAll { it.startsWith('PROBE ') }.each { String line ->
            lines[line.split(' ')[1]] = line + ' '
        }
        lines
    }

    private void write(String path, String content) {
        File file = new File(projectDir, path)
        file.parentFile.mkdirs()
        file.text = content
    }
}
