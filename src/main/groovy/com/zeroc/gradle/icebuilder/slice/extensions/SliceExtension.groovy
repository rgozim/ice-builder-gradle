// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice.extensions


import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
class SliceExtension {


    final NamedDomainObjectContainer<PythonExtension> python

    final DirectoryProperty outputDir

    SliceExtension(Project project,
                   NamedDomainObjectContainer<PythonExtension> python) {
        this.python = python
        this.outputDir = project.objects.directoryProperty()
        this.outputDir.convention(project.layout.buildDirectory.dir("ice"))
    }

    void python(Closure closure) {
        try {
            python.configure(closure)
        } catch (MissingPropertyException ex) {
            python.create('default', closure)
        }
    }
}
