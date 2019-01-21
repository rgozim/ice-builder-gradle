package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

@SuppressWarnings("UnstableApiUsage")
class DependencyTask extends DefaultTask {

    private static final def Log = Logging.getLogger(DependencyTask)

    @InputFiles
    final ListProperty<RegularFile> inputFiles = project.objects.listProperty(RegularFile)

    @Optional
    @Input
    final ConfigurableFileCollection includeDirs = project.layout.configurableFiles()

    // @OutputFiles
    // final ConfigurableFileCollection outputFiles = project.layout.configurableFiles()

    @Internal
    final MapProperty<File, List> results = project.objects.mapProperty(File, List)

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

        // Set result
        results.set(fileSet)
        // outputFiles.setFrom(fileSet)
    }

//    FileCollection getResult() {
//        return result
//    }
//
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
    Map<File, List<File>> parseSliceDependencyXML(xml) {
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        Map<File, List<File>> dependencies = [:]
        xml.children().each {
            if (it.name() == "source") {
                def source = it.attributes().get("name")
                def files = []
                it.children().each {
                    if (it.name() == "dependsOn") {
                        def dependsOn = new File(it.attributes().get("name"))
                        files.add(dependsOn)
                    }
                }
                dependencies.put(new File(source), files)
            }
        }
        return dependencies
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
//    static Set<File> parseSliceDependencyXML(StringBuffer sout) {
//        def xml = new XmlSlurper().parseText(sout.toString())
//        if (xml.name() != "dependencies") {
//            throw new GradleException("malformed XML")
//        }
//
//        Set dependencies = []
//        xml.children().each {
//            if (it.name() == "source") {
//                it.children().each {
//                    if (it.name() == "dependsOn") {
//                        def dependsOn = new File(it.attributes().get("name"))
//                        dependencies.add(dependsOn)
//                    }
//                }
//            }
//        }
//
//        return dependencies
//    }
}
