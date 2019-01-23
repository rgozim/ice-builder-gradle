package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

@SuppressWarnings("UnstableApiUsage")
class DependencyTask extends DefaultTask {

    private static final def Log = Logging.getLogger(DependencyTask)

    @InputFiles
    final ConfigurableFileCollection inputFiles = project.layout.configurableFiles()

    @Optional
    @Input
    final ConfigurableFileCollection includeDirs = project.layout.configurableFiles()

    @Internal
    Set results

    @Internal
    private Configuration config = new Configuration()

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
        results = fileSet
    }

//    void includeDirs(FileCollection collection) {
//        setIncludeDirs(collection)
//    }
//
//    void includeDirs(Object... paths) {
//        setIncludeDirs(paths)
//    }
//
//    void setIncludeDirs(FileCollection collection) {
//        if (includeDirs) {
//            includeDirs = includeDirs + collection
//        } else {
//            includeDirs = collection
//        }
//    }
//
//    void setIncludeDirs(Object... dirs) {
//        setIncludeDirs(project.files(dirs))
//    }

    // Parse the dependency XML which is of the format:
    //
    // <dependencies>
    //   <source name="A.ice">
    //     <dependsOn name="Hello.ice"/>
    //   </source>
    //   <source name="Hello.ice">
    //   </source>
    // </dependencies>
    Set<Dependency> parseSliceDependencyXML(StringBuffer sout) {
        def xml = new XmlSlurper().parseText(sout.toString())
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        Set<Dependency> dependencies = []
        xml.children().each {
            if (it.name() == "source") {
                File source = new File(it.attributes().get("name"))
                it.children().each {
                    if (it.name() == "dependsOn") {
                        File dependsOn = new File(it.attributes().get("name"))

                        def dependency = dependencies.find {
                            it.self == dependsOn
                        }

                        if (dependency) {
                            dependency.sources.add(source)
                        } else {
                            List sourceList = [source]
                            dependencies.add(new Dependency(sourceList, dependsOn))
                        }
                    }
                }
            }
        }

        return dependencies
    }

    static class Dependency {
        List sources = []

        File self

        Dependency(List sources, File self) {
            this.sources = sources
            this.self = self
        }

        @Override
        String toString() {
            def s = "Sources with dependency $self : \n"
            sources.each {
                s = s + "$it\n"
            }
            return s
        }
    }
}
