package com.ubc.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

public abstract class UniversalPlugin implements Plugin<Project> {

    abstract void configureBuildProperties(Project project)
    abstract void configureTasks(Project project)
    abstract void executeTasks(Project project)

    final void apply(Project project) {
        project.configure(project) {
            configureBuildProperties(project)
            configureTasks(project)
        }
        executeTasks(project)
    }
}