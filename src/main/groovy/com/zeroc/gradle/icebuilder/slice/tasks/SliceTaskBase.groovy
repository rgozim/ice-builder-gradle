package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import groovy.transform.CompileStatic
import groovy.transform.Internal
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
abstract class SliceTaskBase extends DefaultTask {

    private static final Logger Log = Logging.getLogger(SliceTaskBase)

    private final DirectoryProperty outputDir = project.objects.directoryProperty()

    private final ConfigurableFileCollection includeDirs = project.files()

    private final MapProperty<File, FileCollection> sourceToDependencies =
            project.objects.mapProperty(File, List<File>)

    private final MapProperty<File, List<File>> dependencyToSources =
            project.objects.mapProperty(File, FileCollection)

    private Set<File> uniqueDependencies = []

    // Change this to a configuration
    protected Configuration config = new Configuration()

    @OutputDirectory
    DirectoryProperty getOutputDir() {
        return outputDir
    }


    @InputFiles
    Set<File> getSources() {
        sourceToDependencies.get().keySet()
    }

    @InputFiles
    Set<File> getDependencies() {
        if (uniqueDependencies.isEmpty()) {
            sourceToDependencies.get().values().each { FileCollection files ->
                uniqueDependencies.addAll(files.files)
            }
        }
        uniqueDependencies
    }

    @Optional
    @Input
    ConfigurableFileCollection getIncludeDirs() {
        return includeDirs
    }

    @Internal
    MapProperty<File, FileCollection> getInputFiles() {
        return sourceToDependencies
    }

    void calculateReverse() {

        Map<File, List<File>> reverse = [:]

        sourceToDependencies.get().each { source, dependencies ->
            dependencies.files.each { File dependency ->
                 if (reverse.containsKey(dependency)) {
                     reverse.get(dependency).add(source)
                 }

            }


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
