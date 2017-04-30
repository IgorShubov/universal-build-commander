package com.ubc.plugin

import com.ubc.BuildToolDescriptor
import com.ubc.BuildToolFinder
import com.ubc.BuildXmlParser
import com.ubc.plugin.gradle.GradlePlugin
import com.ubc.plugin.maven.MavenPlugin
import com.ubc.plugin.msbuild.MSBuildPlugin
import com.ubc.plugin.nativecpp.NativePlugin
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The Build Commander Gradle plugin
 */
class BuildCommanderPlugin implements Plugin<Project> {

    /**
     * Applies the BuildCommanderPlugin to a project
     *
     * @param project   the gradle project
     */
    void apply(Project project) {
        // Add the DSL extension to read our properties from the .gradle file
        project.extensions.create("buildCommander", BuildCommanderPluginExtension)
        project.extensions.create("buildCommanderArtifactory", BuildCommanderPluginArtifactoryExtension)
        project.extensions.create("buildCommanderSonar", BuildCommanderPluginSonarExtension)
        project.buildCommander {
            buildDir = "$project.projectDir.path/clean-room-build"
        }

        println "Build Commander v$project.properties.commanderVersion powered by Gradle v$project.gradle.gradleVersion"

        BuildXmlParser.initSchema()
        // Once the project has been evaluated we do the prep work if the run task or no tasks are specified
        project.afterEvaluate {

            configureBuild(project)

            buildProject(project)
        }

        defineTasks(project)
    }

    private void defineTasks(Project project) {
        // Declare all the available tasks, at this point the download/setup/run ones are empty shells
        project.task('downloadBuildTool', description: 'Downloads the required build tool.')
        project.task('setupBuildTool', dependsOn: 'downloadBuildTool', description: 'Configured the required build tool.')
        project.task('runBuildTool', dependsOn: 'setupBuildTool', description: 'Runs the Build Commander.')
        project.task('runCodeQualityScan', dependsOn: 'setupBuildTool', description: 'Scan source code for code quality analysis.')
        project.task('cleanBuildDirs', description: 'Cleans the Build Commander temporary subdirectories.') << {
            project.delete 'clean-room-build', 'clean-room-maven', 'clean-room-gradle', 'build'
        }
        project.task('showHelp', description: 'Shows a help link to documentation when the build fails.') {
            mustRunAfter 'runBuildTool'

            onlyIf {
                !project.gradle.taskGraph.taskExecutionPlan.failures.isEmpty()
            }

            doLast {
                println "\n>>> [ERROR] Your build may have failed due to missing project or environment properties." +
                        "\n>>> You can specify these in an xml file, and configure it using buildCommander{ buildXmlParh = } \n"
            }
        }
    }

    private void buildProject(Project project) {

        checkRequiredOS(project)
        switch (project.buildCommander.buildToolDescriptor) {
            case BuildToolDescriptor.MAVEN:
                applyMavenPlugin(project)
                break
            case BuildToolDescriptor.GRADLE:
                applyGradlePlugin(project)
                break
            case BuildToolDescriptor.MSBUILD:
            case BuildToolDescriptor.VISUALSTUDIO:
                applyMsBuildPlugin(project)
                break
            case BuildToolDescriptor.MAKE:
            case BuildToolDescriptor.NATIVE:
                applyNativePlugin(project)
                break
            default:
                println "\n>>> [ERROR] The detected build tool, $project.buildCommander.buildToolDescriptor.id, is not yet supported"
                throw new GradleException("Detected unsupported build tool: $project.buildCommander.buildToolDescriptor.id")
        }
    }

    private void applyMavenPlugin(Project project) {
        project.apply plugin:MavenPlugin
        PluginConfigurationClosureFactory.createMavenConfigClosure().call(project)
    }

    private void applyGradlePlugin(Project project) {
        project.apply plugin: GradlePlugin
        PluginConfigurationClosureFactory.createGradleConfigClosure().call(project)
    }

    private void applyMsBuildPlugin(Project project) {
        project.apply plugin: MSBuildPlugin
        PluginConfigurationClosureFactory.createMsBuildConfigClosure().call(project)
    }

    private void applyNativePlugin(Project project) {
        project.apply plugin: NativePlugin
        PluginConfigurationClosureFactory.createNativeConfigClosure().call(project)
    }

