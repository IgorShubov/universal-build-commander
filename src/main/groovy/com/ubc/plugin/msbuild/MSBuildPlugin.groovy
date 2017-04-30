package com.ubc.plugin.msbuild

import com.ubc.plugin.UniversalPlugin
import com.ubc.plugin.BuildCommanderPluginArtifactoryExtension
import com.ubc.plugin.BuildCommanderPluginSonarExtension
import org.gradle.api.Project

class MSBuildPlugin extends UniversalPlugin {

    @Override
    void configureBuildProperties(Project project) {

        project.extensions.create("msBuildPlugin", MSBuildPluginExtension)

        project.msBuildPlugin {
            // NuGet properties
            nugetVersion = "2.8.3"
            nugetZip = project.file("clean-room-nuget/nuget.commandline.${nugetVersion}.nupkg")
            nugetExe = project.file("clean-room-nuget/tools/NuGet.exe")
            nugetConfig = project.file("clean-room-nuget/NuGet.config")

            // NUnit and OpenCover properties
            nunitVersion = "2.6.4"
            nunitZip = project.file("clean-room-tests/nunit.runners.${nunitVersion}.nupkg")
            nunitExe = project.file("clean-room-tests/nunit/tools/nunit-console-x86.exe")
            nunitExe64 = project.file("clean-room-tests/nunit/tools/nunit-console.exe")

            openCoverVersion = "4.5.3723"
            openCoverZip = project.file("clean-room-tests/opencover.${openCoverVersion}.nupkg")
            openCoverExe = project.file("clean-room-tests/opencover/OpenCover.Console.exe")
            openCoverCoberturaVersion = "0.2.2.0"
            openCoverCoberturaZip = project.file("clean-room-tests/OpenCoverToCoberturaConverter.${openCoverCoberturaVersion}.nupkg")
            openCoverCoberturaExe = project.file("clean-room-tests/opencover/OpenCoverToCoberturaConverter.exe")

            sonarRunnerVersion = "1.0.2"
            sonarRunnerDir = project.file("clean-room-tools/SonarQubeMSBuildRunner")
            sonarRunnerExe = project.file("clean-room-tools/SonarQubeMSBuildRunner/tools/MSBuild.SonarQube.Runner.exe")
            sonarRunnerZip = project.file("clean-room-tools/sonar-runner/SonarQubeMSBuild.Runner.${sonarRunnerVersion}.nupkg")
        }
    }

    @Override
    void configureTasks(Project project) {
        project.task("downloadBuildTool", type: DownloadMSBuildTask, overwrite: true)
        project.task("setupBuildTool", type: SetupMSBuildTask, overwrite: true, dependsOn: 'downloadBuildTool')
        project.task("runBuildTool", type: RunMSBuildTask, overwrite: true, dependsOn: 'setupBuildTool')
        project.defaultTasks = ['runBuildTool']
        project.tasks.runBuildTool.finalizedBy('showHelp')
        if (project.msBuildPlugin.buildServer)
            project.tasks.runBuildTool.finalizedBy('getBuildInfoLink')
    }

    @Override
    void executeTasks(Project project) {

    }
}

class MSBuildPluginExtension {

    def String  nugetVersion
    def String  nugetUrl
    def String  nugetRepo
    def File    nugetZip
    def File    nugetExe
    def File    nugetConfig
    def String  nunitVersion
    def String  nunitUrl
    def File    nunitZip
    def File    nunitExe
    def File    nunitExe64
    def String  openCoverVersion
    def String  openCoverUrl
    def File    openCoverZip
    def File    openCoverExe
    def String  openCoverCoberturaVersion
    def String  openCoverCoberturaUrl
    def File    openCoverCoberturaZip
    def File    openCoverCoberturaExe
    def String  sonarRunnerUrl
    def String  sonarRunnerVersion
    def File    sonarRunnerZip
    def File    sonarRunnerDir
    def File    sonarRunnerExe
    def String  projectName
    def String  projectFile
    def String  projectVersion
    def String  verbosity
    def List    targets
    def List    releaseTargets
    def Map     parameters
    def Map     releaseParameters
    def boolean visualCpp
    def String  sdk
    def String  configuration
    def String  platform
    def String  architecture
    def List    buildFiles
    def boolean buildServer
    def boolean daBuild
    def boolean runCodeAnalysis
    def BuildCommanderPluginArtifactoryExtension artifactory
    def BuildCommanderPluginSonarExtension sonar

    MSBuildPluginExtension() {
        artifactory = new BuildCommanderPluginArtifactoryExtension()
        sonar = new BuildCommanderPluginSonarExtension()
    }
}
