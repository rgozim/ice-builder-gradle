package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class)

    def iceHome
    def iceVersion
    def srcDist
    def freezeHome
    def sliceDir
    def slice2py
    def slice2java
    def slice2freezej
    def jarDir
    def cppPlatform
    def cppConfiguration
    def compat
    def _env

    Configuration(iceHome = null, freezeHome = null, cppConfiguration = null, cppPlatform = null, compat = null) {
        this.iceHome = iceHome ?: getIceHome()
        this.freezeHome = freezeHome

        // Guess the cpp platform and cpp configuration to use with Windows source builds
        this.cppConfiguration = cppConfiguration ?: System.getenv("CPP_CONFIGURATION")
        this.cppPlatform = cppPlatform ?: System.getenv("CPP_PLATFORM")

        def os = System.properties['os.name']

        if (this.iceHome != null) {
            srcDist = new File([this.iceHome, "java", "build.gradle"].join(File.separator)).exists()
            slice2py = getSlice2py(this.iceHome)
            slice2java = getSlice2java(this.iceHome)

            //
            // If freezeHome is not set we assume slice2freezej resides in the same location as slice2java
            // otherwise slice2freezej will be located in the freeze home bin directory.
            //
            slice2freezej = getSlice2freezej(this.freezeHome ? this.freezeHome : this.iceHome)

            //
            // Setup the environment required to run slice2java/slice2freezej commands
            //
            if (os.contains("Linux")) {
                def cppDir = srcDist ? "${this.iceHome}/cpp" : this.iceHome;

                def libdir = new File("${cppDir}/lib/i386-linux-gnu").exists() ?
                        "${cppDir}/lib/i386-linux-gnu" : "${cppDir}/lib"
                def lib64dir = new File("${cppDir}/lib/x86_64-linux-gnu").exists() ?
                        "${cppDir}/lib/x86_64-linux-gnu" : "${cppDir}/lib64"
                def env = [libdir, lib64dir]
                if (System.env.LD_LIBRARY_PATH) {
                    env.add(System.env.LD_LIBRARY_PATH)
                }
                _env = ["LD_LIBRARY_PATH=${env.join(File.pathSeparator)}"]
            }

            //
            // Retrieve the version of the Ice distribution being used
            //
            iceVersion = getIceVersion(this.iceHome)

            //
            // This can only happen if iceHome is set to a source distribution. In this case we log a warning
            // and return partially initialized. We DO NOT want to throw an exception because we could be in the
            // middle of a clean, in which case slice2java will be missing.
            //
            if (!iceVersion) {
                LOGGER.warn("Unable to determine the Ice version using slice2java (${slice2java}) from " +
                        "iceHome (${iceHome}). This is expected when cleaning.")
                return
            }

            //
            // --compat only available for Ice 3.7 and higher
            //
            if (SliceExtension.compareVersions(iceVersion, '3.7') >= 0) {
                this.compat = compat ?: false
            } else if (compat != null) {
                LOGGER.warn("Property \"slice.compat\" unavailable for Ice ${iceVersion}.")
            }

            //
            // Guess the slice and jar directories of the Ice distribution we are using
            //
            def sliceDirectories = [
                    [this.iceHome, "share", "slice"],                         // Common shared slice directory
                    [this.iceHome, "share", "ice", "slice"],                  // Ice >= 3.7
                    [this.iceHome, "share", "Ice-${iceVersion}", "slice"],   // Ice < 3.7
                    [this.iceHome, "slice"]                                   // Opt/source installs & Windows distribution
            ]

            def jarDirectories = [
                    [this.iceHome, "share", "java"],                          // Default usr install
                    [this.iceHome, this.compat ? "java-compat" : "java", "lib"],  // Source distribution
                    [this.iceHome, "lib"]                                     // Opt style install & Windows distribution
            ]

            def sliceDirCandidates = sliceDirectories.collect { it.join(File.separator) }
            def jarDirCandidates = jarDirectories.collect { it.join(File.separator) }

            sliceDir = sliceDirCandidates.find { new File(it).exists() }
            jarDir = jarDirCandidates.find { new File(it).exists() }

            if (!sliceDir) {
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
    def getWin32InstallDir(key) {
        def sout = new StringBuffer()
        def serr = new StringBuffer()
        def p = ["reg", "query", key, "/v", "InstallDir"].execute()
        p.waitForProcessOutput(sout, serr)
        if (p.exitValue() != 0) {
            return null
        }
        return sout.toString().split("    ")[3].trim()
    }

    //
    // Query Win32 registry and return the path of the latest Ice version available.
    //
    def getWin32IceHome() {
        def sout = new StringBuffer()
        def serr = new StringBuffer()

        def p = ["reg", "query", "HKLM\\Software\\ZeroC"].execute()
        p.waitForProcessOutput(sout, serr)
        if (p.exitValue() != 0) {
            //
            // reg query will fail if Ice is not installed
            //
            return ""
        }

        def iceInstallDir = null
        def iceVersion = null

        sout.toString().split("\\r?\\n").each {
            try {
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
            } catch (e) {
            }
        }
        return iceInstallDir
    }

    def getIceVersion(iceHome) {
        def slice2java = getSlice2java(iceHome)
        if (new File(slice2java).exists()) {
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
        } else if (!srcDist) {
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
    def getSliceCompiler(name, homeDir) {
        def os = System.properties['os.name']
        //
        // Check if we are using a Slice source distribution
        //
        def srcDist = new File([homeDir, "java", "build.gradle"].join(File.separator)).exists()
        def compilerName = os.contains('Windows') ? "${name}.exe" : name
        def sliceCompiler = null

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
            def basePath = srcDist ? [homeDir, "cpp", "bin", cppPlatform, cppConfiguration] : [homeDir, "tools"]
            basePath = basePath.join(File.separator)
            if (new File(basePath).exists()) {
                sliceCompiler = [basePath, compilerName].join(File.separator)
            }
        }

        if (sliceCompiler == null) {
            sliceCompiler = srcDist ?
                    [homeDir, "cpp", "bin", compilerName].join(File.separator) :
                    [homeDir, "bin", compilerName].join(File.separator)
        }

        return sliceCompiler
    }

    private def parseVersion(v) {
        if (v) {
            def vv = v.tokenize('.')
            if (v.indexOf('a') != -1) {
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

}