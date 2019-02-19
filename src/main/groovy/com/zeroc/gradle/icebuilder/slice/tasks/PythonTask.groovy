package com.zeroc.gradle.icebuilder.slice.tasks

import groovy.transform.CompileStatic
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
class PythonTask extends SliceTaskBase {

    private static final Logger Log = Logging.getLogger(PythonTask)

    @Input
    final Property<String> prefix = project.objects.property(String)

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            sources.each { file ->
                deleteOutputFile(file)
            }
        }

        List filesForProcessing = []
        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails change) {
                // Is the change an include or a source file ?
                if (inputFiles.get().containsKey(change.file)) {
                    // Add input file for processing
                    filesForProcessing.add("${change.file}")
                } else if (dependencies.contains(change.file)) {
                    // If a dependency has changed, then we need to find any source files
                    // using it and add them to be recompiled


                }


                //
//                // Log which file will be included in slice2py
//            Log.info("File for processing: $change.file")
//
//                // Add input file for processing
//                filesForProcessing.add("${change.file}")
            }

        })


//        if (!filesForProcessing.isEmpty()) {
//            List cmd = [config.slice2py, "-I${config.sliceDir}"]
//
//            // Add any additional includes
//            includeDirs.get().each { dir -> cmd.add("-I${dir.asFile}") }
//
//            // Add files for processing
//            cmd.addAll(filesForProcessing)
//
//            if (prefix) {
//                // Set a prefix
//                cmd.add("--prefix=${prefix}")
//            }
//
//            // Set the output directory
//            cmd.add("--output-dir=${outputDir}")
//            executeCommand(cmd)
//        }
//
//        inputs.removed { change ->
//            if (change.file.directory) return
//
//            deleteOutputFile(change.file)
//        }
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
