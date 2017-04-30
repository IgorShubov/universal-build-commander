package com.ubc.plugin.msbuild

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class DownloadMSBuildTask extends DefaultTask {

    @OutputFile
    File nugetZip = project.msBuildPlugin.nugetZip
    @OutputFile
    File nunitZip = project.msBuildPlugin.nunitZip
    @OutputFile
    File openCoverZip = project.msBuildPlugin.openCoverZip
    @OutputFile
    File openCoverCoberturaZip = project.msBuildPlugin.openCoverCoberturaZip
    @OutputFile
    File sonarRunnerZip = project.msBuildPlugin.sonarRunnerZip

    /**
     * The Gradle action that downloads NuGet and MSBuild
     */
    @TaskAction
    def DownloadMSBuildTools() {
        downloadNuGet()
        if (project.tektonMSBuildPlugin.daBuild) {
            downloadNUnit()
            downloadOpenCover()
            downloadOpenCoverToCoberturaConverter()
        }
        downloadMSBuild()
        if (project.tektonMSBuildPlugin.analysisBuild) {
            downloadSonarRunner()
        }
    }

    /**
     * Download the NuGet.CommandLine package
     */
    def downloadNuGet() {
        println "\n>>> Downloading NuGet v${project.tekton.msBuildPlugin.nugetVersion}...\n"

        project.msBuildPlugin.nugetZip.parentFile.mkdirs()
        project.ant.get(src: project.msBuildPlugin.nugetUrl, dest: project.msBuildPlugin.nugetZip.path)
    }

    /**
     * Download the NUnit.Runners package
     */
    def downloadNUnit() {
        println ">>> Downloading NUnit v${project.msBuildPlugin.nunitVersion}...\n"

        project.msBuildPlugin.nunitZip.parentFile.mkdirs()
        project.ant.get(src: project.msBuildPlugin.nunitUrl, dest: project.msBuildPlugin.nunitZip.path)
    }

    /**
     * Download the OpenCover package
     */
    def downloadOpenCover() {
        println ">>> Downloading OpenCover v${project.msBuildPlugin.openCoverVersion}...\n"

        project.msBuildPlugin.openCoverZip.parentFile.mkdirs()
        project.ant.get(src: project.msBuildPlugin.openCoverUrl, dest: project.msBuildPlugin.openCoverZip.path)
    }

    /**
     * Download the OpenCoverToCoberturaConverter package
     */
    def downloadOpenCoverToCoberturaConverter() {
        println ">>> Downloading OpenCoverToCoberturaConverter v${project.msBuildPlugin.openCoverCoberturaVersion}...\n"

        project.msBuildPlugin.openCoverCoberturaZip.parentFile.mkdirs()
        project.ant.get(src: project.msBuildPlugin.openCoverCoberturaUrl, dest: project.msBuildPlugin.openCoverCoberturaZip.path)
    }

    /**
     * Download MSBuild Tools
     */
    def downloadMSBuild() {
//        println ">>> I totally just downloaded MSBuild...\n"
    }

    def downloadSonarRunner() {
        println ">>> Downloading MSBuild SonarRunner v${project.msBuildPlugin.sonarRunnerVersion}...\n"

        project.msBuildPlugin.sonarRunnerZip.parentFile.mkdirs()
        project.ant.get(src: project.msBuildPlugin.sonarRunnerUrl, dest: project.msBuildPlugin.sonarRunnerZip.path)
    }
}
