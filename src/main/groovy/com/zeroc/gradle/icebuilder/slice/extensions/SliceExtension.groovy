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

    SliceExtension(java, python) {
        this.java = java
        this.python = python
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        } catch (MissingPropertyException ex) {
            java.create('default', closure)
        }
    }

    def python(Closure closure) {
        try {
            python.configure(closure)
        } catch (MissingPropertyException ex) {
            python.create('default', closure)
        }
    }
}
