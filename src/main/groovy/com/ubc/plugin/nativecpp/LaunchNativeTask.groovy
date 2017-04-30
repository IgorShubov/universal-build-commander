package com.ubc.plugin.nativecpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class LaunchNativeTask extends DefaultTask {

    @TaskAction
    public void launch() {
        println("\n>>> Launched the Native Build.")
    }
}
