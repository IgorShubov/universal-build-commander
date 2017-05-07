package com.ubc.plugin.nativecpp

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertEquals

public class NativeCppPluginTest {

    // Get a Project with the NativeCppPlugin applied
    private static Project getDefaultProject() {
        final Project project = ProjectBuilder.builder().build()
        project.apply plugin: NativePlugin
        project.apply plugin: NativeCppPlugin
        return project
    }

    @Test
    void downloadUnpack() {
        def project = getDefaultProject()

        project.nativeConfig.nativeDependencies.add(
                new NativeDependency(group: "com.google", name: "protobuf",
                        version: "2.4.1", aol: "x86_32-csl2-gcc41", ext: "tgz")
        )
        project.tasks.downloadAllNative.execute()
    }

    @Test
    void downloadDontUnpack() {
        def project = getDefaultProject()

        project.nativeConfig.nativeDependencies.add(
                new NativeDependency(group: "com.google", name: "protobuf",
                        version: "2.4.1", aol: "x86_32-csl2-gcc41", ext: "tgz"))

        project.nativeConfig.nativeDependencies.add(
                new NativeDependency(group: "net.zlib", name: "zlib",
                        version: "1.2.8", aol: "x86_64-csl2-gcc41", ext: "tgz"))

        project.nativeConfig.unpack = false
        project.tasks.downloadAllNative.execute()
    }

    @Test
    void declareDependency() {
        final Project project = ProjectBuilder.builder().build()
        project.extensions.create("nativePlugin", NativePluginExtension)

        project.nativePlugin {
            nativeConfiguration = [:]
            nativeConfiguration.put('externalDependencies', new ArrayList<Map>())
            def dependency1 = ['groupId': 'com.ubc.test',
                               'artifactId': 'testutil',
                               'version': '1.0',
                               'aol'    : 'linux64_gcc',
                               'ext'    : 'tgz']
            def dependency2 = ['groupId': 'org.apache.commons',
                               'artifactId': 'apache-commons',
                               'version': '2.1',
                               'aol'    : 'linux64_gcc_csl2',
                               'classifier': 'debug',
                               'ext'    : 'tgz']
            nativeConfiguration['externalDependencies'].add(dependency1)
            nativeConfiguration['externalDependencies'].add(dependency2)
        }
        project.apply plugin: NativeCppPlugin


        assertEquals(2, project.nativeConfig.nativeDependencies.size())
        assertEquals("com/ubc/test/testutil/1.0/linux64_gcc/testutil-1.0-linux64_gcc.tgz",
                project.nativeConfig.nativeDependencies[0].getURLPath())
        assertEquals("org/apache/commons/apache-commons/2.1/linux64_gcc_csl2/apache-commons-2.1-linux64_gcc_csl2-debug.tgz",
                project.nativeConfig.nativeDependencies[1].getURLPath())
    }

    @Test
    void declareShortFormDependency() {
        final Project project = ProjectBuilder.builder().build()
        project.extensions.create("nativePlugin", NativePluginExtension)
        project.nativePlugin {
            nativeConfiguration = [:]
            nativeConfiguration.put('externalDependencies', new ArrayList<Map>())
            def dependency1 = ['dependency': 'com.ubc.test:testutil:1.0:linux64_gcc:tgz']
            def dependency2 = ['dependency': 'org.apache.commons:apache-commons:2.1:linux64_gcc_csl2:debug:tgz']
            nativeConfiguration['externalDependencies'].add(dependency1)
            nativeConfiguration['externalDependencies'].add(dependency2)
        }
        project.apply plugin: NativeCppPlugin

        assertEquals(2, project.nativeConfig.nativeDependencies.size())
        assertEquals("com/ubc/test/testutil/1.0/linux64_gcc/testutil-1.0-linux64_gcc.tgz",
                project.nativeConfig.nativeDependencies[0].getURLPath())
        assertEquals("org/apache/commons/apache-commons/2.1/linux64_gcc_csl2/apache-commons-2.1-linux64_gcc_csl2-debug.tgz",
                project.nativeConfig.nativeDependencies[1].getURLPath())

    }

