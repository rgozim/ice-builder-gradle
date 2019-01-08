package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.impldep.org.apache.commons.io.FilenameUtils

class PythonTask extends DefaultTask {

    private static final def Log = Logging.getLogger(SliceTask)

    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    @InputFiles
    @Optional
    FileCollection includeDirs

    @Input
    @Optional
    String prefix

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental)
            project.delete(outputDir.listFiles())

        List<File> files = []
        inputs.outOfDate { change ->
            if (change.file.isFile()) {
                Log.info("$change.file")
                files.add(change.file)
            }
        }

        if (!files.isEmpty()) {
            List cmd = [sliceExt.slice2py, "-I${sliceExt.sliceDir}", "-I${inputDir}"]
            files.each { file ->
                cmd.add(file.getAbsoluteFile())
            }
            if (prefix) {
                cmd.add("--prefix=${prefix}")
            }
            cmd.add("--output-dir=${outputDir}")
            executeCommand(cmd)
        }

        inputs.removed { removed ->
            def extension = FilenameUtils.getExtension(removed.file.name)
            def filename = FilenameUtils.getBaseName(removed.file.name)
            if (prefix) {
                filename = prefix + filename + "_ice"
            }
            def targetFile = project.file("$outputDir/${filename}.{$extension}")
            if (targetFile.exists()) {
                targetFile.delete()
            }
        }
    }

    void executeCommand(List cmd) {
        def sout = new StringBuffer()
        def p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)
        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }
}
