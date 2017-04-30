package com.ubc.plugin.nativecpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

//Todo: this will most likely need to download Cmake at a specified version
class DownloadNativeTask extends DefaultTask {

    @TaskAction
    def DownloadAction() {
        println "\n>>> I totally just downloaded the native plugin...\n"
    }
}
