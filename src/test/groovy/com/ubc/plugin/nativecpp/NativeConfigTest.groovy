package com.ubc.plugin.nativecpp

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class NativeConfigTest {

    // Get a Project with the NativeCppPlugin applied
    private static Project getDefaultProject() {
        final Project project = ProjectBuilder.builder().build()
        project.apply plugin: NativeCppPlugin
        return project
    }

    @Test
    void buildDependsAndAdd() {
        def project = getDefaultProject()

        def dependsList = [['name': "art1", 'group': "group1", 'version': "1.5"],
                           ['name': "blah1", 'group': "group2", 'version': "1.6"]]


        dependsList.each() {
            project.nativeConfig.nativeDependencies.add(
                    new NativeDependency(group: it.group, name: it.name, version: it.version))
        }

        List<NativeDependency> depends = project.nativeConfig.nativeDependencies

        assertEquals "have two depends", 2, depends.size

        assertTrue "depends are correct",
                new NativeDependency(group: "group1", name: "art1", version: "1.5").contentEquals(depends.get(0))
    }

    @Test
    void nativeExtCtor() {
        def nativeConfig = new NativeCppPluginExtension()

        assertEquals("Check download dir default", "externals/download", nativeConfig.downloadDir)
        assertEquals("Check install dir default", "externals/install", nativeConfig.installDir)

        nativeConfig.setBuildCommand("cmake")
        assertEquals("check command", "cmake", nativeConfig.buildCommand)
    }
}
