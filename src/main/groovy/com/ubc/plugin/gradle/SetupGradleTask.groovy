package com.ubc.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SetupGradleTask extends DefaultTask {

    @TaskAction
    def SetupAction() {
        if (!project.gradlePlugin.useCurrentGradle) {
            println "\n>>> Extracting Gradle v$project.gradlePlugin.gradleVersion from zip..."
            extractGradle()
        }
        println "\n>>> Setting up Gradle init script...\n"
        setupGradle()
    }

    def extractGradle() {
        project.copy {
            from { project.zipTree { project.gradlePlugin.gradleZip } }
            into { "clean-room-gradle" }
        }
    }

    def setupGradle() {
        // create local gradle cache
        def gradlePluginExt = project.gradlePlugin
        def gradleUserHome =  new File("clean-room-gradle/.gradle").mkdir()

        // Create a file init.gradle to configure the clean-room-build/build.gradle project
        def cloverSnippet = """
        project.apply plugin: 'com.bmuschko.clover'
        project.clover {
            report {
                 html = true
            }
        }
        project.dependencies {
            clover 'com.cenqua.clover:clover:3.2.0'
        }"""

        def credentialsSnippet = (gradlePluginExt.buildServer && !gradlePluginExt.daBuild) ? """
                    username = '${System.getProperty('artyUsername')}'
                    password = '${System.getProperty('artyPassword')}'""" : ''

        def artifactoryPluginSnippet = """
        if (!project.plugins.hasPlugin('artifactory-publish') &&
            !project.plugins.hasPlugin('com.jfrog.artifactory') &&
            !project.plugins.hasPlugin('jpi') &&
            !project.plugins.hasPlugin('com.jfrog.artifactory-upload')) {
            project.apply plugin: 'artifactory-publish'
        }
        """

        def platformNeutralProjectDir = project.projectDir.path.replaceAll("\\\\", "/")
        def sonarPluginSnippet = (gradlePluginExt.runCodeAnalysis) ? """
        allprojects {
            if (rootProject == project) {
                project.apply plugin: 'org.sonarqube'
                project.sonarqube {
                    properties {
                        property 'sonar.host.url', 'http://$gradlePluginExt.sonar.sonarServer/sonar'
                        property 'sonar.login', '$gradlePluginExt.sonar.sonarUsername'
                        property 'sonar.password', '$gradlePluginExt.sonar.sonarPassword'
                        property 'sonar.projectKey', '$gradlePluginExt.sonar.sonarProjectKey'
                        property 'sonar.projectName', '$gradlePluginExt.sonar.sonarProjectKey'
                        property 'sonar.scm.disabled', 'true'
                        property 'sonar.clover.reportPath', '$platformNeutralProjectDir/clean-room-build/build/reports/clover/clover.xml'
                    }
                }
            }
        }
        """ : ''

        def dependencyDeclaration = (gradlePluginExt.staticAnalysis) ? """
            classpath 'org.jfrog.buildinfo:build-info-extractor-gradle:2.2.2',
            'com.bmuschko:gradle-clover-plugin:2.0.1',
            'org.sonarqube.gradle:gradle-sonarqube-plugin:1.0'
        """ : """
            classpath 'org.jfrog.buildinfo:build-info-extractor-gradle:2.2.2',
            'com.bmuschko:gradle-clover-plugin:2.0.1'
        """

        def initFile = """allprojects {

    println \"""Gradle \$gradle.gradleVersion
Gradle home: \$gradle.gradleHomeDir
Gradle user directory: \$gradle.gradleUserHomeDir
Java version: \${System.getProperty("java.version")}, vendor: \${System.getProperty("java.vendor")}
Java home: \${System.getProperty("java.home")}
OS name: \${ System.getProperty("os.name")}, version: \${System.getProperty("os.version")}, arch: \${System.getProperty("os.arch")}
    \"""

    project.buildscript {
        ext.artifactoryContextUrl = '$gradlePluginExt.repoUrl/artifactory'
        repositories {
            maven { url '$gradlePluginExt.repoUrl/artifactory/libs-release'}
        }
        dependencies {
            $dependencyDeclaration
        }
    }
    afterEvaluate { project ->
        if (project.hasProperty('sourceCompatibility') && project.sourceCompatibility == JavaVersion.toVersion("1.8")) {
            println ">>> [WARNING] The build is running using Java 8, if you need an older version of Java set the 'sourceCompatibility' property in your build.gradle file"
        }
        if (!project.plugins.hasPlugin('maven-publish')) {
            project.apply plugin: 'maven-publish'
        }
        $artifactoryPluginSnippet $cloverSnippet $sonarPluginSnippet
        project.repositories {
            maven {
                url '$gradlePluginExt.repoUrl/artifactory/libs-release'
            }
            maven {
                url '$gradlePluginExt.repoUrl/artifactory/libs-snapshot'
            }
        }
        if (project.plugins.hasPlugin('artifactory-publish') || project.plugins.hasPlugin('com.jfrog.artifactory')) {
            project.artifactory {
                publish {
                    contextUrl = '$gradlePluginExt.repoUrl/artifactory'
                    repository {
                        repoKey = project.version.toString().endsWith('-SNAPSHOT') ? 'libs-snapshot-local' : 'libs-release-local'$credentialsSnippet
                    }
                    defaults {
                        publications ('maven')
                    }
                }
            }
        }
        if (project.tasks.findByPath('wrapper') == null) {
            task wrapper(type: Wrapper, description: 'Generates the Gradle wrapper') {
                distributionUrl '$gradlePluginExt.repoUrl/artifactory/ext-release-local/org/gradle/gradle/2.2.1/gradle-2.2.1-bin.zip'
            }
        }
    }
}
"""
        def initGradle = new File("clean-room-gradle/init.gradle").withWriter { writer ->
            writer << initFile
        }
    }
}
