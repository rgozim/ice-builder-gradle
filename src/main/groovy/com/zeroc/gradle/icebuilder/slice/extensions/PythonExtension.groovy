package com.zeroc.gradle.icebuilder.slice.extensions

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class PythonExtension {
    final String name

    final Project project

    def args

    def include

    def files

    def srcDir

    File outputDir

    PythonExtension(name, project) {
        this.name = name
        this.project = project
        this.srcDir = project.file("src/main/slice")
    }
}
