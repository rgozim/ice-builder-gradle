package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import com.zeroc.gradle.icebuilder.slice.DependencyParser
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@SuppressWarnings("UnstableApiUsage")
abstract class SliceTaskBase extends DefaultTask {

    private static final Logger Log = Logging.getLogger(SliceTaskBase)

    // The directory to write source files to
    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()


    @InputFiles
    final ConfigurableFileCollection sources = project.files()

    @Optional
    @Input
    final ConfigurableFileCollection includeDirs = project.files()

    // Change this to a configuration
    Configuration config = new Configuration()

    final MapProperty<RegularFile, FileCollection> inputFiles = project.objects.mapProperty(File, FileCollection)

    @InputFiles
    List<File> getSources() {
        inputFiles.keySet()
    }

    @InputFiles
    Set<File> getDependencies() {
        Set<File> uniqueDependencies = []
        inputFiles.values().each {
            uniqueDependencies.addAll(it)
        }
        uniqueDependencies
    }

    void setInputFiles(MapProperty<? extends RegularFile, ? extends FileCollection> input) {
        inputFiles.set(input)
    }

    void includes(Object... files) {
        setIncludes(files)
    }

    void setIncludes(Object... files) {
        def includes = includeDirs.get()
        files.each { file ->
            includes.add(project.objects.directoryProperty().set(file as File))
        }
    }

    protected String getOutputFileName(File file) {
        def extension = FilenameUtils.getExtension(file.name)
        def filename = FilenameUtils.getBaseName(file.name)
        return "${filename}.{$extension}"
    }

    protected void deleteOutputFile(File file) {
        // Convert the input filename to the output filename and
        // delete that file
        def targetFile = project.file("$outputDir/${getOutputFileName(file)}")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    protected void executeCommand(List cmd) {
        def sout = new StringBuffer()
        def p = cmd.execute()

        p.waitForProcessOutput(sout, System.err)

        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }
}
