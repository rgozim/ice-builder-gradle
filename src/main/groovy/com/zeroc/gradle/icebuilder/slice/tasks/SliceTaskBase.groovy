package com.zeroc.gradle.icebuilder.slice.tasks

import com.zeroc.gradle.icebuilder.slice.Configuration
import com.zeroc.gradle.icebuilder.slice.utils.DependencyMap
import com.zeroc.gradle.icebuilder.slice.utils.DependencyParser
import groovy.transform.CompileStatic
import groovy.transform.Internal
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

import javax.inject.Inject
import java.util.concurrent.Callable

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
abstract class SliceTaskBase extends DefaultTask {

    private static final Logger Log = Logging.getLogger(SliceTaskBase)

    private final ConfigurableFileCollection includeDirs = project.files()

    private final DirectoryProperty outputDir = project.objects.directoryProperty()

    private final Property<DependencyMap> sources = project.objects.property(DependencyMap)

    // Change this to a configuration
    protected Configuration config = new Configuration()

    @Inject
    ProviderFactory getProviders() {
        throw new UnsupportedOperationException()
    }

    @OutputDirectory
    DirectoryProperty getOutputDir() {
        return outputDir
    }

    @Internal
    Set<File> getSourceSet() {
        sources.get().keySet()
    }

    @Internal
    Set<File> getDependencySet() {
        sources.get().getDependenciesToSource().keySet()
    }

    @Optional
    @Input
    ConfigurableFileCollection getIncludeDirs() {
        return includeDirs
    }

    @Internal
    Property<DependencyMap> getSources() {
        return sources
    }

    @Internal
    Provider<Map<File, List<File>>> getDependencyMap() {
        providers.provider(new Callable<Map<File, List<File>>>() {
            @Override
            Map<File, List<File>> call() throws Exception {
                return sources.get().getDependenciesToSource()
            }
        })
    }

    void setSources(RegularFileProperty srcFile) {
        sources.set(providers.provider(new Callable<DependencyMap>() {
            @Override
            DependencyMap call() throws Exception {
                return DependencyParser.parseAsMap(srcFile.get().asFile.text)
            }
        }))
    }

    void setSources(Provider<? extends RegularFile> xmlFile) {
        sources.set(xmlFile.map { DependencyParser.parseAsMap(it.asFile.text) })
    }

    protected String getOutputFileName(File file) {
        def extension = FilenameUtils.getExtension(file.name)
        def filename = FilenameUtils.getBaseName(file.name)
        return "${filename}.{$extension}"
    }

    protected void deleteOutputFile(File file) {
        // Convert the input filename to the outputDir filename and
        // delete that file
        def targetFile = project.file("$outputDir/${getOutputFileName(file)}")
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    protected void executeCommand(List cmd) {
        def sout = new StringBuffer()
        def p = cmd.execute()

        p.waitForProcessOutput(sout, System.err)

        if (p.exitValue() != 0) {
            throw new GradleException("${cmd[0]} failed with exit code: ${p.exitValue()}")
        }
    }
}
