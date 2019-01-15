package com.zeroc.gradle.icebuilder.slice.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.AbstractExecTask

class DependencyExecTask extends AbstractExecTask {

    DependencyExecTask() {
        super(DependencyExecTask)
        super.executable(project.slice.slice2py)
        super.args("-I${project.slice.sliceDir}")
    }

    def include(Object... arguments) {
        List modified = arguments.collect { "-I${it}" }
        return super.args(modified)
    }

    def iceFiles(FileCollection files) {
        return super.args(files.getFiles())
    }

}
