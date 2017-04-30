package com.ubc.plugin.gradle

class GradleSonarScanTask extends RunGradleTask {

    @Override
    def getGradleTasks() {
        return ["sonarqube"]
    }
}
