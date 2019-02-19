// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice.extensions

import com.zeroc.gradle.icebuilder.slice.Java
import org.gradle.api.NamedDomainObjectContainer

class SliceExtension {

    NamedDomainObjectContainer<Java> java

    NamedDomainObjectContainer<PythonExtension> python

    File output

    SliceExtension(NamedDomainObjectContainer<Java> java,
                   NamedDomainObjectContainer<PythonExtension> python) {
        this.java = java
        this.python = python
    }

    void java(Closure closure) {
        try {
            java.configure(closure)
        } catch (MissingPropertyException ex) {
            java.create('default', closure)
        }
    }

    void python(Closure closure) {
        try {
            python.configure(closure)
        } catch (MissingPropertyException ex) {
            python.create('default', closure)
        }
    }
}
