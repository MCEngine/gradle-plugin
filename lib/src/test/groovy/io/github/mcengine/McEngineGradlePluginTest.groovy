package io.github.mcengine

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class McEngineGradlePluginTest extends Specification {
    def "plugin applies correctly"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.pluginManager.apply('io.github.mcengine.gradle-plugin')

        then:
        project.ext.has("getProjectVersion")
    }
}
