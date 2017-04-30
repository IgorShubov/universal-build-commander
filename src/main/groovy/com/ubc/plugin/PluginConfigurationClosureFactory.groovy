package com.ubc.plugin

import com.ubc.BuildToolDescriptor

class PluginConfigurationClosureFactory {
    static Closure createMavenConfigClosure() {
        def configClosure = { project ->
            project.mavenPlugin {
                artifactory.artifactoryServerUrl = project.buildCommanderArtifactory.artifactoryServerUrl
                artifactory.artifactoryUsername = project.buildCommanderArtifactory.artifactoryUsername
                artifactory.artifactoryPassword = project.buildCommanderArtifactory.artifactoryPassword

                envVars = project.buildCommander.envVars

                if (project.buildCommander.buildFiles && project.buildCommander.buildFiles.size() > 0) {
                    rootPomPath = project.buildCommander.buildFiles.get(0)
                }

                def buildProperties = project.buildCommander.buildProperties
                if (buildProperties.containsKey('version'))
                    mvnVersion = buildProperties.get('version')
                if (buildProperties.containsKey('args'))
                    mvnArgs << buildProperties.get('args').flatten()
                if (buildProperties.containsKey('opts'))
                    mvnOpts = buildProperties.get('opts')
                if (buildProperties.containsKey('rootPom')) {
                    rootPomPath = "${project.buildCommander.buildDir}/${buildProperties.get('rootPom')}/pom.xml"
                    mvnArgs.add('\"-f' + buildProperties.get('rootPom') + '/pom.xml\"')
                    buildInfo = project.buildCommander.buildInfo.replace("target", "${buildProperties.get('rootPom')}/target")
                    project.buildCommander.buildInfo = buildInfo
                }
                if (buildProperties.containsKey('toolchains')) {
                    toolchains = buildProperties.get('toolchains')
                }
                buildDir = project.file("${rootPomPath}/..").absoluteFile
                mvnUrl = "$project.buildCommanderArtifactory.artifactoryServerUrl/libs-release/org/apache/maven/apache-maven/$project.mavenPlugin.mvnVersion/apache-maven-$project.mavenPlugin.mvnVersion-bin.zip"
                daBuild = project.buildCommander.daBuild
                buildServer = project.buildCommander.buildServer
                runCodeAnalysis = project.buildCommander.codeAnalysis

                sonar.sonarProjectKey = project.buildCommanderSonar.sonarProjectKey
                sonar.sonarUsername   = project.buildCommanderSonar.sonarUsername
                sonar.sonarPassword   = project.buildCommanderSonar.sonarPassword
                sonar.sonarServer     = project.buildCommanderSonar.sonarServer

                if (buildProperties) {
                    if (buildProperties.containsKey('analysis-sources')) {
                        sonar.sonarAnalysisSources = buildProperties.get('analysis-sources')
                    }
                    if (buildProperties.containsKey('analysis-sources-exclusions')) {
                        sonar.sonarExclusionSources = buildProperties.get('analysis-sources-exclusions')
                    }
                    if (buildProperties.containsKey('analysis-tests')) {
                        sonar.sonarAnalysisTests = buildProperties.get('analysis-tests')
                    }
                    if (buildProperties.containsKey('analysis-tests-exclusions')) {
                        sonar.sonarExclusionTests = buildProperties.get('analysis-tests-exclusions')
                    }
                }
                mvnZip = project.file("clean-room-maven/maven-${project.mavenPlugin.mvnVersion}.zip")
            }
        }
        configClosure
    }

