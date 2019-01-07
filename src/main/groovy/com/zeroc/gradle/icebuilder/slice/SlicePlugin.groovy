// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.logging.Logging
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

class SlicePlugin implements Plugin<Project> {
    private static final def LOGGER = Logging.getLogger(SliceTask)

    void apply(Project project) {
        def compileSlice = project.tasks.create('compileSlice', SliceTask) {
            group = "Slice"
        }

        // Create and install the extension object.
        def slice = project.extensions.create("slice", SliceExtension,
                project.container(Java), project.container(Python))

        // slice.extensions.add("python", project.container(Python))

        slice.extensions.create("freezej", Freezej,
                project.container(Dict), project.container(Index))

        slice.output = project.file("${project.buildDir}/generated-src")

        if (isAndroidProject(project)) {
            project.afterEvaluate {
                // Android projects do not define a 'compileJava' task. We wait until the project is evaluated
                // and add our dependency to the variant's javaCompiler task.
                getAndroidVariants(project).all { variant ->
                    variant.registerJavaGeneratingTask(project.tasks.getByName('compileSlice'), slice.output)
                }
            }
        } else {
            def compileJava = project.tasks.getByName("compileJava")
            if (compileJava) {
                compileJava.dependsOn('compileSlice')
            }
            project.sourceSets.main.java.srcDir compileSlice.output
        }
    }

    def isAndroidProject(Project project) {
        return project.hasProperty('android') && project.android.sourceSets
    }

    def getAndroidVariants(Project project) {
        // https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/user-guide
        return project.android.hasProperty('libraryVariants') ?
                project.android.libraryVariants : project.android.applicationVariants
    }
}
