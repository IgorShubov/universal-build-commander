package com.ubc.plugin.nativecpp

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Download All task  - create a task for each dependency dynamically and execute it
 * to get all native dependencies defined in nativeConfig
 */
class DownloadAllExternalsTask extends DefaultTask {
    /**
     * Perform the download
     */
    @TaskAction
    void download() {
        def downloadDir = "${project.projectDir}/clean-room-build/${project.nativeConfig.downloadDir}"
        if (project.nativeConfig.nativeDependencies == null)
            return
        project.nativeConfig.nativeDependencies.each { oneDep ->
            def depFile = new File("${downloadDir}/${oneDep.getFileName()}")
            execDownloadTask(oneDep, depFile)

            if (depFile.exists()) {
                project.getLogger().debug("Downloaded [${depFile}]")
                if (project.nativeConfig.unpack) {
                    String targetDir = "${project.projectDir}/clean-room-build/${oneDep.installDir}"
                    def targetDirFile = new File(targetDir)
                    if (!targetDirFile.exists()) {
                        targetDirFile.mkdirs()
                    }
                    execUnpackTask(depFile.getAbsolutePath(), oneDep.getExt(), targetDir)
                }
            }
            addDependencyToProject(project, oneDep)
        }
    }

    void addDependencyToProject(project, oneDep) {
        // No-Op
    }

    void execDownloadTask(oneDep, depFile) {
        def singleDownload = project.tasks.create(name: "Download ${oneDep.name}-${oneDep.version} ",
                type: DownloadExternalTask) {
            description "Download ${oneDep.getFileName()}"
            group "Native Dependencies"
            url resolveURL(oneDep)
            destination = depFile
            username = project.nativeConfig.artifactoryDownloadUsername
            password = project.nativeConfig.artifactoryDownloadPassword
        }
        singleDownload.execute()
    }

    String resolveURL(NativeDependency dependency) {
        String base = (dependency.version.endsWith("-SNAPSHOT")) ?
                project.nativeConfig.snapshotRepository :
                project.nativeConfig.repository;
        if (!base.endsWith('/')) {
            base = base + '/'
        }
        return "${base}${dependency.URLPath}"
    }

    void execUnpackTask(String source, String ext, String destination) {
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            execUnixUnpackTask(source, ext, destination)
        } else {
            execWinUnpackTask(source, ext, destination)
        }
    }

    void execUnixUnpackTask(String source, String ext, String destination) {
        if (ext == "tgz" || ext == "tar.gz") {
            def arguments = ["-zxvf", source]
            project.exec {
                executable = "tar"
                workingDir = destination
                args = arguments
            }
        } else if (ext == "tar") {
            def arguments = ["-xvf", source]
            project.exec {
                executable = "tar"
                workingDir = destination
                args = arguments
            }
        } else if (ext == "zip" || ext == "jar") {
            def arguments = ["-o", source]
            project.exec {
                executable = "unzip"
                workingDir = destination
                args = arguments
            }
        } else {
            project.getLogger().error("Couldn't unpack [${source}] - unknown file extention!")
        }
    }

    void execWinUnpackTask(String source, String ext, String destination) {
        if (ext == "tgz" || ext == "tar.gz") {
            project.ant.untar(src: source, dest: destination, compression: "gzip").execute()
        } else if (ext == "tar") {
            project.ant.untar(src: source, dest: destination).execute()
        } else if (ext == "zip" || ext == "jar") {
            project.ant.unzip(src: source, dest: destination).execute()
        } else {
            project.getLogger().error("Couldn't unpack [${source}] - unknown file extention!")
        }
    }
}