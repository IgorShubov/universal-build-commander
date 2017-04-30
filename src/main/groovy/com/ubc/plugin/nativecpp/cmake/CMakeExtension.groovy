package com.ubc.plugin.nativecpp.cmake

import org.gradle.api.Project

/**
 * This class encapsulates parameters to build up the cmake command
 */
class CMakeExtension {

    String executable = 'cmake'
    String workingDir
    String sourcePath
    String option = ''

    public CMakeExtension(Project project) {
        workingDir = "$project.projectDir/clean-room-build"
        sourcePath = "$project.projectDir/clean-room-build"
    }
}
