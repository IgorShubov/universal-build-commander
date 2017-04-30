package com.ubc.plugin.nativecpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SonarRunnerNativeSetupTask extends DefaultTask {

    @TaskAction
    void setupSonarProperties() {

        def sonarProjectKey = project.nativePlugin.sonar.sonarProductKey
        def sonarTestDirsStmt = project.nativePlugin.sonar.sonarAnalysisTests ? """
sonar.tests=$project.nativePlugin.sonar.sonarAnalysisTests
""" : ""
        def sonarSourcesExcludeStmt = project.nativePlugin.sonar.sonarExclusionSources ? """
sonar.exclusions=$project.nativePlugin.sonar.sonarExclusionSources
""" : ""
        def sonarTestsExcludeStmt = project.nativePlugin.sonar.sonarExclusionTests ? """
sonar.test.exclusions=$project.nativePlugin.sonar.sonarExclusionTests
""" : ""

        def sonarProperties = """
sonar.host.url=http://$project.nativePlugin.sonar.sonarServer/sonar
sonar.login=$project.nativePlugin.sonar.sonarUsername
sonar.password=$project.nativePlugin.sonar.sonarPassword
sonar.language=cpp
sonar.sourceEncoding=UTF-8
sonar.projectKey=$sonarProjectKey
sonar.projectName=$sonarProjectKey
sonar.projectVersion=$project.version
sonar.sources=$project.nativePlugin.sonar.sonarAnalysisSources
$sonarTestDirsStmt
$sonarSourcesExcludeStmt
$sonarTestsExcludeStmt
sonar.verbose=true
sonar.scm.disabled=true
        """

        def sonarPropertiesFile =
                new File("clean-room-build/gbc-sonar.properties").withWriter { writer ->
            writer << sonarProperties
        }
    }
}
