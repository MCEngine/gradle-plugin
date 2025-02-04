package io.github.mcengine

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class McEngineGradlePluginTest extends Specification {
    def "plugin applies correctly"() {
        given:
        def project = ProjectBuilder.builder().withProjectDir(new File('lib')).build()
        
        when:
        try {
            project.pluginManager.apply('io.github.mcengine.gradle-plugin')
        } catch (Exception e) {
            e.printStackTrace() // Print error details
            throw e
        }

        then:
        project.ext.has("getProjectVersion")
    }
}
