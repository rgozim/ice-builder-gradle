package com.zeroc.gradle.icebuilder.slice

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    File projectDir
    File buildFile
    File settingsFile

    def setup() {
        projectDir = temporaryFolder.root
        temporaryFolder.newFolder()
        buildFile = temporaryFolder.newFile(getBuildFileName())
        settingsFile = temporaryFolder.newFile(getSettingsFileName())
    }

    abstract String getBuildFileName()

    abstract String getSettingsFileName()

    protected BuildResult build(String... arguments) {
        createAndConfigureGradleRunner(arguments).build()
    }

    protected BuildResult buildAndFail(String... arguments) {
        createAndConfigureGradleRunner(arguments).buildAndFail()
    }

    protected File file(String relativePath) {
        File file = new File(projectDir, relativePath)
        assert file.exists()
        file
    }

    private GradleRunner createAndConfigureGradleRunner(String... arguments) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments + '-s' as List<String>)
                .withPluginClasspath()
    }

}