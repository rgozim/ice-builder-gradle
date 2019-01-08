package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class PythonTask extends AbstractExecTask {

    private static final def Log = Logging.getLogger(SliceTask)

    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    @Input
    @Optional
    FileCollection includeDirs

    @Input
    @Optional
    String prefix

    PythonTask() {
        super(PythonTask.class)
    }

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental)
            project.delete(outputDir.listFiles())

        def cmd = buildCommand()
        inputs.outOfDate { change ->
            cmd.add("$change.file")
            executeCommand(cmd)
        }
    }



    List buildCommand() {
        // Change this to a configuration
        SliceExtension sliceExt = project.slice
        List cmd = [
                sliceExt.slice2py,
                "--output-dir=$outputDir",
                "-I${sliceExt.sliceDir}",
        ]
        if (includeDirs) {
            includeDirs.each { dir -> cmd.add("-I${dir}") }
        }
        if (prefix) {
            cmd.add("--prefix=$prefix")
        }
        return cmd
    }

    void executeCommand(List cmd) {
        Log.info("Command: $cmd")
        def sout = new StringBuffer()
        def serr = new StringBuffer()
        def p = cmd.execute(project.slice.env, null)

        p.waitForProcessOutput(sout, serr)
        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
        Log.info(sout.toString())
    }
}
