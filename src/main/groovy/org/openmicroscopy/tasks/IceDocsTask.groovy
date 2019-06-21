package org.openmicroscopy.tasks


import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class IceDocsTask extends IceTask {

    @Input
    @Optional
    final Property<Boolean> underscore = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Integer> index = project.objects.property(Integer)

    @Input
    @Optional
    final Property<Integer> summary = project.objects.property(Integer)

    @InputFile
    @Optional
    final RegularFileProperty header = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty footer = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty indexHeader = project.objects.fileProperty()

    @InputFile
    @Optional
    final RegularFileProperty indexFooter = project.objects.fileProperty()

    IceDocsTask() {
        super("slice2html")
    }

    @TaskAction
    void apply() {
        List<String> cmd = createBaseCompileSpec()

        if (header.isPresent()) {
            cmd.addAll(["--hdr", String.valueOf(header.asFile.get())])
        }

        if (footer.isPresent()) {
            cmd.addAll(["--ftr", String.valueOf(footer.asFile.get())])
        }

        if (indexFooter.isPresent()) {
            cmd.addAll(["--indexhdr", String.valueOf(indexFooter.asFile.get())])
        }

        // Add the source files
        getSource().each {
            cmd.add(String.valueOf(it))
        }

        executeCommand(cmd)
    }

}
