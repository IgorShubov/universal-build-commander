package com.ubc.plugin.nativecpp

import org.gradle.api.Plugin
import org.gradle.api.Project

/*
  * This plugin basically creates the extension "publishConfig" to the project.
  * It adds the publication declared in test-build.xml to the publishConfig
 */
class ConfigurePublishNativePlugin implements Plugin<Project> {

    void apply(Project project) {

        println("\n>>> ConfigurePublishNativePlugin attempting to configure the publishing.")

        project.extensions.create('publishConfig', PublishConfiguration)

        project.publishConfig.publishEntireTargetDir = false
    }

    private boolean requiresPublishConfig(Project project) {
        return (project.hasProperty("universalNativePlugin") &&
                project.universalNativePlugin.nativeConfiguration &&
                project.universalNativePlugin.nativeConfiguration['publication'])
    }
}
