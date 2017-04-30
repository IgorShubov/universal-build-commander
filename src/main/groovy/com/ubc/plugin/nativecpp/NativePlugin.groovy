package com.ubc.plugin.nativecpp

import com.ubc.plugin.UniversalPlugin
import com.ubc.plugin.BuildCommanderPluginArtifactoryExtension
import com.ubc.plugin.BuildCommanderPluginSonarExtension
import org.gradle.api.Project

class NativePlugin extends UniversalPlugin {

    @Override
    void configureBuildProperties(Project project) {

        project.extensions.create("nativePlugin", NativePluginExtension)
        project.nativePlugin {
            nativeBuildTool = 'gmake'
            nativeBuildDir = '.'
            nativeMakefile = 'Makefile'
        }
    }

    @Override
    void configureTasks(Project project) {
        println "\n>>> Using the Native/Make Plugin to build the project\n"
        NativeBuildSupport.declarePublication(project)
        NativeBuildSupport.loadBuildPlugins(project)
        NativeBuildSupport.configureTaskGraph(project)
        NativeBuildSupport.configureCodeAnalysisTasks(project)
    }

    @Override
    void executeTasks(Project project) {

    }
}

class NativePluginExtension {
    def String  archiveName
    def String  nativeBuildTool
    def String  nativeBuildDir
    def String  nativeMakefile
    def String  nativeBuildTargets
    def Map     cmakeProperties
    def String  aol
    def String  dependencyDefaultAol
    def Map     nativeConfiguration
    def Map     envVars
    def boolean runCodeAnalysis
    def boolean buildServer
    def boolean daBuild
    def String  version
    def String  compilerVersion
    def BuildCommanderPluginArtifactoryExtension artifactory
    def BuildCommanderPluginSonarExtension sonar

    NativePluginExtension() {
        artifactory = new BuildCommanderPluginArtifactoryExtension()
        sonar = new BuildCommanderPluginSonarExtension()
    }
}
