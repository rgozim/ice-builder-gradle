package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.SlicePlugin
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class JavaTask extends SliceTaskBase {

    private static final def Log = Logging.getLogger(JavaTask)

    @org.gradle.api.tasks.Input
    @Optional
    String prefix

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            inputFiles.each { file ->
                deleteOutputFile(file)
            }
        }

        List filesForProcessing = []
        inputs.outOfDate { change ->
            if (change.file.directory) return

            // Log which file will be included in slice2py
            Log.info("File for processing: $change.file")

            // Add input file for processing
            filesForProcessing.add("${change.file}")
        }

        if (!filesForProcessing.isEmpty()) {
            List cmd = [config.slice2java, "-I${config.sliceDir}"]

            if (includeDirs) {
                // Add any additional includes
                includeDirs.each { dir -> cmd.add("-I${dir}") }
            }

            //
            // --compat only available for Ice 3.7 and higher
            //
            if (SlicePlugin.compareVersions(iceVersion, '3.7') >= 0) {
                this.compat = compat ?: false
            } else if (compat != null) {
                LOGGER.warn("Property \"slice.compat\" unavailable for Ice ${iceVersion}.")
            }

            // Add files for processing
            cmd.addAll(filesForProcessing)

            // Set the output directory
            cmd.add("--output-dir=${outputDir}")
            executeCommand(cmd)
        }

        inputs.removed { change ->
            if (change.file.directory) return

            deleteOutputFile(change.file)
        }
    }

}
