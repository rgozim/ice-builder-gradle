package org.openmicroscopy.tasks


import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

class IcePythonTask extends IceTask {

    @Input
    @Optional
    final Property<String> prefix = project.objects.property(String)

    IcePythonTask() {
        super("slice2py")
    }

    @TaskAction
    void action() {
        List<String> cmd = createBaseCompileSpec()

        // Add files for processing
        getSource().each { File file ->
            cmd.add(String.valueOf(file))
        }

        if (prefix.isPresent()) {
            // Set a prefix
            cmd.add("--prefix=" + prefix.get())
        }

        executeCommand(cmd)
    }

    ManyToMany<File, File> createDependencyGraph() {
        List<String> cmd = createBaseCompileSpec()

        // Add the source files
        source.files.each {
            cmd.add(String.valueOf(it))
        }

        // We only want dependency printout
        cmd.add("--depend-xml")

        StringBuffer sout = new StringBuffer()
        cmd.execute().waitForProcessOutput(sout, System.err)

        return parseSliceDependencyXML(sout)
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
    private ManyToMany<File, File> parseSliceDependencyXML(StringBuffer sout) {
        def xml = new XmlSlurper().parseText(sout.toString())
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        ManyToMany<File, File> results = new ManyToMany<>()
        xml.children().each {
            if (it.name() == "source") {
                File source = new File(it.attributes().get("name"))
                it.children().each {
                    if (it.name() == "dependsOn") {
                        File dependsOn = new File(it.attributes().get("name"))
                        results.put(source, dependsOn)
                    }
                }
            }
        }

        return results
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
        outputDir.set(dir)
    }

    void prefix(String text) {
        setPrefix(text)
    }

    void setPrefix(String text) {
        prefix.set(text)
    }

    void deleteOutputFile(File file) {
        // Convert the input filename to the output filename and
        // delete that file
        File targetFile = project.file("$outputDir/${getOutputFileName(file)}")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    String getOutputFileName(File file) {
        String temp = file.name
        String extension = FilenameUtils.getExtension(temp)
        String filename = FilenameUtils.getBaseName(temp)
        if (prefix.isPresent()) {
            filename = prefix.get() + filename + "_ice"
        }
        return "${filename}.{$extension}"
    }

}
