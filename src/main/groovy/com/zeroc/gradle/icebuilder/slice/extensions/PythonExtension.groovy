package com.zeroc.gradle.icebuilder.slice.extensions

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty

@CompileStatic
class PythonExtension {

    private final Project project

    final String name

    final ConfigurableFileCollection includes

    final ConfigurableFileCollection sources

    final DirectoryProperty sourceDir

    final DirectoryProperty outputDir

    def args

    PythonExtension(String name, Project project) {
        this.name = name
        this.project = project
        this.includes = project.files()
        this.sources = project.files()
        this.sourceDir = project.objects.directoryProperty()
        this.outputDir = project.objects.directoryProperty()

        // this.srcDir = project.file("src/main/slice")
    }
}
