package com.zeroc.gradle.icebuilder.slice.tasks


import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

@SuppressWarnings("UnstableApiUsage")
class ParseDependenciesTask extends DefaultTask {

    @InputFile
    final RegularFileProperty dependencyXmlFile = project.objects.fileProperty()

    private final MapProperty<RegularFileProperty, FileCollection> inputFiles =
            project.objects.mapProperty(File, FileCollection)

    @TaskAction
    void apply() {
        // parseXml
        def xml = new XmlSlurper().parseText(dependencyXmlFile.get().asFile.text)
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        xml.children().each {
            if (it.name() == "source") {
                RegularFileProperty source = project.objects.fileProperty()
                source.set(new File(it.attributes().get("name")))

                List<File> dependencies = []
                it.children().each {
                    if (it.name() == "dependsOn") {
                        File dependsOn = new File(it.attributes().get("name"))
                        dependencies.add(dependsOn)
                    }
                }

                inputFiles.put(source, project.files(dependencies))
            }
        }
    }

    @Nested
    MapProperty<RegularFileProperty, FileCollection> getInputFiles() {
        return inputFiles
    }


}