    /**
     * Set the build configuration from the build.xml file as project extra properties
     *
     * @param project the gradle project
     * @param buildTool the detected build tool descriptor
     */
    protected static void configureBuild(Project project) {

        boolean buildXmlFileExists = project.buildCommander.buildXmlPath != null && project.file(project.buildCommander.buildXmlPath).exists()

        if (buildXmlFileExists) {
            def buildXmlFile = project.file(project.buildCommander.buildXmlPath)
            println ">>> Validating and applying build properties from $buildXmlFile"
            try {
                // todo check whether having project.configure means we dont need to prefix project.
                project.configure(project) {
                    def buildXmlParser = new BuildXmlParser()
                    buildXmlParser.validateBuildProperties(buildXmlFile.absolutePath)
                    def envProperties = buildXmlParser.getEnvironmentProperties(buildXmlFile.absolutePath)

                    if (envProperties.containsKey('buildTool')) {
                        def declaredBuildTool = envProperties.get('buildTool')
                        if (declaredBuildTool == 'msbuild')
                            declaredBuildTool = 'visual studio'
                        declaredBuildTool = declaredBuildTool.tokenize().collect { it.capitalize() }.join(' ')

                        if (declaredBuildTool) {
                            println "\n>>> [INFO] Retrieving build file paths using the user declared buildToolDescriptor $declaredBuildTool"
                        }

                        def declaredBuildToolDescriptor = BuildToolDescriptor.getBuildToolDescriptor(declaredBuildTool)
                        project.buildCommander.buildToolDescriptor = declaredBuildToolDescriptor
                    }
                }
            } catch (Exception e) {
                println "\n>>> [ERROR] Error validating and applying build properties from $buildXmlFile:"
                println e.message
                throw new InvalidUserDataException("Build failed due to invalid user configuration in $buildXmlFile.absolutePath", e)
            }
        } else {
            println "\n>>> buildXmlFile is not defined for project, using default settings"
            // Need to do the discovery of build tool
        }

        project.configure(project) {
            def buildToolFinder = new BuildToolFinder()
            def finderResult = buildToolFinder.findBuildTool(project.buildCommander.buildDir, project.buildCommander.buildToolDescriptor)

            project.buildCommander.buildToolDescriptor = finderResult['buildTool']
            project.buildCommander.buildFiles = finderResult['buildFiles']

            println "\n>>> Found Build Tool $project.buildCommander.buildToolDescriptor"
            if (buildXmlFileExists) {
                def buildXmlFile = project.file(project.buildCommander.buildXmlPath)
                def buildXmlParser = new BuildXmlParser()

                def buildProperties = buildXmlParser.getBuildProperties(buildXmlFile.absolutePath, project.buildCommander.buildToolDescriptor)
                project.buildCommander.buildProperties = buildProperties

                def buildPropertiesSummary = ''
                buildProperties.eachWithIndex { prop, i ->
                    buildPropertiesSummary += "${prop.key}=${prop.value}"
                    if (i < buildProperties.size() - 1)
                        buildPropertiesSummary += ", "
                }
                println "\n>>> Using build properties: $buildPropertiesSummary"

                if (buildProperties.containsKey('platform'))
                    project.buildCommander.platform = buildProperties.get('platform')
                if (buildProperties.containsKey('variables'))
                    project.buildCommander.envVars = buildProperties.get('variables')
            }
        }
    }

    /**
     * Checks the build is running on the required OS
     *
     * @param project   the gradle project
     * @param buildTool the detected build tool descriptor
     */
    void checkRequiredOS(Project project) {
        def error, isRequiredPlatform = true
        def buildTool = project.buildCommander.buildToolDescriptor
        def errorGeneral = "the current build will be cancelled, marked as 'Not Built' and a new build will be automatically triggered on the correct OS"

        if (project.buildCommander.platform) {
            if (project.buildCommander.platform == 'win' && !Os.isFamily(Os.FAMILY_WINDOWS)) {
                isRequiredPlatform = false
                error = "your $project.buildCommander.buildXmlPath specifies Windows as the required platform"
            }
            else if (project.buildCommander.platform == 'linux' && !Os.isFamily(Os.FAMILY_UNIX)) {
                isRequiredPlatform = false
                error = "your $project.buildCommander.buildXmlPath specifies Linux as the required platform"
            }
        }
        else if ((buildTool == BuildToolDescriptor.MSBUILD || buildTool == BuildToolDescriptor.VISUALSTUDIO) && !Os.isFamily(Os.FAMILY_WINDOWS)) {
            isRequiredPlatform = false
            error = "it has been detected to be a .Net build which must run on Windows"
        }

        if (!isRequiredPlatform) {
            println "\n>>> [ERROR] This build is not running on the required OS, $error, $errorGeneral"
            throw new GradleException("The detected operating system does not match the one required")
        }
    }
}

/**
 * Configure the parameters within our plugin extension
 */
class BuildCommanderPluginExtension {
    def String      buildDir
    def String      buildXmlPath
    def BuildToolDescriptor buildToolDescriptor
    def Map         buildProperties = [:]
    def List<String>      buildFiles
    def boolean     buildServer
    def boolean     daBuild
    def boolean     codeAnalysis
    def String      buildInfo
    def Map         envVars = [:]
    def String      platform
    def String      archiveName // this is for native build.
}

class BuildCommanderPluginSonarExtension {
    def String      sonarProjectKey
    def String      sonarServer
    def String      sonarUsername
    def String      sonarPassword
    def String      sonarAnalysisSources
    def String      sonarAnalysisTests
    def String      sonarExclusionSources
    def String      sonarExclusionTests
}

class BuildCommanderPluginArtifactoryExtension {
    def String      artifactoryServerUrl
    def String      artifactoryUsername
    def String      artifactoryPassword

}