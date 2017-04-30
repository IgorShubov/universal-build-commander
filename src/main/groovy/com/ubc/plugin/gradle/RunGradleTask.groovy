package com.ubc.plugin.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class RunGradleTask extends DefaultTask {

    @TaskAction
    def execGradle() {
        def gradlePE = project.gradlePlugin
        project.exec {
            def console = System.console()
            def logFile, logFileStream, teeStream = System.out
            def buildLog = "clean-room-build/build.log"

            logFile = project.file(buildLog)
            logFile.parentFile.mkdirs()
            logFileStream = new FileOutputStream(logFile)
            teeStream = new TeeOutputStream(System.out, logFileStream)
            standardOutput = teeStream
            workingDir = "$project.projectDir/clean-room-build"

            def command = []
            def pathSeparator = ':'
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                pathSeparator = ';'
                if (gradlePE.useCurrentGradle) {
                    def path = "$project.projectDir;${System.getenv('PATH')}"
                    environment = [
                            'JAVA_HOME': System.getenv("JAVA_HOME"),
                            'PATH'     : path,
                            'TMP'      : System.getenv("TMP")
                    ]
                    command << ['cmd', '/c', 'gradlew.bat']
                } else {
                    def path = "$project.projectDir/clean-room-gradle/gradle-$gradlePE.gradleVersion/bin;${System.getenv('PATH')}"
                    environment = [
                            'JAVA_HOME': System.getenv("JAVA_HOME"),
                            'PATH'     : path,
                            'TMP'      : System.getenv("TMP")
                    ]
                    command << ['cmd', '/c', 'gradle']
                }
            } else {
                if (gradlePE.useCurrentGradle) {
                    command << ["$project.projectDir/gradlew"]
                    environment = [
                            'JAVA_HOME': System.getenv("JAVA_HOME"),
                            'PATH'     : "$project.projectDir:${System.getenv('PATH')}"
                    ]
                } else {
                    command << ["./../clean-room-gradle/gradle-$gradlePE.gradleVersion/bin/gradle"]
                    environment = [
                            'JAVA_HOME': System.getenv("JAVA_HOME"),
                            'PATH'     : "./../clean-room-gradle/gradle-$gradlePE.gradleVersion/bin:${System.getenv('PATH')}"
                    ]
                }
            }
            command << ['--init-script', "$project.projectDir/clean-room-gradle/init.gradle"]
            command << ['--gradle-user-home', "$project.projectDir/clean-room-gradle/.gradle"]
            if (gradlePE.gradleSystemProperties) {
                command << gradlePE.gradleSystemProperties.collect { it.replace(/"/, '') }
            }
            if (gradlePE.gradleProjectProperties) {
                command << gradlePE.gradleProjectProperties.collect { it.replace(/"/, '') }
            }

            if (gradlePE.envVars) {
                gradlePE.envVars.each {
                    if (it.key == 'PATH') {
                        environment.put(it.key, it.value+pathSeparator+environment.get('PATH'))
                    } else {
                        environment.put(it.key, it.value)
                    }
                }
            }
            if (gradlePE.gradleOpts) {
                environment.put('GRADLE_OPTS', gradlePE.gradleOpts)
            }

            if (!gradlePE.buildServer) {
                // add --debug and --full-stacktrace
                def debug = console.readLine('\n\n> Run build with debug output? (yes/no) yes: ')
                if (debug == '' || debug.toLowerCase() == 'yes') {
                    command << '--debug'
                }
                def fullStackTrace = console.readLine('\n> Run build with full stacktrace? (yes/no) yes: ')
                if (fullStackTrace == '' || fullStackTrace.toLowerCase() == 'yes') {
                    command << '--full-stacktrace'
                }
            }

            if (gradlePE.gradleArgs) {
                command << gradlePE.gradleArgs.collect { it.replaceAll(/"/, '') }
            }

            command << getGradleTasks()

            command << ['--build-file', gradlePE.gradleBuildFile]

            if (project.hasProperty("gradleSettingsFile")) {
                command << ['--settings-file', gradlePE.gradleSettingsFile]
            }

            command = command.flatten()
            println("\n>>> Gradle will be invoked using '${command.join(' ')}'\n")
            commandLine = command
        }
    }

    def getGradleTasks() {

        def buildServerTasks = ["clean build", "artifactoryPublish"]
        def daBuildTasks = ["clean build test", "publishToMavenLocal"]
        def nonDaLocalBuildTasks = ["clean build", "publishToMavenLocal"]

        def assignedBuildTasks = project.gradlePlugin.daBuild ?
                daBuildTasks : project.gradlePlugin.buildServer ? buildServerTasks : nonDaLocalBuildTasks

        def gradleTasks = assignedBuildTasks[0].split(' ').flatten()

        def settingsPath = "$project.projectDir/clean-room-build/settings.gradle"
        def settingsGradleFile = new File(settingsPath)
        boolean isRootPrj = settingsGradleFile.exists()

        if (project.gradlePlugin.gradleTasks) {
            project.gradlePlugin.gradleTasks.each {
                if (isRootPrj && it.toString().equals("cloverGenerateReport")) {
                    gradleTasks << "cloverAggregateReport"
                } else {
                    gradleTasks << it.toString()
                }
            }
        }

        gradleTasks << assignedBuildTasks[1]

        return gradleTasks.unique()
    }
}
