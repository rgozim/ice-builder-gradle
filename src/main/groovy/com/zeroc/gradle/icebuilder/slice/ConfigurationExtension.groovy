package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project

class ConfigurationExtension {

    private final Project project

    String iceHome

    String freezeHome

    String cppPlatform

    String cppConfiguration

    Boolean compat

    ConfigurationExtension(Project project) {
        this.project = project
    }

}
