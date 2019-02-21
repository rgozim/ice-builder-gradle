// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import com.zeroc.gradle.icebuilder.slice.extensions.PythonExtension
import com.zeroc.gradle.icebuilder.slice.extensions.SliceExtension
import com.zeroc.gradle.icebuilder.slice.factories.PythonContainerFactory
import com.zeroc.gradle.icebuilder.slice.tasks.DependencyTask
import com.zeroc.gradle.icebuilder.slice.tasks.PythonTask
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

@CompileStatic
class SlicePlugin implements Plugin<Project> {

    SliceExtension slice

    void apply(Project project) {

        // def javaContainer = project.container(Java)
        def pythonContainer = project.container(PythonExtension, new PythonContainerFactory(project))

        // Create and install the extension object.
        slice = project.extensions.create("slice", SliceExtension,
                project, pythonContainer)

        configurePythonTasks(project, slice)
    }

    void configurePythonTasks(Project project, SliceExtension slice) {
        slice.python.all { PythonExtension pythonExt ->
            // Create task to calculate dependencies
            def preTask = createDependencyTask(project, pythonExt)
            createPythonTask(project, pythonExt, preTask)
        }
    }

    TaskProvider<DependencyTask> createDependencyTask(Project project, PythonExtension ext) {
        String name = "calculate${ext.name.capitalize()}Dependencies"
        project.tasks.register(name, DependencyTask, new Action<DependencyTask>() {
            @Override
            void execute(DependencyTask t) {
                t.with {
                    includeDirs.from(ext.includes)
                    inputFiles.from(ext.sources)
                    outputFile.set(project.layout.buildDirectory.file("ice/dependencies/${ext.name}.xml"))
                }
            }
        })
    }

//    TaskProvider<ParseDependenciesTask> createDependencyMapTask(Project project, PythonExtension ext,
//                                                                TaskProvider<DependencyTask> dependencyTask) {
//        String name = "parse${ext.name.capitalize()}Dependencies"
//        project.tasks.register(name, ParseDependenciesTask, new Action<ParseDependenciesTask>() {
//            @Override
//            void execute(ParseDependenciesTask t) {
//                t.with {
//                    dependsOn(dependencyTask)
//                    dependencyXmlFile.set(dependencyTask.get().outputFile)
//                }
//            }
//        })
//    }

    TaskProvider<PythonTask> createPythonTask(Project project, PythonExtension ext, TaskProvider<DependencyTask> pretask) {
        String taskName = "python" + ext.name.capitalize()
        project.tasks.register(taskName, PythonTask, new Action<PythonTask>() {
            @Override
            void execute(PythonTask t) {
                t.with {
                    dependsOn(pretask)
                    setSources(pretask.get().outputFile)
                    includeDirs.from(ext.includes)
                    outputDir.set(slice.outputDir)
                }
            }
        })
    }


//    def isAndroidProject(Project project) {
//        return project.hasProperty('android') && project.android.sourceSets
//    }

//    def getAndroidVariants(Project project) {
//        // https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/user-guide
//        return project.android.hasProperty('libraryVariants') ?
//                project.android.libraryVariants : project.android.applicationVariants
//    }

    // 1 is a > b
    // 0 if a == b
    // -1 if a < b
//    static def compareVersions(a, b) {
//        def verA = a.tokenize('.')
//        def verB = b.tokenize('.')
//
//        for (int i = 0; i < Math.min(verA.size(), verB.size()); ++i) {
//            if (verA[i] != verB[i]) {
//                return verA[i] <=> verB[i]
//            }
//        }
//        // Common indices match. Assume the longest version is the most recent
//        verA.size() <=> verB.size()
//    }
}
