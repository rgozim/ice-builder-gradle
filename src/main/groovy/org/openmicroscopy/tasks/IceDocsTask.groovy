package org.openmicroscopy.tasks

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class IceDocsTask extends SourceTask {

    private static final def Log = Logging.getLogger(IceDocsTask)

    @Input
    @Optional
    final Property<Boolean> underscore = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> debug = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Integer> index = project.objects.property(Integer)

    @Input
    @Optional
    final Property<Integer> summary = project.objects.property(Integer)

    @InputFile
    @Optional
    final RegularFileProperty header = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty footer = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty indexHeader = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty indexFooter = project.objects.fileProperty()

    @InputFiles
    @Optional
    final ListProperty<Directory> includeDirs = project.objects.listProperty(Directory)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    IceDocsTask() {
        super()
        setIncludes(["**/*.ice"])
    }

    @TaskAction
    void apply() {
        List<String> cmd = ["slice2html", "-I" + sliceExt.sliceDir]

        cmd.addAll(["--output-dir", String.valueOf(outputDir.asFile.get())])

        List<Directory> includeDirsList = includeDirs.getOrNull()
        if (includeDirsList) {
            // Add any additional includes
            includeDirsList.each { Directory dir ->
                cmd.add("-I" + dir.asFile)
            }
        }

        if (header.isPresent()) {
            cmd.addAll(["--hdr", String.valueOf(header.asFile.get())])
        }

        if (footer.isPresent()) {
            cmd.addAll(["--ftr", String.valueOf(footer.asFile.get())])
        }

        if (indexHeader.isPresent()) {
            cmd.addAll(["--indexhdr", String.valueOf(indexHeader.asFile.get())])
        }

        if (indexFooter.isPresent()) {
            cmd.addAll(["--indexhdr", String.valueOf(indexFooter.asFile.get())])
        }

        // Add the source files
        source.files.each {
            cmd.add(String.valueOf(it))
        }

        if (debug.getOrElse(false)) {
            cmd.add("-d")
        }

        executeCommand(cmd)
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
