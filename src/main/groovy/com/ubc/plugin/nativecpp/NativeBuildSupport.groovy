package com.ubc.plugin.nativecpp

import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPublicationsPlugin

class NativeBuildSupport {

    static void loadBuildPlugins(Project project) {
        project.apply plugin: 'ivy-publish'
        project.apply plugin: 'artifactory-publish'
    }

    static boolean publishRequired(Project project) {
        return project.nativePlugin.buildServer && !project.nativePlugin.daBuild && project.group != "" && project.version != "unspecified"
    }

    // This method configure the project.gradle.taskGraph.  If a project that needs to
    // publish to artifactory, the task graph execution becomes
    // "runBuildTool->artifactoryPublish->launchBuild"
    // If a project does not publish, the task grap execution becomes
    // "runBuildTool->launchBuild"
    //
    static void configureTaskGraph(Project project) {
        project.task("runBuildTool", type: RunNativeTask, overwrite: true)

        if (publishRequired(project)) {
            project.tasks['artifactoryPublish'].dependsOn('runBuildTool')
            project.task("launchBuild", type: LaunchNativeTask, dependsOn: 'artifactoryPublish')
        } else {
            project.task("launchBuild", type: LaunchNativeTask, dependsOn: 'runBuildTool')
        }

        project.defaultTasks = ['launchBuild']
        project.tasks.launchBuild.finalizedBy('showHelp')
        if (project.nativePlugin.buildServer)
            project.tasks.launchBuild.finalizedBy('getBuildInfoLink')
    }

    static void configureCodeAnalysisTasks(Project project) {
        project.task('sonarConfig', type:SonarRunnerNativeSetupTask)
        project.tasks['sonarConfig'].mustRunAfter(project.tasks['launchBuild'])
        project.task('runCodeQualityScan', type:SonarRunnerNativeExecTask, dependsOn: 'sonarConfig', overwrite: true)
        project.defaultTasks = ['launchBuild', 'runCodeQualityScan']
        project.tasks.launchBuild.finalizedBy('showHelp')
        project.tasks.runCodeQualityScan.finalizedBy('showHelp')
    }


    // The following method declarePublication() is the starting point of the
    // native build.  It sets up publishConfig and nativeConfig for the
    // publication and the dependency of this project.  It also declares the
    // Closure callbacks for IvyPublishing plugin and ArtifactoryPublication plugin
    // so when these plugins were applied to the project, the closure callbacks will
    // be invoked to configure the project's publications and artifactory
    //
    static void declarePublication(Project project) {
        configurePublication(project)
        if (project.hasProperty('publishConfig')) {
            project.plugins.withType(IvyPublishPlugin) {
                project.extensions.configure PublishingExtension, new ClosureBackedAction({
                    println("\n>>> Configuring publication ivyNativePub")
                    publications {
                        ivyNativePub(IvyPublication) {
                            def nativeArtf = project.publishConfig.nativeArtifact
                            def artfFile = "${project.projectDir}/clean-room-build/${project.publishConfig.publishDir}/${nativeArtf.artifactName}"

                            module nativeArtf.name
                            println "Adding $artfFile to the artifact list"
                            artifact(project.file(artfFile))
                            descriptor.withXml {
                                asNode().info[0].attributes().put('aol', nativeArtf.aol)
                                project.nativeConfig.nativeDependencies.each { NativeDependency nativeDep ->
                                    asNode().dependencies[0].appendNode('dependency', nativeDep.asIvyDependencyMap())
                                }
                            }
                        }
                    }
                })
            }
        }

        project.plugins.withType(ArtifactoryPublicationsPlugin) {
            if (project.hasProperty('publishConfig')) {
                project.artifactoryPublish {
                    publications('ivyNativePub')
                }
                configureArtifactory(project)
            }
        }
    }

