package com.ubc.plugin.nativecpp

import org.gradle.api.Plugin
import org.gradle.api.Project


class NativeCppPlugin implements Plugin<Project> {

    void apply(Project project) {
        // Project level configuration for this plugin
        project.extensions.create('nativeConfig', NativeCppPluginExtension)

        // Create the DownloadAllExternalsTask
        project.task('downloadAllNative', type: DownloadAllExternalsTask) {
            description 'Download all external native dependencies'
            group "Native Dependencies"
        }
    }
}