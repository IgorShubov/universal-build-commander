package com.ubc.plugin.nativecpp.cmake

import com.ubc.plugin.nativecpp.NativeBuildSupport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CMakeTask extends DefaultTask {


    @TaskAction
    void execCMake() {
        def cmakeExt = project.extensions.getByName('cmake')
        def cmakeArgs = [ ]
        if (!cmakeExt.option.isEmpty()) {
            cmakeExt.option.tokenize(' ').each {
                cmakeArgs << it
            }
        }
        cmakeArgs << cmakeExt.sourcePath
        println "Execute CMake $cmakeExt.executable on directory $cmakeExt.workingDir with args $cmakeArgs"
        if (cmakeExt.workingDir) {
            def cmakeWorkingDir = new File(cmakeExt.workingDir)
            if (!cmakeWorkingDir.exists()) {
                cmakeWorkingDir.mkdirs()
            }
        }
        def execResult = project.exec {
            environment = NativeBuildSupport.getNativeEnv(project)
            executable = cmakeExt.executable
            workingDir = cmakeExt.workingDir
            args = cmakeArgs

            println("Arguments = ${args}.  Environment = ${environment}.  Executable = ${executable}.  WorkingDir = ${workingDir}")
        }
    }
}
