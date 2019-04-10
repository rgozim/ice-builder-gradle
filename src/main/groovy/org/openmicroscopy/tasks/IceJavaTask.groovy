package org.openmicroscopy.tasks

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class IceJavaTask extends SourceTask {

    private static final def Log = Logging.getLogger(IceJavaTask)

    @Input
    @Optional
    final Property<Boolean> tie = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> impl = project.objects.property(Boolean)

    @InputFiles
    @Optional
    final ListProperty<Directory> includeDirs = project.objects.listProperty(Directory)

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    // Change this to a configuration
    SliceExtension sliceExt = project.slice

    IceJavaTask() {
        super()
        setIncludes(["**/*.ice"])
    }

    @TaskAction
    void action() {
        List<String> cmd = [sliceExt.slice2java, "-E", "-d", "-I" + sliceExt.sliceDir]

        List<Directory> incDirs = includeDirs.getOrNull()
        if (incDirs) {
            // Add any additional includes
            incDirs.each { Directory dir ->
                cmd.add("-I" + dir.asFile)
            }
        }

        source.files.each { File file ->
            cmd.add(String.valueOf(file))
        }

        if (tie.getOrElse(false)) {
            cmd.add("--tie")
        }

        if (impl.getOrElse(false)) {
            cmd.add("--impl")
        }

        cmd.add("--output-dir=" + outputDir.asFile.get())
        executeCommand(cmd)
    }

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getSource() {
        super.getSource()
    }

    void executeCommand(List<String> cmd) {
        StringBuffer sout = new StringBuffer()
        Process p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)
        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }

}