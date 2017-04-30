package com.ubc.plugin.maven

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class MavenPluginTest {

    private static Project getDefaultProject() {
        final Project project = ProjectBuilder.builder().build()
        project.apply plugin: MavenPlugin
        return project
    }

    @Test
    void hasCorrectConfObj() {
        Project proj = getDefaultProject()
        assertTrue proj.plugins.hasPlugin(MavenPlugin)
        assertNotNull proj.extensions.findByName("mavenPlugin")
    }

    @Test
    void hasCorrectDefaultProps() {
        Project proj = getDefaultProject()
        assertEquals proj.mavenPlugin.mvnVersion, "3.2.3"
        assertEquals proj.mavenPlugin.mvnZip, proj.file("clean-room-maven/maven-${proj.mavenPlugin.mvnVersion}.zip")
        assertEquals proj.mavenPlugin.mvnArgs, ["\"-DskipTests=true\"", "\"-DbuildUser=${System.getProperty('artyUsername') ?: 'n/a'}\"", "\"-DbuildPassword=${System.getProperty('artyPassword') ?: 'n/a'}\""]
        assertEquals proj.mavenPlugin.mvnSettings, proj.file("clean-room-maven/build-settings.xml")
        assertEquals proj.mavenPlugin.protobufExec, "protoc-"
        assertEquals proj.mavenPlugin.envVars, [:]
        assertEquals proj.mavenPlugin.toolchains, [:]
        assertEquals proj.mavenPlugin.buildServer, false
        assertEquals proj.mavenPlugin.daBuild, false
        assertFalse proj.hasProperty('buildCommander') //todo: what happens if buildCommander is not defined in real life
        assertEquals proj.mavenPlugin.mvnUrl, null
        assertEquals proj.mavenPlugin.protobufUrl, null
    }

    @Test
    void hasCorrectBuildProps() {
        Project proj = getDefaultProject()
        assertTrue proj.mavenPlugin.hasProperty('rootPomPath')
        //assertTrue proj.mavenPlugin.rootPomPath.endsWith('/pom.xml')
    }
}
