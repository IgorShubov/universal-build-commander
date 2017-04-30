package com.ubc.plugin.nativecpp

import org.apache.commons.codec.binary.Base64
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * DownloadTask
 * Downloads the content of a url to a specified file
 * TODO: Make this incremental, so we don't download twice
 */
class DownloadExternalTask extends DefaultTask {
    /**
     * URL to download
     */
    @Input
    String url;

/**
 * File to store the downloaded object in
 */
    @OutputFile
    File destination;

    String username

    String password

    /**
     * Perform the download
     */
    @TaskAction
    void doDownload() {
        project.getLogger().info("Downloading... ${destination}")
        destination.withOutputStream { out ->
            URL url = new URL(url)
            URLConnection connection = url.openConnection()
            if (username && password) {
                def userPass = "$username:$password"
                connection.setRequestProperty("Authorization", "Basic ${new String(new Base64().encode(userPass.bytes))}")
            }
            out << connection.inputStream
        }
    }
}