    @Test
    void declareDependencyDefaultAol() {
        final Project project = ProjectBuilder.builder().build()
        project.extensions.create("nativePlugin", NativePluginExtension)
        project.nativePlugin {
            dependencyDefaultAol = 'linux64_csl3_gcc'
            nativeConfiguration = [:]
            nativeConfiguration.put('externalDependencies', new ArrayList<Map>())
            def dependency1 = ['groupId'   : 'com.ubc.test',
                               'artifactId': 'testutil',
                               'version'   : '1.0',
                               'ext'       : 'tgz']
            def dependency2 = ['groupId'   : 'org.apache.commons',
                               'artifactId': 'apache-commons',
                               'version'   : '2.1',
                               'classifier': 'debug',
                               'ext'       : 'tgz']
            def dependency3 = ['dependency' : 'org.log4j:log4j-api:1.2.8:jar']

            nativeConfiguration['externalDependencies'].add(dependency1)
            nativeConfiguration['externalDependencies'].add(dependency2)
            nativeConfiguration['externalDependencies'].add(dependency3)
        }
        project.apply plugin: NativeCppPlugin

        assertEquals(3, project.nativeConfig.nativeDependencies.size())
        assertEquals("com/ubc/test/testutil/1.0/linux64_csl3_gcc/testutil-1.0-linux64_csl3_gcc.tgz",
                project.nativeConfig.nativeDependencies[0].getURLPath())
        assertEquals("org/apache/commons/apache-commons/2.1/linux64_csl3_gcc/apache-commons-2.1-linux64_csl3_gcc-debug.tgz",
                project.nativeConfig.nativeDependencies[1].getURLPath())
        assertEquals("org/log4j/log4j-api/1.2.8/linux64_csl3_gcc/log4j-api-1.2.8-linux64_csl3_gcc.jar",
                project.nativeConfig.nativeDependencies[2].getURLPath())
    }

    @Test
    void declareDependencyNoDefaultAol() {
        final Project project = ProjectBuilder.builder().build()
        project.extensions.create("nativePlugin", NativePluginExtension)
        project.nativePlugin {
            aol = 'linux64_csl3_gcc'
            nativeConfiguration = [:]
            nativeConfiguration.put('externalDependencies', new ArrayList())
            def dependency1 = ['groupId'   : 'com.ubc.test',
                               'artifactId': 'testutil',
                               'version'   : '1.0',
                               'ext'       : 'tgz']
            def dependency2 = ['groupId'   : 'org.apache.commons',
                               'artifactId': 'apache-commons',
                               'version'   : '2.1',
                               'classifier': 'debug',
                               'ext'       : 'tgz']
            def dependency3 = ['dependency' : 'org.log4j:log4j-api:1.2.8:jar']
            nativeConfiguration['externalDependencies'].add(dependency1)
            nativeConfiguration['externalDependencies'].add(dependency2)
            nativeConfiguration['externalDependencies'].add(dependency3)
        }
        project.apply plugin: NativeCppPlugin

        assertEquals(3, project.nativeConfig.nativeDependencies.size())
        assertEquals("com/ubc/test/testutil/1.0/testutil-1.0.tgz",
                project.nativeConfig.nativeDependencies[0].getURLPath())
        assertEquals("org/apache/commons/apache-commons/2.1/apache-commons-2.1-debug.tgz",
                project.nativeConfig.nativeDependencies[1].getURLPath())
        assertEquals("org/log4j/log4j-api/1.2.8/log4j-api-1.2.8.jar",
                project.nativeConfig.nativeDependencies[2].getURLPath())
    }

    @Test
    void declareShortFormDependencyDefaultAol() {
        final Project project = ProjectBuilder.builder().build()
        project.extensions.create("nativePlugin", NativePluginExtension)
        project.nativePlugin {
            aol = 'linux64_csl3_gcc'
            nativeConfiguration = [:]
            nativeConfiguration.put('externalDependencies', new ArrayList<Map>())
            def dependency1 = ['dependency': 'com.ubc.test:testutil:1.0:tgz']
            // with short form, we don't support default AOL with classifier
            def dependency2 = ['dependency': 'org.apache.commons:apache-commons:2.1:linux64_csl3_gcc:debug:tgz']
            nativeConfiguration['externalDependencies'].add(dependency1)
            nativeConfiguration['externalDependencies'].add(dependency2)
        }
        project.apply plugin: NativeCppPlugin

        assertEquals(2, project.nativeConfig.nativeDependencies.size())
        assertEquals("com/ubc/test/testutil/1.0/testutil-1.0.tgz",
                project.nativeConfig.nativeDependencies[0].getURLPath())
        assertEquals("org/apache/commons/apache-commons/2.1/linux64_csl3_gcc/apache-commons-2.1-linux64_csl3_gcc-debug.tgz",
                project.nativeConfig.nativeDependencies[1].getURLPath())

    }
}
