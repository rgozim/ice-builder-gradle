package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class DependencyTask extends DefaultTask {

    @InputFiles
    FileCollection inputFiles

    @Input
    @Optional
    FileCollection includeDirs

    Configuration config = new Configuration()

    @TaskAction
    void apply() {
        List cmd = [config.slice2py, "-I${config.sliceDir}"]

        if (includeDirs) {
            // Add any additional includes
            includeDirs.each { dir -> cmd.add("-I${dir}") }
        }

        cmd.addAll(inputFiles.files)
        cmd.add("--depend-xml")

        def sout = new StringBuffer()
        def p = cmd.execute()
        p.waitForProcessOutput(sout, System.err)

        // These files are dependencies of the input ice files
        this.outputs = project.files(parseSliceDependencyXML(sout))
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