    static Closure createGradleConfigClosure() {

        def configClosure = { project ->
            project.gradlePlugin {
                buildServer = project.buildCommander.buildServer
                daBuild = project.buildCommander.daBuild
                envVars = project.buildCommander.envVars

                artifactory.artifactoryServerUrl = project.buildCommanderArtifactory.artifactoryServerUrl
                artifactory.artifactoryUsername = project.buildCommanderArtifactory.artifactoryUsername
                artifactory.artifactoryPassword = project.buildCommanderArtifactory.artifactoryPassword

                gradleBuildFile = project.buildCommander.buildFiles.get(0)

                def buildProperties = project.buildCommander.buildProperties
                if (buildProperties.containsKey('version')) {
                    gradleVersion = buildProperties.get('version')
                }

                def currentGradleVersion = project.gradle.gradleVersion
                useCurrentGradle = (!buildServer && gradleVersion.equals(currentGradleVersion))

                if (buildProperties.containsKey('systemProperties'))
                    gradleSystemProperties = buildProperties.get('systemProperties')
                if (buildProperties.containsKey('projectProperties'))
                    gradleProjectProperties = buildProperties.get('projectProperties')
                if (buildProperties.containsKey('opts'))
                    gradleOpts = buildProperties.get('opts')
                if (buildProperties.containsKey('args'))
                    gradleArgs = buildProperties.get('args').flatten()
                if (buildProperties.containsKey('tasks'))
                    gradleTasks = buildProperties.get('tasks')

                project.buildCommander.buildInfo = "clean-room-build/build/build-info.json"

                if (buildProperties.containsKey('buildFile')) {
                    gradleBuildFile = project.file("clean-room-build").path + "/" + buildProperties.get('buildFile')
                    int lastSlash = buildProperties.get('buildFile').lastIndexOf('/')
                    if (lastSlash != -1) {
                        def buildFileDir = buildProperties.get('buildFile').substring(0, lastSlash)
                        project.buildCommander.buildInfo = "clean-room-build/$buildFileDir/build/build-info.json"
                    }
                }
                if (buildProperties.containsKey('settingsFile')) {
                    gradleSettingsFile = buildProperties.get('settingsFile')
                }
                gradleUrl = "$project.buildCommanderArtifactory.artifactoryUrl/libs-release/org/gradle/gralde/$gradleVersion/gradle-${gradleVersion}-bin.zip"
                gradleZip = project.file("clean-room-gradle/gradle-${gradleVersion}.zip")
                runCodeAnalysis = project.buildCommander.codeAnalysis

                sonar.sonarProjectKey = project.buildCommanderSonar.sonarProjectKey
                sonar.sonarUsername   = project.buildCommanderSonar.sonarUsername
                sonar.sonarPassword   = project.buildCommanderSonar.sonarPassword
                sonar.sonarServer     = project.buildCommanderSonar.sonarServer

                if (buildProperties) {
                    if (buildProperties.containsKey('analysis-sources')) {
                        sonar.sonarAnalysisSources = buildProperties.get('analysis-sources')
                    }
                    if (buildProperties.containsKey('analysis-sources-exclusions')) {
                        sonar.sonarExclusionSources = buildProperties.get('analysis-sources-exclusions')
                    }
                    if (buildProperties.containsKey('analysis-tests')) {
                        sonar.sonarAnalysisTests = buildProperties.get('analysis-tests')
                    }
                    if (buildProperties.containsKey('analysis-tests-exclusions')) {
                        sonar.sonarExclusionTests = buildProperties.get('analysis-tests-exclusions')
                    }
                }
            }
        }
        configClosure
    }

    static Closure createMsBuildConfigClosure() {

        def configClosure = { project ->
            project.msBuildPlugin {
                buildServer = project.buildCommander.buildServer
                daBuild = project.buildCommander.daBuild


                artifactory.artifactoryServerUrl = project.buildCommanderArtifactory.artifactoryServerUrl
                artifactory.artifactoryUsername = project.buildCommanderArtifactory.artifactoryUsername
                artifactory.artifactoryPassword = project.buildCommanderArtifactory.artifactoryPassword

                nugetUrl = "$artifactory.artifactoryServerUrl/libs-release/NuGet.CommandLine.${nugetVersion}.nupkg"
                nugetRepo = "$artifactory.artifactoryServerUrl/api/nuget/libs-release"
                nunitUrl = "$artifactory.artifactoryServerUrl/libs-release/NUnit.Runners.${nunitVersion}.nupkg"
                openCoverUrl = "$artifactory.artifactoryServerUrl/libs-release/OpenCover.${openCoverVersion}.nupkg"
                openCoverCoberturaUrl = "$artifactory.artifactoryServerUrl/libs-release/OpenCoverToCoberturaConverter.${openCoverCoberturaVersion}.nupkg"
                sonarRunnerUrl = "$artifactory.artifactoryServerUrl/libs-release/MSBuild.SonarQube.Runner.${sonarRunnerVersion}.nupkg"

                def buildProperties = project.buildCommander.buildProperties
                if (project.buildCommander.buildToolDescriptor == BuildToolDescriptor.VISUALSTUDIO) {
                    if (buildProperties.containsKey('projectFile'))
                        projectFile = buildProperties.get('projectFile').trim()
                    if (buildProperties.containsKey('projectName'))
                        projectName = buildProperties.get('projectName')
                    if (buildProperties.containsKey('projectVersion'))
                        projectVersion = buildProperties.get('projectVersion')
                    if (buildProperties.containsKey('verbosity'))
                        verbosity = buildProperties.get('verbosity')
                    if (buildProperties.containsKey('targets'))
                        targets = buildProperties.get('targets')
                    if (buildProperties.containsKey('releaseTargets'))
                        releaseTargets = buildProperties.get('releaseTargets')
                    if (buildProperties.containsKey('parameters'))
                        parameters = buildProperties.get('parameters')
                    if (buildProperties.containsKey('releaseParameters'))
                        releaseParameters = buildProperties.get('releaseParameters')
                    if (buildProperties.containsKey('visualCpp')) {
                        visualCpp = true
                        if (buildProperties.visualCpp.containsKey('sdk'))
                            sdk = buildProperties.visualCpp.get('sdk')
                        else
                            sdk = "7.1"
                        if (buildProperties.visualCpp.containsKey('configuration'))
                            configuration = buildProperties.visualCpp.get('configuration')
                        if (buildProperties.visualCpp.containsKey('platform'))
                            platform = buildProperties.visualCpp.get('platform')
                        if (buildProperties.visualCpp.containsKey('architecture'))
                            architecture = buildProperties.visualCpp.get('architecture')
                    }
                }
                buildFiles = project.buildCommander.buildFiles
                sonar.sonarProjectKey = project.buildCommanderSonar.sonarProjectKey
                sonar.sonarServer = project.buildCommanderSonar.sonarServer
                sonar.sonarUsername = project.buildCommanderSonar.sonarUsername
                sonar.sonarPassword = project.buildCommanderSonar.sonarPassword

                runCodeAnalysis = project.buildCommander.codeAnalysis
            }
        }
        configClosure
    }

