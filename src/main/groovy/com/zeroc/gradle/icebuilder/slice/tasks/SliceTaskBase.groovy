package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

abstract class SliceTaskBase extends DefaultTask {

    private static final def Log = Logging.getLogger(SliceTaskBase)

    @InputFiles
    FileCollection inputFiles

    @OutputDirectory
    File outputDir

    @Input
    @Optional
    FileCollection includeDirs = project.files()

    @InputFiles
    @Optional
    FileCollection dependencies = project.files()

    // Change this to a configuration
    Configuration config = new Configuration()

    void inputFiles(Object... files) {
        setInputFiles(files)
    }

    void inputFiles(FileCollection collection) {
        setInputFiles(collection)
    }

    void setInputFiles(FileCollection collection) {
        if (inputFiles) {
            inputFiles = inputFiles + collection
        } else {
            inputFiles = collection
        }
    }

    void setInputFiles(Object... files) {
        setInputFiles(project.files(files))
    }

    void sources(DependencyTask inputTask) {
        setSources(inputTask)
    }

    void setSources(DependencyTask task) {
        setInputFiles(project.files(task))
        // setIncludeDirs(project.files(task.includeDirs))
    }

    void includeDirs(FileCollection collection) {
        setIncludeDirs(collection)
    }

    void includeDirs(Object... paths) {
        setIncludeDirs(paths)
    }

    void setIncludeDirs(FileCollection collection) {
        if (includeDirs) {
            includeDirs = includeDirs + collection
        } else {
            includeDirs = collection
        }
    }

    void setIncludeDirs(Object... dirs) {
        setIncludeDirs(project.files(dirs))
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
        outputDir = dir
    }

    void prefix(String text) {
        setPrefix(text)
    }

    void setPrefix(String text) {
        prefix = text
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
