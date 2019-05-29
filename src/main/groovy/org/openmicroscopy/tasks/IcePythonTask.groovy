package org.openmicroscopy.tasks

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class IcePythonTask extends DefaultTask {

    private static final def Log = Logging.getLogger(IcePythonTask)

    @InputFiles
    final ConfigurableFileCollection sourceFiles = project.files()

    @InputFiles
    @Optional
    final ConfigurableFileCollection includeDirs = project.files()

    @Input
    @Optional
    final Property<String> prefix = project.objects.property(String)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            sourceFiles.files.each { file ->
                deleteOutputFile(file)
            }
        }

        List<String> filesForProcessing = []
        inputs.outOfDate { change ->
            if (change.file.directory) return

            // Log which file will be included in slice2py
            Log.info("File for processing: $change.file")

            // Add input file for processing
            filesForProcessing.add(String.valueOf(change.file))
        }

        if (!filesForProcessing.isEmpty()) {
            List<String> cmd = ["slice2py", "-I" + sliceExt.sliceDir]

            if (!includeDirs.isEmpty()) {
                // Add any additional includes
                includeDirs.files.each { dir -> cmd.add("-I" + dir) }
            }

            // Add files for processing
            cmd.addAll(filesForProcessing)

            if (prefix.isPresent()) {
                // Set a prefix
                cmd.add("--prefix=" + prefix.get())
            }

            // Set the output directory
            cmd.add("--output-dir=" + outputDir.asFile.get())
            executeCommand(cmd)
        }

        inputs.removed { change ->
            if (change.file.directory) return

            deleteOutputFile(change.file)
        }
    }

    void outputDir(String dir) {
        setOutputDir(dir)
    }

    void outputDir(File dir) {
        setOutputDir(dir)
    }

    void setOutputDir(String dir) {
        setOutputDir(project.file(dir))
    }

    void setOutputDir(File dir) {
        outputDir.set(dir)
    }

    void prefix(String text) {
        setPrefix(text)
    }

    void setPrefix(String text) {
        prefix.set(text)
    }

    void deleteOutputFile(File file) {
        // Convert the input filename to the output filename and
        // delete that file
        File targetFile = project.file("$outputDir/${getOutputFileName(file)}")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    String getOutputFileName(File file) {
        String temp = file.name
        String extension = FilenameUtils.getExtension(temp)
        String filename = FilenameUtils.getBaseName(temp)
        if (prefix.isPresent()) {
            filename = prefix.get() + filename + "_ice"
        }
        return "${filename}.{$extension}"
    }

    void executeCommand(List cmd) {
        StringBuffer sout = new StringBuffer()
        Process p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)
        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }
}
