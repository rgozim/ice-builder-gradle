// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import com.zeroc.gradle.icebuilder.slice.extensions.ConfigurationExtension
import com.zeroc.gradle.icebuilder.slice.extensions.PythonExtension
import com.zeroc.gradle.icebuilder.slice.extensions.SliceExtension
import com.zeroc.gradle.icebuilder.slice.tasks.PythonTask;
import org.gradle.api.Plugin
import org.gradle.api.Project

class SlicePlugin implements Plugin<Project> {

    final String GROUP = "slice"

    void apply(Project project) {
        // Create and install the extension object.
        SliceExtension slice = project.extensions.create("slice", SliceExtension,
                project.container(Java),
                project.container(PythonExtension, { new PythonExtension(it, project) })
        )

        // Default output location for generated slice output
        slice.output = project.file("${project.buildDir}/generated-src")

        // Configuration extension for customising ice env variables
        ConfigurationExtension configExt = slice.extensions.create( "config", ConfigurationExtension, project)

        // Configure SliceTask
        project.afterEvaluate {
            def sliceConfig = new Configuration(
                    configExt.iceHome,
                    configExt.freezeHome,
                    configExt.cppConfiguration,
                    configExt.cppPlatform,
                    configExt.compat
            )

            slice.java.all { Java javaExt ->


            }

            slice.python.all { PythonExtension pythonExt ->
                def taskName = "python${pythonExt.name.capitalize()}"
                def pythonTask = project.tasks.create(taskName, PythonTask) {
                    group = GROUP
                    config = sliceConfig
                }
                def compileJava = project.tasks.getByName("compileJava")
                if (compileJava) {
                    compileJava.dependsOn pythonTask
                }
            }
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

    // 1 is a > b
    // 0 if a == b
    // -1 if a < b
    static def compareVersions(a, b) {
        def verA = a.tokenize('.')
        def verB = b.tokenize('.')

        for (int i = 0; i < Math.min(verA.size(), verB.size()); ++i) {
            if (verA[i] != verB[i]) {
                return verA[i] <=> verB[i]
            }
        }
        // Common indices match. Assume the longest version is the most recent
        verA.size() <=> verB.size()
    }
}
