package com.ubc.plugin.nativecpp

/**
 * Holds configuration defaults for the plugin and tasks
 */
class NativeCppPluginExtension {
    // Where tarballs are downloaded (relative to ${buildDir})
    String downloadDir = "externals/download"

    // Where externals are unpacked (relative to ${buildDir})
    String installDir = "externals/install"
    /**
     * If set to <code>true</code> (default) - Unpack all external depends into
     * <code>installDir</code>, otherwise do not unpack
     */
    Boolean unpack = true

    def nativeDependencies = []

    String repository

    String snapshotRepository

    String buildCommand

    String artifactoryDownloadUsername

    String artifactoryDownloadPassword
}