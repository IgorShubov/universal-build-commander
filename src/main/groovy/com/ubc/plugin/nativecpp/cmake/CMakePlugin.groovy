package com.ubc.plugin.nativecpp.cmake

import org.gradle.api.Plugin
import org.gradle.api.Project

class CMakePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create('cmake', CMakeExtension, project)
    }
}
