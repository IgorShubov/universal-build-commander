package com.ubc.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

public class UbcBuildCommanderTest {

    public File buildFile

    @Test
    public void testApplyPlugin() {

        buildFile = new File(this.class.getResource("build.gradle").path)

        GradleRunner.create()
                .withProjectDir(buildFile.parentFile)
                .build()

    }
}
