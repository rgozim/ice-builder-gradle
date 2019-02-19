package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.DependencyParser
import org.apache.commons.io.FilenameUtils
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs


class PythonTask extends SliceTaskBase {

    private static final def Log = Logging.getLogger(PythonTask)

    @Input
    final Property<String> prefix = project.objects.property(String)

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        // parseXml
        Set<DependencyParser.Dependency> dependencies =
                DependencyParser.parseSliceDependencyXML(dependencyXmlFile.get().asFile.text)



        if (!inputs.incremental) {
            inputFiles.get().each { file ->
                deleteOutputFile(file.asFile)
            }
        }

        List filesForProcessing = []
        inputs.outOfDate { change ->
            // Ignore directories
            if (change.file.directory) return

            // Ignore dependencies, we don't compile them
            if (dependencies.get().contains(change.file)) return

            // Log which file will be included in slice2py
            Log.info("File for processing: $change.file")

            // Add input file for processing
            filesForProcessing.add("${change.file}")
        }

        if (!filesForProcessing.isEmpty()) {
            List cmd = [config.slice2py, "-I${config.sliceDir}"]

            // Add any additional includes
            includeDirs.get().each { dir -> cmd.add("-I${dir.asFile}") }

            // Add files for processing
            cmd.addAll(filesForProcessing)

            if (prefix) {
                // Set a prefix
                cmd.add("--prefix=${prefix}")
            }

            // Set the output directory
            cmd.add("--output-dir=${outputDir}")
            executeCommand(cmd)
        }

        inputs.removed { change ->
            if (change.file.directory) return

            deleteOutputFile(change.file)
        }
    }

    @Override
    String getOutputFileName(File file) {
        def extension = FilenameUtils.getExtension(file.name)
        def filename = FilenameUtils.getBaseName(file.name)
        if (prefix) {
            filename = prefix + filename + "_ice"
        }
        return "${filename}.{$extension}"
    }

}
