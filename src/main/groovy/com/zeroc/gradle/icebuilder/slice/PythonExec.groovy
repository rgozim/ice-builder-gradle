package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.AbstractExecTask

class PythonExec extends AbstractExecTask {

    PythonExec() {
        super(PythonExec.class)
        super.executable(project.slice.slice2py)
        super.args("-I${project.slice.sliceDir}")
    }

    def include(Object... arguments) {
        List modified = arguments.collect { "-I${it}" }
        return super.args(modified)
    }

    def prefix(String prefix) {
        return super.args("--prefix=${prefix}")
    }

    def outputDir(File dir) {
        return super.args("--output-dir=${dir}")
    }

    def iceFiles(FileCollection files) {
        return super.args(files.getFiles())
    }

}
