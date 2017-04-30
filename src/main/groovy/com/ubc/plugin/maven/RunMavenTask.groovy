package com.ubc.plugin.maven

import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class RunMavenTask extends DefaultTask {

    /**
     * The Gradle task that runs Maven using project.exec()
     */
    @TaskAction
    def execMvn() {
        project.exec {
            def console = System.console()
            def logFile, logFileStream, teeStream = System.out
            def buildLog = "clean-room-build/build.log"
            def path, debug = 'no'
            logFile = project.file(buildLog)
            logFile.parentFile.mkdirs()
            logFileStream = new FileOutputStream(logFile)
            teeStream = new TeeOutputStream(System.out, logFileStream)

            standardOutput = teeStream
            workingDir = "$project.projectDir/clean-room-build"

            def mavenPE = project.mavenPlugin
            def mvnGoals = mavenGoals
            if (mavenPE.daBuild) {
                mavenPE.mvnArgs.remove("\"-DskipTests=true\"")
            }
            if (mavenPE.buildServer) {
                if (mavenPE.daBuild) {
                    mavenPE.mvnArgs.add("\"-Dmaven.clover.license=${project.properties.get('cloverLicense')}\"")
                }
            } else if (!mavenPE.mvnArgs.grep('"-X"')) {
                debug = console.readLine('\n\n> Run build with debug output? (yes/no) yes: ')
            }

            if (debug == '' || debug == 'yes')
                mavenPE.mvnArgs.add('-X')

            def pathSeparator = ":"
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                pathSeparator = ";"
                path = "$project.projectDir/clean-room-maven/apache-maven-$mavenPE.mvnVersion/bin$pathSeparator${System.getenv('PATH')}"
                commandLine = (["cmd", "/c", "mvn", mvnGoals.split(' ')] << mavenPE.mvnArgs).flatten()
                environment = [
                        'M2_HOME'  : "$project.projectDir/clean-room-maven/apache-maven-$mavenPE.mvnVersion",
                        'JAVA_HOME': System.getenv("JAVA_HOME"),
                        'PATH'     : path,
                        'TMP'      : System.getenv("TMP")
                ]
            } else {
                // Todo path doesnt work for unix
                pathSeparator = ":"
                path = "$project.projectDir/clean-room-maven/apache-maven-$mavenPE.mvnVersion/bin$pathSeparator${System.getenv('PATH')}"
                environment = [
                        'M2_HOME'  : "$project.projectDir/clean-room-maven/apache-maven-$mavenPE.mvnVersion",
                        'PATH'     : path,
                ]
                commandLine = (["./../clean-room-maven/apache-maven-$mavenPE.mvnVersion/bin/mvn"] << mvnGoals.split(' ') << mavenPE.mvnArgs).flatten()
            }
            if (mavenPE.envVars) {
                mavenPE.envVars.each {
                    if (it.key == 'PATH') {
                        environment.put(it.key, it.value+pathSeparator+environment.get(it.key))
                    } else {
                        environment.put(it.key, it.value)
                    }
                }
            }

            if (mavenPE.mvnOpts)
                environment.put('MAVEN_OPTS', mavenPE.mvnOpts)

            println "Environment = $environment"
            println "Command Line = $commandLine"
            println "\n>>> Maven will be invoked using '$mvnGoals ${mavenPE.mvnArgs.join(' ')}'\n"
        }
    }

    def getMavenGoals() {
        def mavenPE = project.mavenPlugin
        if (mavenPE.daBuild)
            return "clean com.atlassian.maven.plugins:maven-clover2-plugin:4.0.2:setup install com.atlassian.maven.plugins:maven-clover2-plugin:4.0.2:aggregate com.atlassian.maven.plugins:maven-clover2-plugin:4.0.2:clover -s../clean-room-maven/build-settings.xml -V -fae"
        else if (mavenPE.buildServer)
            return "clean deploy -s../clean-room-maven/build-settings.xml -V"
        else
            return "clean install -s../clean-room-maven/build-settings.xml -V"
    }
}
