package com.ubc.plugin.nativecpp

import com.ubc.plugin.BuildCommanderPluginExtension
import org.gradle.api.Project
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*
import static org.testng.Assert.assertFalse

public class NativePublishTest {

    @Test
    public void publishConfiguration() {

        def project = ProjectBuilder.builder().build()
        addPublicationConfig(project)
        project.apply plugin: ConfigurePublishNativePlugin

        assertNotNull(project.extensions.findByType(PublishConfiguration))

        def expectedPublishConfig = new PublishConfiguration( publishDir: 'pubDir',
                                                              nativeArtifact: new NativeArtifact(group: 'com.ubc',
                                                                                                 name: 'libXyz',
                                                                                                 version: '1.1.0',
                                                                                                 aol: 'x86-64-linux-gcc',
                                                                                                 classifier: 'debug',
                                                                                                 ext: 'tgz',
                                                                                                 targetDir: 'output'))

        assertEquals(expectedPublishConfig.publishDir, project.publishConfig.publishDir)
        assertEquals(expectedPublishConfig.nativeArtifact.artifactName, project.publishConfig.nativeArtifact.artifactName)
        assertEquals(expectedPublishConfig.nativeArtifact.targetDir, project.publishConfig.nativeArtifact.targetDir)
        assertEquals(expectedPublishConfig.nativeArtifact.group, project.publishConfig.nativeArtifact.group)
    }

    @Test
    public void publishWholeDirConfiguration() {
        def project = ProjectBuilder.builder().build()
        project.setGroup("com.ubc")
        project.setVersion("1.1.0")
        project.extensions.create("universalNativePlugin", NativePluginExtension)
        project.extensions.create("buildCommander", BuildCommanderPluginExtension)
        project.buildCommander {
            archiveName = "pubTest"
        }
        project.tektonNativePlugin {
            aol = "x86-64-linux-gcc"
            nativeConfiguration = [
                'publishDir' : 'pubDir',
                'publication': [
                    'classifier'    : 'debug',
                    'targetDir'     : 'output',
                    'packageContext': 'fsw'
                ]
            ]
        }
        NativeBuildSupport.configurePublication(project)

        assertTrue(project.publishConfig.publishEntireTargetDir)
        assertEquals('com.ubc', project.publishConfig.nativeArtifact.group)
        assertEquals('pubTest', project.publishConfig.nativeArtifact.name)
        assertEquals('1.1.0', project.publishConfig.nativeArtifact.version)
        assertEquals('x86-64-linux-gcc', project.publishConfig.nativeArtifact.aol)
        assertEquals('debug', project.publishConfig.nativeArtifact.classifier)
        assertEquals('tgz', project.publishConfig.nativeArtifact.ext)
        assertEquals('output', project.publishConfig.nativeArtifact.targetDir)
        assertEquals('fsw', project.publishConfig.packageContext)
    }

    @Test
    public void configureTaskGraphWithPublication() {
        def project = ProjectBuilder.builder().build()
        project.extensions.create("universalNativePlugin", NativePluginExtension)
        project.universalNativePlugin {
            buildServer = true
            daBuild = false
        }

        project.apply plugin: 'artifactory-publish'
        NativeBuildSupport.configureTaskGraph(project)

        def taskNames = project.tasks.collect{ it.name }
        assertTrue(taskNames.contains("artifactoryPublish"))
        assertTrue(taskNames.contains("launchBuild"))
        assertTrue(taskNames.contains("runBuildTool"))
        assertFalse(taskNames.contains("setupBuildTool"))
        assertFalse(taskNames.contains("downloadBuildTool"))
    }

    @Test
    public void applyIvyPublishPlugin() {

        def project = ProjectBuilder.builder().build()
        addPublicationConfig(project)
        project.extensions.create("buildCommander", BuildCommanderPluginExtension)
        project.buildCommander.repoUrl = "https://dl.bintray.com/"
        NativeBuildSupport.declarePublication(project)
        NativeBuildSupport.loadBuildPlugins(project)

        def pub = project.publishing.publications
        assertEquals(1, pub.size())

        IvyPublication ivyPub = pub.getByName('ivyNativePub')
        assertNotNull(ivyPub)
        assertEquals(1, ivyPub.getArtifacts().size())
        assertNotNull(project.tasks.findByPath("artifactoryPublish"))
    }

    private void addPublicationConfig(Project project) {
        project.setGroup("com.ubc")
        project.setVersion("1.1.0")
        project.extensions.create("universalNativePlugin", NativePluginExtension)
        project.universalNativePlugin {

            aol = "x86-64-linux-gcc"
            nativeConfiguration = ['publishDir' : 'pubDir',
                                   'publication': ['classifier': 'debug', 'ext': 'tgz', 'artifact': 'libXyz', 'targetDir': 'output']
            ]
        }
    }
}
