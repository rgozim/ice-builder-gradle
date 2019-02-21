package com.zeroc.gradle.icebuilder.slice.tasks

import groovy.transform.CompileStatic
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
class PythonTask extends SliceTaskBase {

    private static final Logger Log = Logging.getLogger(PythonTask)

    @Optional
    @Input
    final Property<String> prefix = project.objects.property(String)

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            sources.each { file ->
                // deleteOutputFile(file)
            }
        }

        Set<File> filesForProcessing = []
        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails change) {
                // Is the change an includes or a source file ?
                if (sources.get().containsKey(change.file)) {
                    // Add input file for processing
                    filesForProcessing.add(change.file)
                } else if (dependencyMap.get().containsKey(change.file)) {
                    // If a dependency has changed, then we need to find any source sources
                    // using it and add them to be recompiled
                    def sources = dependencyMap.get().get(change.file)

                    filesForProcessing.addAll(sources)
                }
            }
        })

        if (!filesForProcessing.isEmpty()) {
            List cmd = [config.slice2py, "-I${config.sliceDir}"]

            // Add any additional includes
            includeDirs.files.each { File dir -> cmd.add("-I${dir}") }

            // Add sources for processing
            cmd.addAll(filesForProcessing)

            if (prefix) {
                // Set a prefix
                cmd.add("--prefix=${prefix}")
            }

            // Set the outputDir directory
            cmd.add("--outputDir-dir=${outputDir}")
            executeCommand(cmd)
        }

//        inputs.removed { change ->
//            if (change.file.directory) return
//
//            deleteOutputFile(change.file)
//        }
    }

    @Override
    String getOutputFileName(File file) {
        String extension = FilenameUtils.getExtension(file.name)
        String filename = FilenameUtils.getBaseName(file.name)
        if (prefix.isPresent()) {
            filename = prefix.get() + filename + "_ice"
        }
        return "${filename}.{$extension}"
    }

}
