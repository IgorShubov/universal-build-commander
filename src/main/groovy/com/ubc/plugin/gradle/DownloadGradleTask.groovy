package com.ubc.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DownloadGradleTask extends DefaultTask {

    @TaskAction
    def DownloadAction() {
        project.gradlePlugin.gradleZip.parentFile.mkdirs()
        if (project.gradlePlugin.useCurrentGradle) {
            println "\n>>> Using the current version of Gradle: $project.gradlePlugin.gradleVersion\n"
        } else {
            println "\n>>> Downloading Gradle v${project.gradlePlugin.gradleVersion}...\n"
            project.ant.get(src: project.gradlePlugin.gradleUrl, dest: project.gradlePlugin.gradleZip.path)
        }
    }
}