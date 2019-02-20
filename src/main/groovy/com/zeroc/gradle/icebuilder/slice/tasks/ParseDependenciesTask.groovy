package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.utils.DependencyMap
import com.zeroc.gradle.icebuilder.slice.utils.DependencyParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

@SuppressWarnings("UnstableApiUsage")
class ParseDependenciesTask extends DefaultTask {

    @InputFile
    final RegularFileProperty dependencyXmlFile = project.objects.fileProperty()

    private Property<DependencyMap> dependencyMap = project.objects.property(DependencyMap)

    @TaskAction
    void apply() {
        // parseXml
        dependencyMap.set(DependencyParser.parseAsMap(dependencyXmlFile.get().asFile.text))
    }

    @Nested
    Property<DependencyMap> getDependencyMap() {
        return dependencyMap
    }

}
