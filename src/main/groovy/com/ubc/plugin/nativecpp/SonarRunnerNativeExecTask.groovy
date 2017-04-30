package com.ubc.plugin.nativecpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SonarRunnerNativeExecTask extends DefaultTask {

    @TaskAction
    void runSonarAnalysis() {
        // Execute sonnar runner
        // sonar-runner -Dproject.settings=$project.projectDir/clean-room-build/sonar.properties
        project.javaexec {
            main = "org.sonar.runner.Main"
            args = ["-X"].toList()
            systemProperties = [
                    'project.home' : "$project.projectDir/clean-room-build",
                    'project.settings' : "$project.projectDir/clean-room-build/gbc-sonar.properties"
            ]
            classpath = project.buildscript.configurations.classpath
            workingDir = "$project.projectDir/clean-room-build"
        }
    }
}
