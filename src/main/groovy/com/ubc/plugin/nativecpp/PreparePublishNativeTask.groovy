package com.ubc.plugin.nativecpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar

class PreparePublishNativeTask extends DefaultTask {

    @TaskAction
    void exec() {
        def artf = project.publishConfig.nativeArtifact

        if (project.publishConfig.publishEntireTargetDir) {

            def archiveTask = project.tasks.create(name: "archive-${project.publishConfig.archiveName}", type: Tar) {
                extension = 'tgz'
                compression = Compression.GZIP
                destinationDir = project.file("${project.projectDir}/clean-room-build/${project.publishConfig.publishDir}")
                archiveName = artf.artifactName

                if (project.publishConfig.packageContext) {
                    into("${project.publishConfig.packageContext}/${project.publishConfig.archiveName}/${project.version}/${project.publishConfig.aol}") {
                        from "${project.projectDir}/clean-room-build/${artf.targetDir}"
                    }
                } else {
                    into("${project.publishConfig.archiveName}/${project.version}/${project.publishConfig.aol}") {
                        from "${project.projectDir}/clean-room-build/${artf.targetDir}"
                    }
                }
            }
            println("\n>>> Executing ${archiveTask.name}")
            archiveTask.execute()
        } else {
            def copyTask = project.tasks.create(name: "rename-${project.publishConfig.archiveName}", type: Copy) {
                from "${project.projectDir}/clean-room-build/${artf.targetDir}/${artf.name}.${artf.ext}"
                into "${project.projectDir}/clean-room-build/${project.publishConfig.publishDir}"
                rename { String fileName ->
                    fileName.replace("${artf.name}.${artf.ext}", "${artf.artifactName}")
                }
            }
            println("\n>>> Executing ${copyTask.name}")
            copyTask.execute()
        }
    }
}
