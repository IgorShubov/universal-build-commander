package com.ubc.plugin.maven

import com.ubc.plugin.UniversalPlugin
import com.ubc.plugin.BuildCommanderPluginArtifactoryExtension
import com.ubc.plugin.BuildCommanderPluginSonarExtension
import org.apache.xerces.parsers.DOMParser
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class MavenPlugin extends UniversalPlugin {

    static String DEFAULT_MAVEN_VERSION = "3.2.3"
    @Override
    void configureBuildProperties(Project project) {
        project.extensions.create("mavenPlugin", MavenPluginExtension)

        // Default Maven properties
        project.mavenPlugin {
            mvnVersion = DEFAULT_MAVEN_VERSION
            mvnZip = project.file("clean-room-maven/maven-${mvnVersion}.zip")
            mvnArgs = ["\"-DskipTests=true\"", "\"-DbuildUser=${System.getProperty('artyUsername') ?: 'n/a'}\"", "\"-DbuildPassword=${System.getProperty('artyPassword') ?: 'n/a'}\""]
            mvnSettings = project.file("clean-room-maven/build-settings.xml")
            protobufExec = "protoc-"
            envVars = [:]
            toolchains = [:]
            buildServer = false
            daBuild = false
        }
    }

    @Override
    void configureTasks(Project project) {
        project.task("downloadBuildTool", type: DownloadMavenTask, overwrite: true)
        project.task("setupBuildTool", type: SetupMavenTask, overwrite: true, dependsOn: 'downloadBuildTool')
        project.task("runBuildTool", type: RunMavenTask, overwrite: true, dependsOn: 'setupBuildTool')
        project.task("runCodeQualityScan", type: MavenSonarScanTask, overwrite:true, dependsOn: 'setupBuildTool')
        if (project.mavenPlugin.runCodeAnalysis) {
            project.defaultTasks = ['runBuildTool', 'runCodeQualityScan']
        } else {
            project.defaultTasks = ['runBuildTool']
        }
        project.tasks.runBuildTool.finalizedBy('showHelp')
        if (project.mavenPlugin.buildServer)
            project.tasks.runBuildTool.finalizedBy('getBuildInfoLink')
    }

    @Override
    void executeTasks(Project project) {

    }


    def List<String> getModules(Project project) {

        DOMParser parser = new DOMParser()
        parser.setFeature("http://xml.org/sax/features/namespaces", false)
        File rootPom = new File(project.mavenPlugin.rootPomPath.replaceAll("%20", " "))
        parser.parse(rootPom.toURI().toString())
        Document document = parser.getDocument()
        def root = document.getDocumentElement()
        NodeList modulesNL = root.getElementsByTagName("modules")
        def modules = []
        modulesNL.each { modulesN ->
            def modulesNode = modulesN as Element
            NodeList moduleNL = modulesNode.getElementsByTagName("module")
            moduleNL.each {
                modules << it.textContent
            }
        }
        modules
    }
}

class MavenPluginExtension {
    def boolean buildServer
    def boolean daBuild
    def boolean runCodeAnalysis
    def String  mvnVersion
    def String  mvnUrl
    def File    mvnZip
    def Collection mvnArgs = []
    def String  mvnOpts
    def File    mvnSettings

    def String  protobufUrl
    def String  protobufExec

    def String  buildDir
    def Map     toolchains
    def Map     envVars
    def String  buildInfo
    def String  rootPomPath
    def Collection modules = []

    def BuildCommanderPluginArtifactoryExtension artifactory
    def BuildCommanderPluginSonarExtension sonar

    MavenPluginExtension() {
        artifactory = new BuildCommanderPluginArtifactoryExtension()
        sonar = new BuildCommanderPluginSonarExtension()
    }
}