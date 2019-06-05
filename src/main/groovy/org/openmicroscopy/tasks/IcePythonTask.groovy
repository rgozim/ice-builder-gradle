package org.openmicroscopy.tasks


import org.apache.commons.io.FilenameUtils
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

class IcePythonTask extends IceTask {

    @Input
    @Optional
    final Property<String> prefix = project.objects.property(String)

    IcePythonTask(String iceCommand) {
        super("slice2py")
    }

    @TaskAction
    void action(IncrementalTaskInputs inputs) {
        // If the user for example used --rerun-tasks
        // this task is not incremental. Only
        // inputs.outOfDate is executed, so we must first
        // remove all output files.
        if (!inputs.incremental) {
            project.delete(outputDir.asFile.get().listFiles())
        }

        // Input file has changed, so we convert it.
        List<String> filesForProcessing = []
        inputs.outOfDate { InputFileDetails outOfDate ->
            // Add input file for processing
            filesForProcessing.add(String.valueOf(outOfDate.file))
        }

        if (!filesForProcessing.isEmpty()) {
            List<String> cmd = createBaseCompileSpec()

            // Add files for processing
            cmd.addAll(filesForProcessing)

            if (prefix.isPresent()) {
                // Set a prefix
                cmd.add("--prefix=" + prefix.get())
            }

            executeCommand(cmd)
        }

        inputs.removed { change ->
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

}
