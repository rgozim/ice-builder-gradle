package org.openmicroscopy.tasks


import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

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
}
