package com.ubc.plugin.msbuild

import com.ullink.Msbuild
import com.ullink.Registry
import groovy.io.FileType
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class RunMSBuildTask extends Msbuild {

    String defaultSdk64Path = "C:\\Program Files\\Microsoft SDKs\\Windows"
    String defaultSdkPath= "C:\\Program Files (x86)\\Microsoft SDKs\\Windows"
    String defaultSetEnvSubPath = "bin\\setenv.cmd"
    String sdkRegPath = "Software\\Microsoft\\Microsoft SDKs\\Windows"
    String nugetCache = "${project.projectDir}${File.separator}clean-room-nuget${File.separator}nuget-cache"
    String buildFile


    /**
     * The constructor that sets params for the extended Msbuild task in the gradle-msbuild-plugin
     */
    RunMSBuildTask() {
        buildDir = "${project.projectDir}${File.separator}clean-room-build${File.separator}"
        if (project.msBuildPlugin.projectFile)
            buildFile = project.file("$buildDir$project.msBuildPlugin.projectFile").path
        else {
            def slnFiles = project.msBuildPlugin.buildFiles.findAll { it.endsWith('.sln') }
            if (slnFiles.size() == 1) {
                // if only one sln is found use it where ever it is in the tree
                buildFile = slnFiles.find()
            } else {
                buildFile = project.msBuildPlugin.buildFiles.sort {
                    f1, f2 ->
                        def c1 = (f1 =~ /${Pattern.quote(File.separator)}/).count
                        def c2 = (f2 =~ /${Pattern.quote(File.separator)}/).count
                        def s1 = f1.endsWith('.sln')
                        def s2 = f2.endsWith('.sln')
                        c1 <=> c2 ?: s2 <=> s1 ?: f1 <=> f2
                }.first()
            }

            println "\n>>> No .sln or .csproj specified, using detected build file: $buildFile\n"
        }
        if (buildFile.endsWith('.sln'))
            solutionFile = buildFile
        else
            projectFile = buildFile

        if (project.msBuildPlugin.analysisBuild) {
            version = "14.0"
        }

        if (project.msBuildPlugin.projectName)
            projectName = project.msBuildPlugin.projectName
        else
            projectName = project.name

        if (project.msBuildPlugin.verbosity)
            verbosity = project.msBuildPlugin.verbosity
        else
            verbosity = 'normal'

        if (project.msBuildPlugin.targets)
            targets = project.msBuildPlugin.targets

        if (project.msBuildPlugin.parameters)
            parameters = project.msBuildPlugin.parameters

        if (!project.msBuildPlugin.daBuild) {
            configuration = "Release"
            if (project.msBuildPlugin.releaseTargets)
                targets = project.msBuildPlugin.releaseTargets
            if (project.msBuildPlugin.releaseParameters)
                parameters = project.msBuildPlugin.releaseParameters
        }

        if (project.msBuildPlugin.visualCpp) {
            println ">>> Finding path to Windows SDK v${project.msBuildPlugin.sdk}...\n"

            def sdkPath = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, "$sdkRegPath\\v${project.msBuildPlugin.sdk}", "InstallationFolder")

            if (!sdkPath) {
                println ">>> Couldn't locate Windows SDK v${project.msBuildPlugin.sdk}, trying to default to latest SDK installed...\n"
                sdkPath = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, sdkRegPath, "CurrentInstallFolder")
            }

            if (!sdkPath || !new File(sdkPath).isDirectory() || !new File(sdkPath + defaultSetEnvSubPath).exists()) {
                sdkPath = null //reset this as setenv couldn't be found
                println ">>> Couldn't locate Windows SDK through the registry, searching in the default folder $defaultSdkPath...\n"
                def folder = new File(defaultSdkPath)
                if (!folder.exists()) {
                    folder = new File(defaultSdk64Path)
                }
                if (folder.exists()) {
                    folder.eachFile(FileType.DIRECTORIES) { sdkFolder ->
                        if (!sdkFolder.absolutePath.contains(project.msBuildPlugin.sdk)) {
                            def cmd = new File(sdkFolder.absolutePath + (sdkFolder.absolutePath.endsWith(File.separator) ? "" : File.separator) +  defaultSetEnvSubPath)
                            if (cmd.exists()) {
                                println "$cmd.absolutePath exists..."
                                if (sdkPath)
                                    println "Updating to a more recent SDK version..."
                                sdkPath = sdkFolder.absolutePath
                            }
                        }
                    }
                }
            }

            if (sdkPath) {
                sdkSetEnv = [sdkPath + defaultSetEnvSubPath]

                if (project.msBuildPlugin.configuration)
                    sdkSetEnv += "/${project.msBuildPlugin.configuration}"
                if (project.msBuildPlugin.architecture)
                    sdkSetEnv += "/${project.msBuildPlugin.architecture}"
                if (project.msBuildPlugin.platform)
                    sdkSetEnv += "/${project.msBuildPlugin.platform}"
                sdkSetEnv += "&"
            }
            else {
                println ">>> [ERROR] Couldn't locate a Windows SDK install, invoking MSBuild but the compilation will probably fail for environmental reasons.\n"
            }
        }

        println ">>> Parsing ${buildFile}...\n"
    }

    /**
     * A Gradle action override that runs nuget restore before running msbuild
     */
    @Override
    @TaskAction
    def build() {
        def logFile, logFileStream, teeStream = System.out
        def buildLog = "${buildDir}nuget-restore.log"
        logFile = project.file(buildLog)
        logFile.parentFile.mkdirs()
        logFileStream = new FileOutputStream(logFile)
        teeStream = new TeeOutputStream(System.out, logFileStream)
        project.file(nugetCache).mkdirs()

        def nugetCmd = [project.msBuildPlugin.nugetExe]
        nugetCmd += 'restore'
        if (buildFile.endsWith('.sln')) {
            nugetCmd += buildFile
            nugetCmd += '-PackagesDirectory'
            nugetCmd += "${project.file(buildFile).parentFile}${File.separator}packages"
        }
        nugetCmd += '-NonInteractive'
        nugetCmd += '-DisableParallelProcessing' // temporarily put this to reduce load on Arty
        nugetCmd += '-ConfigFile'
        nugetCmd += project.msBuildPlugin.nugetConfig
        nugetCmd += '-Verbosity'
        nugetCmd += (verbosity == 'minimal') ? 'quiet' : 'normal'

        println "\n>>> NuGet will be invoked using '${nugetCmd.join(' ')}'\n"

        def result = project.exec {
            commandLine = nugetCmd
            standardOutput = teeStream
            workingDir = buildDir
            ignoreExitValue = true
            environment = [NuGetCachePath: nugetCache] + System.getenv()
        }

        if (result.getExitValue() != 0)
            println "\n>>> [WARNING] There was an error running the NuGet restore command, your build may fail due to missing packages"

        if (project.msBuildPlugin.runCodeAnalysis) {
            def sonarProjectKey = project.msBuildPlugin.productKey
            if (project.msBuildPlugin.component) {
                sonarProjectKey += "-$project.msBuildPlugin.component"
            }

            def sonarBeginCommand = [
                    project.msBuildPlugin.sonarRunnerExe,
                    "begin",
                    "/k:\"${sonarProjectKey}\"",
                    "/n:\"${sonarProjectKey}\"",
                    "/v:\"${project.msBuildPlugin.projectVersion}\"",
                    "/d:sonar.cs.opencover.reportsPaths=\"${buildDir}${File.separator}tests${File.separator}OpenCover-Coverage.xml\""
            ]

            println "\n>>> Sonar Runner will be invoked using: '${sonarBeginCommand}'"
            def sonarBeginExec = project.exec {
                commandLine = sonarBeginCommand
                workingDir = project.file(buildFile).parentFile
                ignoreExitValue = true
            }
            buildDir = project.file(buildFile).parentFile.path
        }

        if (version != null) {
            println "\n>>> Project will be build with MSBuild version ${version}"
        }
        super.build()

        if (project.msBuildPlugin.daBuild || project.msBuildPlugin.runCodeAnalysis) {
            println "\n\n>>> BUILD COMPLETE, running automated tests with NUnit...'\n"
            project.file("clean-room-build${File.separator}tests").delete() // TODO this doesnt currently work

            def testFailures = []
            if (project.msBuildFile.containsKey('Solution')) {
                project.msBuildFile.Solution.eval._SolutionProjectProjects.each {
                    def csproj = project.msBuildFile.get(it.Filename)
                    if (csproj) {   // && it.Filename.contains('Test')) {
                        def testFailure = runNUnit(it.Filename, it.FullPath, csproj.eval.Properties.PlatformTarget == 'x64' ? true : false)
                        if (testFailure)
                            testFailures.add(it.Filename)
                    }
                }
            } else {
                def csproj = project.msBuildFile.find().value
                def testFailure = runNUnit(project.msBuildFile.find().key, csproj.msbuild.buildFile, csproj.eval.Properties.PlatformTarget == 'x64' ? true : false)
                if (testFailure)
                    testFailures.add(project.msBuildFile.find().key)
            }

            def openCoverCoberturaCommand = [project.msBuildPlugin.openCoverCoberturaExe, "-input:tests${File.separator}OpenCover-Coverage.xml", "-output:tests${File.separator}Cobertura-Coverage.xml", "-sources:${project.file(buildFile).parent}"]
            println "\n>>> OpenCoverToCoberturaConverter will be invoked using '${openCoverCoberturaCommand.join(' ')}'\n"
            def openCoverCoberturaResult = project.exec {
                commandLine = openCoverCoberturaCommand
//            standardOutput = teeStream TODO add this
                workingDir = "${project.projectDir}${File.separator}clean-room-build${File.separator}"
                ignoreExitValue = true
            }

//            if (!testFailures.isEmpty())
//                throw new GradleException("\n>>> [ERROR] Failing the build, there were test failures or errors for the following projects: ${testFailures.join(' ')}\n")

            if (project.msBuildPlugin.runCodeAnalysis) {
                def sonarEndCommand = [project.msBuildPlugin.sonarRunnerExe, 'end']
                println "\n>>> Sonar Runner will be invoked using: '${sonarEndCommand}'"
                def sonarEndExec = project.exec {
                    commandLine = sonarEndCommand
                    workingDir = project.file(buildFile).parentFile
                    ignoreExitValue = true
                }
            }
        }

    }

    def runNUnit(String projectName, String projectFile, boolean is64Bit) {
        def logFile, logFileStream, teeStream = System.out
        project.file("clean-room-build${File.separator}tests").mkdirs()
        def testLog = "clean-room-build${File.separator}tests${File.separator}${projectName}-Tests.log"
        logFile = project.file(testLog)
        logFileStream = new FileOutputStream(logFile)
        teeStream = new TeeOutputStream(System.out, logFileStream)

        def openCoverCommand = [project.msBuildPlugin.openCoverExe, "-register${project.msBuildPlugin.buildServer ? '' : ':user'}", "-target:${is64Bit ? project.msBuildPlugin.nunitExe64 : project.msBuildPlugin.nunitExe}", "-targetargs:/xml:${buildDir}tests$File.separator$projectName-TestResult.xml $projectFile /nologo /noshadow",/*, "-mergebyhash", */ "-mergeoutput", "-output:tests${File.separator}OpenCover-Coverage.xml", "-returntargetcode"]
        println "\n>>> NUnit will be invoked via OpenCover using '${openCoverCommand.join(' ')}'\n"

        def openCoverResult = project.exec {
            commandLine = openCoverCommand
            standardOutput = teeStream
            workingDir = buildDir
            ignoreExitValue = true
        }

        if (openCoverResult.getExitValue() != 0) {
            println "\n>>> [ERROR] There were test failures or errors for the $projectName project, the build will continue...\n"
            true
        } else {
            false
        }
    }
}