    static Closure createNativeConfigClosure() {

        def configClosure = { project ->
            project.nativePlugin {
                archiveName = project.buildCommander.archiveName
                runCodeAnalysis = project.buildCommander.codeAnalysis
                envVars = project.buildCommaner.envVars
                nativeMakefile = project.buildCommander.buildFiles.get(0)
                buildServer = project.buildCommander.buildServer
                daBuild = project.buildCommander.daBuild

                artifactory.artifactoryServerUrl = project.buildCommanderArtifactory.artifactoryServerUrl
                artifactory.artifactoryUsername = project.buildCommanderArtifactory.artifactoryUsername
                artifactory.artifactoryPassword = project.buildCommanderArtifactory.artifactoryPassword

                sonar.sonarServer = project.buildCommanderSonar.sonarServer
                sonar.sonarUsername = project.buildCommanderSonar.sonarUsername
                sonar.sonarPassword = project.buildCommanderSonar.sonarPassword
                sonar.sonarProjectKey = project.buildCommanderSonar.sonarProjectKey

                def buildProperties = project.buildCommander.buildProperties
                if (buildProperties) {
                    if (buildProperties.containsKey('makeExec')) {
                        nativeBuildTool = buildProperties['makeExec']
                    }
                    if (buildProperties.containsKey('makeTargets')) {
                        nativeBuildTargets = buildProperties['makeTargets']
                    }
                    if (buildProperties.containsKey('workingDir')) {
                        nativeBuildDir = buildProperties['workingDir']
                    }
                    if (buildProperties.containsKey('makefile')) {
                        def makefile = buildProperties['makefile']
                        nativeMakefile = "$project.projectDir/clean-room-build/$makefile"
                    }
                    if (buildProperties.containsKey('cmake')) {
                        cmakeProperties = buildProperties['cmake']
                    }
                    if (buildProperties.containsKey('compiler')) {
                        compilerVersion = buildProperties['compiler']
                    }
                    if (buildProperties.containsKey('nativeConfiguration')) {
                        nativeConfiguration = buildProperties.get('nativeConfiguration')
                    }
                    if (buildProperties.containsKey('aol')) {
                        aol = buildProperties.get('aol')
                    }
                    if (buildProperties.containsKey('dependencyDefaultAol')) {
                        dependencyDefaultAol = buildProperties.get('dependencyDefaultAol')
                    }
                    if (buildProperties.containsKey('groupId')) {
                        project.setGroup(buildProperties.get('groupId'))
                    }
                    if (buildProperties.containsKey('version')) {
                        project.setVersion(buildProperties.get('version'))
                    }

                    if (buildProperties.containsKey('analysis-sources')) {
                        sonar.sonarAnalysisSources = buildProperties.get('analysis-sources')
                    }
                    if (buildProperties.containsKey('analysis-sources-exclusions')) {
                        sonar.sonarExclusionSources = buildProperties.get('analysis-sources-exclusions')
                    }
                    if (buildProperties.containsKey('analysis-tests')) {
                        sonar.sonarAnalysisTests = buildProperties.get('analysis-tests')
                    }
                    if (buildProperties.containsKey('analysis-tests-exclusions')) {
                        sonar.sonarExclusionTests = buildProperties.get('analysis-tests-exclusions')
                    }
                }
            }
        }
        configClosure
    }
}
