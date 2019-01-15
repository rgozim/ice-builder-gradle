package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import com.zeroc.gradle.icebuilder.slice.Dependency
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

@SuppressWarnings("UnstableApiUsage")
class DependencyTask extends DefaultTask {

    private static final def Log = Logging.getLogger(DependencyTask)

    @InputFiles
    final ConfigurableFileCollection inputFiles = project.files()

    @Optional
    @Input
    final ListProperty<Directory> includeDirs = project.objects.listProperty(Directory)


    Set results

    @Internal
    private Configuration config = new Configuration()

    @TaskAction
    void apply() {
        List<String> cmd = [config.slice2py, "-I${config.sliceDir}"]

        for (Directory dir : includeDirs.get()) {
            cmd.add("-I" + dir.asFile.toString())
        }

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

    @InputFiles
    private FileCollection getIncludedFiles() {
        FileCollection files = getProject().files()
        for (Directory dir : includeDirs.get()) {
            files = files + project.fileTree(dir.asFile)
        }
        return files
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
    private Set<Dependency> parseSliceDependencyXML(StringBuffer sout) {
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
                            Set sourceList = [source]
                            dependencies.add(new Dependency(sourceList, dependsOn))
                        }
                    }
                }
            }
        }

        return dependencies
    }


}
