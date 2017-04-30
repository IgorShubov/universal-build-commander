package com.ubc.plugin.gradle

import com.ubc.plugin.BuildCommanderPluginArtifactoryExtension
import com.ubc.plugin.BuildCommanderPluginSonarExtension
import com.ubc.plugin.UniversalPlugin
import org.gradle.api.Project

class GradlePlugin extends UniversalPlugin {

    static String DEFAULT_GRADLE_VERSION = "2.7"


    @Override
    void configureBuildProperties(Project project) {

        project.extensions.create("gradlePlugin", GradlePluginExtension)
        project.gradlePlugin {

            gradleVersion = DEFAULT_GRADLE_VERSION
            gradleBuildFile = project.file("clean-room-build/build.gradle").path
            gradleZip = project.file("clean-room-gradle/gradle-${project.gradlePlugin.gradleVersion}.zip")
        }
    }

    @Override
    void configureTasks(Project project) {
        project.task("downloadBuildTool", type: DownloadGradleTask, overwrite: true)
        project.task("setupBuildTool", type: SetupGradleTask, overwrite: true, dependsOn: 'downloadBuildTool')
        project.task("runBuildTool", type: RunGradleTask, overwrite: true, dependsOn: 'setupBuildTool')
        project.task("runCodeQualityScan", type: GradleSonarScanTask, overwrite: true, dependsOn: 'setupBuildTool')
        if (project.gradlePlugin.runSonar) {
            project.defaultTasks = ['runBuildTool', 'runCodeQualityScan']
        } else {
            project.defaultTasks = ['runBuildTool']
        }
        project.tasks.runBuildTool.finalizedBy('showHelp')
        if (project.gradlePlugin.buildServer)
            project.tasks.runBuildTool.finalizedBy('getBuildInfoLink')
    }

    @Override
    void executeTasks(Project project) {

    }

}

class GradlePluginExtension {
    def boolean buildServer
    def boolean daBuild
    def boolean runCodeAnalysis
    def boolean useCurrentGradle
    def String  gradleBuildFile
    def String  gradleVersion
    def File    gradleZip
    def String  gradleUrl
    def String  gradleSettingsFile
    def String  gradleOpts
    def List    gradleTasks
    def List    gradleArgs
    def List    gradleSystemProperties
    def List    gradleProjectProperties
    def Map     envVars
    def BuildCommanderPluginArtifactoryExtension artifactory
    def BuildCommanderPluginSonarExtension sonar

    GradlePluginExtension() {
        artifactory = new BuildCommanderPluginArtifactoryExtension()
        sonar = new BuildCommanderPluginSonarExtension()
    }
}
