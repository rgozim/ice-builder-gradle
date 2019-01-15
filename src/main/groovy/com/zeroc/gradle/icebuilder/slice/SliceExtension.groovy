// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.logging.Logging
import org.gradle.api.NamedDomainObjectContainer

class SliceExtension {

    final NamedDomainObjectContainer<Java> java

    final NamedDomainObjectContainer<Python> python

    private static final def LOGGER = Logging.getLogger(SliceExtension)

    private def iceHome = null
    private def iceVersion = null
    private def iceArtifactVersion = null
    private def srcDist = false
    private def freezeHome = null
    private def sliceDir = null
    private def slice2py = null
    private def slice2java = null
    private def slice2freezej = null
    private def jarDir = null
    private def cppPlatform = null
    private def cppConfiguration = null
    private def compat = null

    private def env = []
    private def initialized = false
    def output

    private Configuration configuration = null

    SliceExtension(java, python) {
        this.java = java
        this.python = python
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        } catch(MissingPropertyException ex) {
            java.create('default', closure)
        }
    }

    def python(Closure closure) {
        try {
            python.configure(closure)
        } catch(MissingPropertyException ex) {
            python.create('default', closure)
        }
    }

    private def parseVersion(v) {
        if(v) {
            def vv = v.tokenize('.')
            if(v.indexOf('a') != -1) {
                return "${vv[0]}.${vv[1].replace('a', '.0-alpha')}"
            } else if (v.indexOf('b') != -1) {
                return "${vv[0]}.${vv[1].replace('b', '.0-beta')}"
            } else {
                return v
            }
        } else {
            return null;
        }
    }

    private void init() {
        LOGGER.debug('Initializing configuration')
        initialized = true // must happen before calling setters

        configuration = new Configuration(iceHome, freezeHome, cppConfiguration, cppPlatform, compat)
        iceHome = configuration._iceHome
        iceVersion = configuration._iceVersion
        iceArtifactVersion = parseVersion(configuration._iceVersion)
        srcDist = configuration._srcDist
        freezeHome = configuration._freezeHome
        sliceDir = configuration._sliceDir
        slice2py = configuration._slice2py
        slice2java = configuration._slice2java
        slice2freezej = configuration._slice2freezej
        jarDir = configuration._jarDir
        cppPlatform = configuration._cppPlatform
        cppConfiguration = configuration._cppConfiguration
        compat = configuration._compat
        env = configuration._env

        LOGGER.debug("Property: iceHome = ${iceHome}")
        LOGGER.debug("Property: iceVersion = ${iceVersion}")
        LOGGER.debug("Property: srcDist = ${srcDist}")
        LOGGER.debug("Property: freezeHome = ${freezeHome}")
        LOGGER.debug("Property: sliceDir = ${sliceDir}")
        LOGGER.debug("Property: slice2py = ${slice2py}")
        LOGGER.debug("Property: slice2java = ${slice2java}")
        LOGGER.debug("Property: slice2freezej = ${slice2freezej}")
        LOGGER.debug("Property: jarDir = ${jarDir}")
        LOGGER.debug("Property: cppPlatform = ${cppPlatform}")
        LOGGER.debug("Property: cppConfiguration = ${cppConfiguration}")
        LOGGER.debug("Property: compat = ${compat}")
        LOGGER.debug("Property: env = ${env}")

        assert initialized == true
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

    // Compare iceVersion with version
    // 1 is iceVersion > version
    // 0 if iceVersion == version
    // -1 if iceVersion < version
    def compareIceVersion(version) {
        lazyInit()
        return compareVersions(iceVersion, version)
    }

    def getIceHome() {
        lazyInit()
        return iceHome
    }

    def setIceHome(value) {
        iceHome = value
        initialized = false
    }

    def getIceVersion() {
        lazyInit()
        return iceVersion
    }

    def getIceArtifactVersion() {
        lazyInit()
        return iceArtifactVersion
    }

    def getSrcDist() {
        lazyInit()
        return srcDist
    }

    def getFreezeHome() {
        lazyInit()
        return freezeHome
    }

    def setFreezeHome(value) {
        freezeHome = value
        initialized = false
    }

    def getSliceDir() {
        lazyInit()
        return sliceDir
    }

    def getSlice2py() {
        lazyInit()
        return slice2py
    }

    def getSlice2java() {
        lazyInit()
        return slice2java
    }

    def getSlice2freezej() {
        lazyInit()
        return slice2freezej
    }

    def getJarDir() {
        lazyInit()
        return jarDir
    }

    def getCppPlatform() {
        lazyInit()
        return cppPlatform
    }

    def setCppPlatform(value) {
        cppPlatform = value
        initialized = false
    }

    def getCppConfiguration() {
        lazyInit()
        return cppConfiguration
    }

    def setCppConfiguration(value) {
        cppConfiguration = value
        initialized = false
    }

    def getCompat() {
        lazyInit()
        return compat
    }

    def setCompat(value) {
        compat = value
        initialized = false
    }

    def getEnv() {
        lazyInit()
        return env
    }

    def lazyInit() {
        if(!initialized) {
            init()
        }
    }
}
