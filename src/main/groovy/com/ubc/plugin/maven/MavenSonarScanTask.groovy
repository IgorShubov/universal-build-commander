package com.ubc.plugin.maven


class MavenSonarScanTask extends RunMavenTask {

    @Override
    def getMavenGoals() {
        return "org.codehaus.mojo:sonar-maven-plugin:2.7.1:sonar"
    }
}
