package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import com.zeroc.gradle.icebuilder.slice.Dependency
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@SuppressWarnings("UnstableApiUsage")
abstract class SliceTaskBase extends DefaultTask {

    private static final def Log = Logging.getLogger(SliceTaskBase)

    // The directory to write source files to
    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    @InputFiles
    final ConfigurableFileCollection inputFiles = project.files

    @Optional
    @Input
    final ConfigurableFileCollection includeDirs = project.files()

    @Input
    final SetProperty<Dependency> dependencySrcMap = project.objects.setProperty(Dependency)

    // Change this to a configuration
    Configuration config = new Configuration()

    @InputFiles
    FileCollection getIncludeFiles() {
        FileCollection files = project.files()
        for (File dir : inputFiles.files) {
            files = files + project.fileTree(dir)
        }
        return files
    }

    protected void deleteOutputFile(File file) {
        // Convert the input filename to the output filename and
        // delete that file
        def targetFile = project.file("$outputDir/${file.name}")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    protected void executeCommand(List cmd) {
        def sout = new StringBuffer()
        def p = cmd.execute()

        p.waitForProcessOutput(sout, System.err)

        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }
}
