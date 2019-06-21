package org.openmicroscopy.tasks

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty

abstract class IceTask extends DefaultTask {

    private final Property<Boolean> debug = project.objects.property(Boolean)

    private final DirectoryProperty outputDir = project.objects.directoryProperty()

    private final ListProperty<Directory> includeDirs = project.objects.listProperty(Directory)

    private final ConfigurableFileCollection source = project.files()

    private final String iceCommand

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    IceTask(String iceCommand) {
        super()
        this.iceCommand = iceCommand
    }

    @InputFiles
    @SkipWhenEmpty
    FileCollection getSource() {
        return source.filter { File file ->
            file.name.endsWith(".ice")
        }
    }

    @Input
    @Optional
    Property<Boolean> getDebug() {
        return debug
    }

    @InputFiles
    @Optional
    ListProperty<Directory> getIncludeDirs() {
        return includeDirs
    }

    @OutputDirectory
    DirectoryProperty getOutputDir() {
        return outputDir
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

    void source(Object... files) {
        this.source.from(files)
    }

    void source(Iterable<?> files) {
        this.source.from(files)
    }

    void setSource(Object... files) {
        this.source.setFrom(files)
    }

    void setSource(Iterable<?> files) {
        this.source.setFrom(files)
    }

    void setIncludeDirs(String... dirs) {
        List<Directory> list = dirs.collect {
            project.layout.projectDirectory.dir(it)
        }
        this.includeDirs.addAll(list)
    }

}