    static void configureArtifactory(Project project) {

        if (project.hasProperty('publishConfig')) {
            println("\n>>> Configuring artifactory URL, RepoKey, Login, and Layout")
            project.artifactory {
                contextUrl = project.nativePlugin.artifactory.artifactoryServerUrl
                publish {
                    repository {
                        repoKey = project.version.toString().endsWith('-SNAPSHOT') ? 'native-libs-snapshot-local' : 'native-libs-release-local'
                        username = project.nativePlugin.artifactory.artifactoryUsername
                        password = project.nativePlugin.artifactory.artifactoryPassword

                        ivy {
                            def nativeArtf = project.publishConfig.nativeArtifact
                            def layout = "[organization]/${nativeArtf.name}/[revision]/${nativeArtf.name}-[revision].[ext]"
                            if (!StringUtils.isEmpty(nativeArtf.aol) && !StringUtils.isEmpty(nativeArtf.classifier)) {
                                layout = "[organization]/${nativeArtf.name}/[revision]/${nativeArtf.aol}/${nativeArtf.name}-[revision]-${nativeArtf.aol}-${nativeArtf.classifier}.[ext]"
                            } else if (StringUtils.isEmpty(nativeArtf.aol)) {
                                layout = "[organization]/${nativeArtf.name}/[revision]/${nativeArtf.name}-[revision]-${nativeArtf.classifier}.[ext]"
                            } else if (StringUtils.isEmpty(nativeArtf.classifier)) {
                                layout = "[organization]/${nativeArtf.name}/[revision]/${nativeArtf.aol}/${nativeArtf.name}-[revision]-${nativeArtf.aol}.[ext]"
                            }

                            artifactLayout = layout
                        }
                    }
                    defaults {
                        publishPom = false
                    }
                }
            }
        }
    }

    static void configurePublication(Project project) {
        println("\n>>> Configuring the publication ")
        if (publishRequired(project)) {
            project.apply plugin: ConfigurePublishNativePlugin
            setupConfigurePublishNativePlugin(project)
        }
        project.apply plugin: NativeCppPlugin
        setupNativeCppPlugin(project)
    }

    static void setupNativeCppPlugin(Project project) {
        project.nativeConfig.repository = "${project.nativePlugin.artifactory.artifactoryServerUrl}/libs-release"
        project.nativeConfig.snapshotRepository = "${project.nativePlugin.artifactory.artifactoryServerUrl}/libs-snapshot"
        project.nativeConfig.artifactoryDownloadUsername = project.nativePlugin.artifactory.artifactoryUsername
        project.nativeConfig.artifactoryDownloadPassword = project.nativePlugin.artifactory.artifactoryPassword

        Map nativeConfiguration = project.nativePlugin.nativeConfiguration

        /*
         do something like this
         nativeConfig.downloadDir = "someDir"
         nativeConfig.installDir = "someOtherDir"
         project.nativeConfig.nativeDependencies.add(
                new NativeDependency(group: "com.google", name: "protobuf",
                        version: "2.4.1", aol: "x86_32-csl2-gcc41", ext: "tgz")
         )
        */
        if (nativeConfiguration) {
            if (nativeConfiguration['downloadDir'] != null) {
                project.nativeConfig.downloadDir = nativeConfiguration['downloadDir']
            }
            if (nativeConfiguration['installDir'] != null) {
                project.nativeConfig.installDir = nativeConfiguration['installDir']
            }
            def extDependencyDeclarations = nativeConfiguration['externalDependencies']
            def defaultAOL = project.nativePlugin.dependencyDefaultAol
            if (extDependencyDeclarations != null) {
                extDependencyDeclarations.each { declaration ->
                    def dependencyInstallDir = project.nativeConfig.installDir
                    if (declaration['installDir'] != null) {
                        dependencyInstallDir = declaration['installDir']
                    }
                    if (declaration['dependency'] != null) {
                        String dep = declaration['dependency']
                        String[] depDeclaration = dep.tokenize(':')
                        // depDeclaration will be in the form of
                        // groupId:artifactId:version:aol:[classifier]:ext
                        switch(depDeclaration.size()) {
                            case 6:
                                project.nativeConfig.nativeDependencies.add(
                                        new NativeDependency(
                                                group: depDeclaration[0],
                                                name: depDeclaration[1],
                                                version: depDeclaration[2],
                                                aol: depDeclaration[3],
                                                classifier: depDeclaration[4],
                                                ext: depDeclaration[5],
                                                installDir: dependencyInstallDir
                                        )
                                )
                                break
                            case 5:
                                project.nativeConfig.nativeDependencies.add(
                                        new NativeDependency(
                                                group: depDeclaration[0],
                                                name: depDeclaration[1],
                                                version: depDeclaration[2],
                                                aol: depDeclaration[3],
                                                ext: depDeclaration[4],
                                                installDir: dependencyInstallDir
                                        )
                                )
                                break
                            case 4:
                                project.nativeConfig.nativeDependencies.add(
                                        new NativeDependency(
                                                group: depDeclaration[0],
                                                name: depDeclaration[1],
                                                version: depDeclaration[2],
                                                aol: defaultAOL,
                                                ext: depDeclaration[3],
                                                installDir: dependencyInstallDir
                                        )
                                )
                                break
                            default:
                                println("\n WARN: Invalid dependency declaration: $dep")
                        }
                    } else {
                        def depAOL = declaration['aol'] ?: defaultAOL
                        project.nativeConfig.nativeDependencies.add(
                                new NativeDependency(
                                        group: declaration['groupId'],
                                        name: declaration['artifactId'],
                                        version: declaration['version'],
                                        aol: depAOL,
                                        classifier: declaration['classifier'],
                                        ext: declaration['ext'],
                                        installDir: dependencyInstallDir
                                )
                        )
                    }
                }
            }
        }
    }

