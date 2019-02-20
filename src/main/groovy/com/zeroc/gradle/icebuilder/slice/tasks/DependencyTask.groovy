package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class DependencyTask extends DefaultTask {

    private static final Logger Log = Logging.getLogger(DependencyTask)

    @Optional
    @Input
    final ConfigurableFileCollection includeDirs = project.layout.configurableFiles()

    @InputFiles
    final ConfigurableFileCollection inputFiles = project.layout.configurableFiles()

    @OutputFile
    final RegularFileProperty outputFile = project.objects.fileProperty()

    private Configuration config = new Configuration()

    @TaskAction
    void apply() {
        List cmd = [config.slice2py, "-I${config.sliceDir}"]
        includeDirs.each { dir -> cmd.add("-I${dir}") }
        cmd.addAll(inputFiles.files)
        cmd.add("--depend-xml")

        StringBuffer sout = new StringBuffer()
        Process p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)

        // Write results to output file
        writeFile(outputFile.get().asFile, sout.toString())
    }

    private void writeFile(File destination, String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(destination));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

}
