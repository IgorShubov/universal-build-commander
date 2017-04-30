package com.ubc.plugin.maven

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DownloadMavenTask extends DefaultTask {

    /**
     * The Gradle task that downloads the Maven zip
     */
    @TaskAction
    def DownloadAction() {
        println "\n>>> Downloading Maven v$project.mavenPlugin.mvnVersion...\n"

        project.mavenPlugin.mvnZip.parentFile.mkdirs()
        project.ant.get(src: project.mavenPlugin.mvnUrl, dest: project.mavenPlugin.mvnZip.path)
    }
}
