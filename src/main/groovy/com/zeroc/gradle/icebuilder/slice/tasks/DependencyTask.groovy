package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class DependencyTask extends DefaultTask {

    private static final def Log = Logging.getLogger(DependencyTask)

    @InputFiles
    FileCollection inputFiles

    @Input
    @Optional
    FileCollection includeDirs = project.files()

    // Default config
    @Internal
    Configuration config = new Configuration()

    @OutputFiles
    FileCollection result = project.files()

    // private FileCollection result = project.files()

    @TaskAction
    void apply() {
        List cmd = [config.slice2py, "-I${config.sliceDir}"]

        includeDirs.each { dir -> cmd.add("-I${dir}") }

        cmd.addAll(inputFiles.files)
        cmd.add("--depend-xml")

        def sout = new StringBuffer()
        def p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)

        Log.info(sout as String)

        def fileSet = parseSliceDependencyXML(sout)

        // These files are dependencies of the input ice files
        result = project.files(fileSet)

        // Log out
        // result.each { Log.info("$it") }
    }

    FileCollection getResult() {
        return result
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

    // Parse the dependency XML which is of the format:
    //
    // <dependencies>
    //   <source name="A.ice">
    //     <dependsOn name="Hello.ice"/>
    //   </source>
    //   <source name="Hello.ice">
    //   </source>
    // </dependencies>
    static Set parseSliceDependencyXML(StringBuffer sout) {
        def xml = new XmlSlurper().parseText(sout.toString())
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        Set dependencies = []
        xml.children().each {
            if (it.name() == "source") {
                it.children().each {
                    if (it.name() == "dependsOn") {
                        def dependsOn = new File(it.attributes().get("name"))
                        dependencies.add(dependsOn)
                    }
                }
            }
        }

        return dependencies
    }
}
