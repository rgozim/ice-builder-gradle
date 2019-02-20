package com.zeroc.gradle.icebuilder.slice.factories

import com.zeroc.gradle.icebuilder.slice.extensions.PythonExtension
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

class PythonContainerFactory implements NamedDomainObjectFactory<PythonExtension> {

    private final Project project

    PythonContainerFactory(Project project) {
        this.project = project
    }

    @Override
    PythonExtension create(String name) {
        return new PythonExtension(name, this.project)
    }
    
}
