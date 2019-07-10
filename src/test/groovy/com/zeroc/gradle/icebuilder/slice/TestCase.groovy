// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import static org.junit.Assume.assumeNotNull

class TestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    Project project
    SliceExtension slice

    @Before
    void createProject() {
        project = ProjectBuilder.builder()
                .withProjectDir(temporaryFolder.root)
                .build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        slice = project.extensions.getByType(SliceExtension)
    }

    @Before
    void checkIceInstalled() {
        assumeNotNull(slice.iceHome)
        assumeNotNull(slice.slice2java)
    }

    @After
    void cleanupProject() {
        project.delete()
        project = null
    }

    def newProjectWithProjectDir() {
        def p = ProjectBuilder.builder().withProjectDir(project.rootDir).build()
        p.pluginManager.apply 'java'
        p.pluginManager.apply 'slice'
        return p
    }

    void forceReinitialization() {
        // setting any variable forces reinitialization
        def iceHome = slice.iceHome
        slice.iceHome = iceHome
    }

}
