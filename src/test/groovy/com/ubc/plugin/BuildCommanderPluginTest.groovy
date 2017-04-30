package com.ubc.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class BuildCommanderPluginTest {

    Project createProject() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: BuildCommanderPlugin
        return project
    }


    @Test
    void testCoreProperties() {

        def project = createProject()
        assertNotNull(project)
        project.buildCommander {
            buildDir = "/this/is/a/build/dir"
            buildXmlPath = "/this/is/a/build/dir/build.xml"
        }
        def buildCommanderProps = project.getExtensions().getByName("buildCommander")
        assertNotNull(buildCommanderProps)
        assertEquals("/this/is/a/build/dir", buildCommanderProps.buildDir,)
        assertEquals("/this/is/a/build/dir/build.xml", buildCommanderProps.buildXmlPath,)

        def buildCommanderArty = project.getExtensions().getByName("buildCommanderArtifactory")
        assertNotNull(buildCommanderArty)
        assertNull(buildCommanderArty.artifactoryServerUrl)
        assertNull(buildCommanderArty.artifactoryUsername)
        assertNull(buildCommanderArty.artifactoryPassword)

        def buildCommanderSonar = project.getExtensions().getByName("buildCommanderSonar")
        assertNotNull(buildCommanderSonar)
        assertNull(buildCommanderSonar.sonarProjectKey)
        assertNull(buildCommanderSonar.sonarServer)
        assertNull(buildCommanderSonar.sonarUsername)
        assertNull(buildCommanderSonar.sonarPassword)
        assertNull(buildCommanderSonar.sonarAnalysisSources)
        assertNull(buildCommanderSonar.sonarAnalysisTests)
        assertNull(buildCommanderSonar.sonarExclusionSources)
        assertNull(buildCommanderSonar.sonarExclusionTests)
    }

    /*@Test
    void callConfigureBuild() {
        def project = createProject()

        BuildCommanderPlugin.configureBuild(project)
    }*/
}