    static void setupConfigurePublishNativePlugin(Project project) {
        project.publishConfig.archiveName = project.nativePlugin.archiveName

        def pub = project.nativePlugin.nativeConfiguration['publication'] // pub is from test-build.xml
        def artf = new NativeArtifact()
        artf.group = project.group
        artf.name = pub.artifact ?: project.nativePlugin.archiveName ?: project.name
        if (pub.artifact == null) {
            project.publishConfig.publishEntireTargetDir = true
        }
        artf.version = project.version
        artf.classifier = pub.classifier
        if (project.nativePlugin.aol) {
            project.publishConfig.aol = artf.aol = project.nativePlugin.aol
        }

        artf.ext = pub.ext ?: 'tgz'
        artf.targetDir = pub.targetDir ?: '.'

        project.publishConfig.nativeArtifact = artf
        project.publishConfig.packageContext = pub.packageContext

        if (project.nativePlugin.nativeConfiguration['publishDir'] != null)
            project.publishConfig.publishDir = project.nativePlugin.nativeConfiguration['publishDir']

        println ">>>> Added ${artf.artifactName} to publish configuration"

        project.task('preparePublishNative', type: PreparePublishNativeTask) {
            description "copy the native artifact to be published to ${project.publishConfig.publishDir}"
        }
    }

    static Map<String, String> getNativeEnv(Project project) {
        def environment = ["PATH" : "/usr/local/bin:/bin:/usr/bin"]
        if (project.nativePlugin.envVars) {
            environment << project.nativePlugin.envVars
            if (project.nativePlugin.envVars["PATH"] != null) {
                environment["PATH"] = "/usr/local/bin:/bin:/usr/bin:" + project.nativePlugin.envVars["PATH"]
            }
        }
        if (project.nativePlugin.compilerVersion) {
            environment["PATH"] = "/cs/cxxtools/${project.nativePlugin.compilerVersion}-1/bin/:${environment["PATH"]}"
            environment["CC"] = "/cs/cxxtools/${project.nativePlugin.compilerVersion}-1/bin/gcc"
            environment["CXX"] = "/cs/cxxtools/${project.nativePlugin.compilerVersion}-1/bin/g++"
            environment["LD_LIBRARY_PATH"] = "/cs/cxxtools/${project.nativePlugin.compilerVersion}-1/lib64"
        }
        environment
    }
}
