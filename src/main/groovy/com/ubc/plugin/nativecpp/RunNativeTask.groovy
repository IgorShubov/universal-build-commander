package com.ubc.plugin.nativecpp

import com.ubc.plugin.nativecpp.cmake.CMakePlugin
import com.ubc.plugin.nativecpp.cmake.CMakeTask
import com.ubc.plugin.nativecpp.gnumake.GnuMakeBuild
import com.ubc.plugin.nativecpp.gnumake.GnuMakePlugin
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class RunNativeTask extends DefaultTask {

    @TaskAction
    def RunAction() {
        project.getLogger().info "\n>>> running the task RunNativeTask \n"
        project.getLogger().info "\n>>> invoking the NativeCppPlugin task downloadAllNative()"

        if (!Os.isFamily(Os.FAMILY_UNIX)) {
            println('>>>Native build support using cmake, gcc, and gmake can only be executed in Linux environment.')
            println('>>>Please refer to ubc-schema.xsd on how to configure your build to run on Linux')
            throw new GradleException("Platform mismatch for native build support")
        }

        def downloadAllExternals = project.tasks['downloadAllNative']
        downloadAllExternals.execute()

        if (project.nativePlugin.cmakeProperties) {
            project.apply plugin: CMakePlugin
            def cmake = project.tasks.create(name: "cmake", type: CMakeTask)
            project.cmake {
                if (project.nativePlugin.cmakeProperties) {
                    Map cmakeProperties = project.nativePlugin.cmakeProperties
                    if (cmakeProperts['cmakeExec']) {
                        executable = cmakeProperties['cmakeExec']
                    }
                    if (cmakeProperties['workingDir']) {
                        workingDir += "/${cmakeProperties['workingDir']}"
                    }
                    if (cmakeProperties['sourcePath']) {
                        sourcePath += "/${cmakeProperties['sourcePath']}"
                    }
                    if (cmakeProperties['option']) {
                        option = cmakeProperties['option']
                    }
                }
            }
            cmake.execute()
        }

        project.apply plugin: GnuMakePlugin
        project.gnumake {
            executable = project.nativePlugin.nativeBuildTool ?: 'make'
            makefile = project.nativePlugin.nativeMakefile ?: null
            makeTargets = project.nativePlugin.nativeBuildTargets ?: ""

            workingDir = "$project.projectDir/clean-room-build"
            if (project.nativePlugin.buildDir)
                workingDir += "/${project.nativePlugin.buildDir}"
        }

        def gnumake = project.tasks.create(name: "gnuMakeBuild", type: GnuMakeBuild)
        gnumake.execute()

        if (project.hasProperty('publishConfig')) {
            println("\n>>> Execute PreparePublishNative task")
            project.tasks['preparePublishNative'].execute()
        }
    }
}
