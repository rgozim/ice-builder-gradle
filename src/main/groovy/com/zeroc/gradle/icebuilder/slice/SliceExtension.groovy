//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.gradle.api.NamedDomainObjectContainer

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SliceExtension {

    final NamedDomainObjectContainer<Java> java

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

    private static Configuration configuration = null
    private static final def LOGGER = Logging.getLogger(SliceExtension)

    class Configuration {

        def _iceHome = null
        def _iceVersion = null
        def _srcDist = false
        def _freezeHome = null
        def _sliceDir = null
        def _slice2py = null
        def _slice2java = null
        def _slice2freezej = null
        def _jarDir = null
        def _cppPlatform = null
        def _cppConfiguration = null
        def _compat = null
        def _env = []

        Configuration(iceHome = null, freezeHome = null) {
            _iceHome = iceHome ?: getIceHome();
            _freezeHome = freezeHome

            // Guess the cpp platform and cpp configuration to use with Windows source builds
            _cppConfiguration = cppConfiguration ?: System.getenv("CPP_CONFIGURATION")
            _cppPlatform = cppPlatform ?: System.getenv("CPP_PLATFORM")

            def os = System.properties['os.name']

            if(_iceHome != null) {
                _srcDist = new File([_iceHome, "java", "build.gradle"].join(File.separator)).exists()
                _slice2py = getSlice2py(_iceHome)
                _slice2java = getSlice2java(_iceHome)

                //
                // If freezeHome is not set we assume slice2freezej resides in the same location as slice2java
                // otherwise slice2freezej will be located in the freeze home bin directory.
                //
                _slice2freezej = getSlice2freezej(_freezeHome ? _freezeHome : _iceHome)

                //
                // Setup the environment required to run slice2java/slice2freezej commands
                //
                if(os.contains("Linux")) {
                    def cppDir = _srcDist ? "${_iceHome}/cpp" : _iceHome;

                    def libdir = new File("${cppDir}/lib/i386-linux-gnu").exists() ?
                        "${cppDir}/lib/i386-linux-gnu" : "${cppDir}/lib"
                    def lib64dir = new File("${cppDir}/lib/x86_64-linux-gnu").exists() ?
                        "${cppDir}/lib/x86_64-linux-gnu" : "${cppDir}/lib64"
                    def env = [libdir, lib64dir]
                    if(System.env.LD_LIBRARY_PATH) {
                        env.add(System.env.LD_LIBRARY_PATH)
                    }
                    _env = ["LD_LIBRARY_PATH=${env.join(File.pathSeparator)}"]
                }

                //
                // Retrieve the version of the Ice distribution being used
                //
                _iceVersion = getIceVersion(_iceHome)

                //
                // This can only happen if iceHome is set to a source distribution. In this case we log a warning
                // and return partially initialized. We DO NOT want to throw an exception because we could be in the
                // middle of a clean, in which case slice2java will be missing.
                //
                if(!_iceVersion) {
                    LOGGER.warn("Unable to determine the Ice version using slice2java (${slice2java}) from " +
                                "iceHome (${iceHome}). This is expected when cleaning.")
                    return
                }

                //
                // --compat only available for Ice 3.7 and higher
                //
                if(SliceExtension.compareVersions(_iceVersion, '3.7') >= 0) {
                    _compat = compat ?: false
                } else if(compat != null) {
                    LOGGER.warn("Property \"slice.compat\" unavailable for Ice ${_iceVersion}.")
                }

                //
                // Guess the slice and jar directories of the Ice distribution we are using
                //
                List<Path> sliceDirectories = [
                    Paths.get(_iceHome, "share", "slice"),                         // Common shared slice directory
                    Paths.get(_iceHome, "share", "ice", "slice"),                  // Ice >= 3.7
                    Paths.get(_iceHome, "share", "Ice-${_iceVersion}", "slice"),   // Ice < 3.7
                    Paths.get(_iceHome, "slice")                                   // Opt/source installs & Windows distribution
                ]

                List<Path> jarDirectories = [
                    Paths.get(_iceHome, "share", "java"),                          // Default usr install
                    Paths.get(_iceHome, _compat ? "java-compat" : "java", "lib"),  // Source distribution
                    Paths.get(_iceHome, "lib")                                     // Opt style install & Windows distribution
                ]

                _sliceDir = sliceDirectories.find { Files.exists(it) }.toString()
                _jarDir = jarDirectories.find { Files.exists(it) }.toString()

                if (!_sliceDir) {
                    LOGGER.warn("Unable to locate slice directory in iceHome (${iceHome})")
                }
            }
        }

        def getIceHome() {
            if (System.env.ICE_HOME != null) {
                return System.env.ICE_HOME
            }

            def os = System.properties['os.name']
            if (os == "Mac OS X") {
                return "/usr/local"
            } else if (os.contains("Windows")) {
                return getWin32IceHome()
            } else {
                return "/usr"
            }
        }

        //
        // Query Win32 registry key and return the InstallDir value for the given key
        //
        String getWin32InstallDir(key) {
            StringBuffer sout = new StringBuffer()
            StringBuffer serr = new StringBuffer()
            Process p = ["reg", "query", key, "/v", "InstallDir", "/reg:64"].execute()
            p.waitForProcessOutput(sout, serr)
            if (p.exitValue() != 0) {
                return null
            }
            return sout.toString().split("    ")[3].trim()
        }

        //
        // Query Win32 registry and return the path of the latest Ice version available.
        //
        String getWin32IceHome() {
            StringBuffer sout = new StringBuffer()
            StringBuffer serr = new StringBuffer()

            List<String> cmd = ["reg", "query", "HKLM\\Software\\ZeroC", "/reg:64"]
            Process p = cmd.execute()
            p.waitForProcessOutput(sout, serr)
            if (p.exitValue() != 0) {
                //
                // reg query will fail if Ice is not installed
                //
                return ""
            }

            String iceInstallDir = null
            def iceVersion = null

            String output = sout.toString()
            output.split("\\r?\\n").each {
                try{
                    if (it.indexOf("HKEY_LOCAL_MACHINE\\Software\\ZeroC\\Ice") != -1) {
                        def installDir = getWin32InstallDir(it)
                        if (installDir != null) {
                            def version = getIceVersion(installDir)
                            if (iceVersion == null || compareVersions(version, iceVersion) == 1) {
                                iceInstallDir = installDir
                                iceVersion = version
                            }
                        }
                    }
                } catch(e){
                }
            }
            return iceInstallDir
        }

        def getIceVersion(iceHome) {
            def slice2java = getSlice2java(iceHome)
            if(new File(slice2java).exists()) {
                def command = [slice2java, "--version"]
                def sout = new StringBuffer()
                def serr = new StringBuffer()
                def p = command.execute(_env, null)
                p.waitForProcessOutput(sout, serr)
                if (p.exitValue() != 0) {
                    println serr.toString()
                    throw new GradleException("${command[0]} command failed: ${p.exitValue()}")
                }
                return serr.toString().trim()
            } else if(!_srcDist) {
                // Only throw an exception if we are not using a source distribution. A binary distribution should
                // always have slice2java, howerver a source distribution may not. For example, during a clean.
                throw new GradleException("slice2java (${slice2java}) not found. Please ensure that Ice is installed " +
                                          "and the iceHome property (${iceHome}) is correct.")
            } else {
                return null;
            }
        }

        def getSlice2py(iceHome) {
            return getSliceCompiler("slice2py", iceHome)
        }

        def getSlice2java(iceHome) {
            return getSliceCompiler("slice2java", iceHome)
        }

        def getSlice2freezej(freezeHome) {
            return getSliceCompiler("slice2freezej", freezeHome)
        }

        //
        // Return the path to the specified slice compiler (slice2java|slice2freezej) with respect to
        // the specified homeDir (iceHome|freezeHome)
        //
        def getSliceCompiler(String name, String homeDir) {
            String os = System.properties['os.name']
            //
            // Check if we are using a Slice source distribution
            //
            boolean srcDist = Files.exists(Paths.get(homeDir, "java", "build.gradle"))
            String compilerName = os.contains('Windows') ? name + ".exe" : name
            Path sliceCompiler = null

            //
            // Set the location of the sliceCompiler executable
            //
            if (os.contains("Windows")) {

                //
                // For Windows source distribution we first check for <IceHome>\cpp\bin\<cppPlatform>\<cppConfiguration>
                // that correspond with Ice 3.7 or greater source distribution.
                ///
                // For Windows binary distribution we first check for <IceHome>\tools that correspond with NuGet package
                // layout.
                //
                Path basePath = srcDist ?
                        Paths.get(homeDir, "cpp", "bin", _cppPlatform, _cppConfiguration) :
                        Paths.get(homeDir, "tools")
                if (Files.exists(basePath)) {
                    sliceCompiler = basePath.resolve(compilerName)
                }
            }

            if (sliceCompiler == null) {
                sliceCompiler = srcDist ?
                        Paths.get(homeDir, "cpp", "bin", compilerName) :
                        Paths.get(homeDir, "bin", compilerName)
            }

            return sliceCompiler.toString()
        }
    }

    SliceExtension(java) {
        this.java = java
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        } catch(MissingPropertyException ex) {
            java.create('default', closure)
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

        Configuration c = new Configuration(iceHome, freezeHome)

        iceHome = c._iceHome
        iceVersion = c._iceVersion
        iceArtifactVersion = parseVersion(c._iceVersion)
        srcDist = c._srcDist
        freezeHome = c._freezeHome
        sliceDir = c._sliceDir
        slice2py = c._slice2py
        slice2java = c._slice2java
        slice2freezej = c._slice2freezej
        jarDir = c._jarDir
        cppPlatform = c._cppPlatform
        cppConfiguration = c._cppConfiguration
        compat = c._compat
        env = c._env

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
