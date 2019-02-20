// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice.extensions

import com.zeroc.gradle.icebuilder.slice.Java
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

@SuppressWarnings("UnstableApiUsage")
@CompileStatic
class SliceExtension {

    // final NamedDomainObjectContainer<Java> java

    final NamedDomainObjectContainer<PythonExtension> python

    final DirectoryProperty output

    SliceExtension(Project project,
                   NamedDomainObjectContainer < Java > java,
                   NamedDomainObjectContainer<PythonExtension> python) {
        // this.java = java
        this.python = python
        this.output = project.objects.directoryProperty()
        this.output.convention(project.layout.buildDirectory.dir("ice"))
    }

//    void java(Closure closure) {
//        try {
//            java.configure(closure)
//        } catch (MissingPropertyException ex) {
//            java.create('default', closure)
//        }
//    }

    void python(Closure closure) {
        try {
            python.configure(closure)
        } catch (MissingPropertyException ex) {
            python.create('default', closure)
        }
    }
}
