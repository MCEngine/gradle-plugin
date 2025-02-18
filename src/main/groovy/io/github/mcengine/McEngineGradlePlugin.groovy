package io.github.mcengine

import org.gradle.api.Plugin
import org.gradle.api.Project

class McEngineGradlePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.ext.getProjectVersion = { String projectName ->
            def version = project.findProperty("${projectName}-version") ?: 'unspecified'
            def classifier = project.findProperty("${projectName}-classifier") ?: 'unspecified'
            return [version, classifier]
        }
    }
}
