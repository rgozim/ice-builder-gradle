package org.openmicroscopy.tasks

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask

abstract class IceTask extends SourceTask {

    @Input
    @Optional
    final Property<Boolean> debug = project.objects.property(Boolean)

    @InputFiles
    @Optional
    final ListProperty<Directory> includeDirs = project.objects.listProperty(Directory)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    private final String iceCommand

    private IceTask() {
        super()
    }

    IceTask(String iceCommand) {
        super()
        this.iceCommand = iceCommand
        this.setIncludes(["**/*.ice"])
    }

    List<String> createBaseCompileSpec() {
        List<String> cmd = [iceCommand]

        if (debug.getOrElse(false)) {
            cmd.add("-d")
        }

        // Set the output directory
        cmd.addAll(["--output-dir", String.valueOf(outputDir.asFile.get())])

        // Add internal slice dir as default include
        cmd.add("-I" + sliceExt.sliceDir)

        // Include directories used for "include/import" directives in ice files
        List<Directory> includeDirsList = includeDirs.getOrNull()
        if (includeDirsList) {
            // Add any additional includes
            includeDirsList.each { Directory dir ->
                cmd.add("-I" + dir.asFile)
            }
        }

        return cmd
    }

    void executeCommand(List<String> cmd) {
        StringBuffer sout = new StringBuffer()
        Process p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)
        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }

    /**
     * This method exists so incremental build support will kick in if the
     * contents of any of those directories change.
     * @return collection of all files in directories
     */
//    @InputFiles
//    private FileCollection getIncludedFiles() {
//        FileCollection files = project.files()
//        List<Directory> dirs = includeDirs.get()
//        for (Directory dir : dirs) {
//            files = files + project.fileTree(dir)
//        }
//        return files
//    }

}
