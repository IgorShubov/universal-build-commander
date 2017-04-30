package com.ubc.plugin.nativecpp

import com.ubc.plugin.nativecpp.gnumake.GnuMakePlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder


class NativeGnuMakePluginTest extends GroovyTestCase {

    private static Project getDefaultProject() {
        final Project project = ProjectBuilder.builder().build()
        return project
    }

    void testApplyGnuMakePlugin() {

        def project = getDefaultProject()
        project.extensions.create("tektonNativePlugin", NativePluginExtension)
        project.tektonNativePlugin {
            nativeBuildTool = 'gmake'
            nativeMakefile = 'Makefile'
            nativeBuildTargets = 'compile, test'
            nativeBuildDir = '/myproject'
        }

        project.apply plugin: GnuMakePlugin

        assertEquals(project.tektonNativePlugin.nativeBuildTool, project.gnumake.executable)
        assertEquals(project.tektonNativePlugin.nativeMakefile, project.gnumake.makefile)
        assertEquals("$project.projectDir/clean-room-build/$project.tektonNativePlugin.nativeBuildDir", project.gnumake.workingDir)
        assertEquals(project.tektonNativePlugin.nativeBuildTargets, project.gnumake.makeTargets)
    }
}
