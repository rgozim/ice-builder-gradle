package com.zeroc.gradle.icebuilder.slice

class SliceJavaTaskTest extends AbstractTest {

    public static final String PROJECT_NAME = 'omero-api-plugin'

    File iceSrcDir

    def setup() {
        iceSrcDir = temporaryFolder.newFolder("src", "main", "slice")

        setupBuildfile()
    }

    def "succeeds with minimal configuration"() {
        given:
        File outputDir = new File(projectDir, "build/generated/slice/java")

        File iceFile = new File(iceSrcDir, "test.ice")
        iceFile << setupSimpleIceFile()

        when:
        build("compileSlice")

        then:
        outputDir.listFiles().length > 0
    }

    def "succeeds with custom ice source directory"() {
        given:
        File outputDir = new File(projectDir, "build/generated/slice/java")

        File customSliceDir = temporaryFolder.newFolder("src", "custom", "slice")
        File iceFile = new File(customSliceDir, "test.ice")
        iceFile << setupSimpleIceFile()

        buildFile << """
            slice.java {
                srcDir = "$customSliceDir"
            }
        """

        when:
        build("compileSlice")

        then:
        outputDir.listFiles().length > 0
    }

    def "outputs to correct package directory"() {
        given:
        File outputDir = new File(projectDir, "build/generated/slice/java/com/zeroc")

        File iceFile = new File(iceSrcDir, "test.ice")
        iceFile << setupJavaIceFileWithPackage()

        when:
        build("compileSlice")

        then:
        outputDir.listFiles().length > 0
    }

    @Override
    String getBuildFileName() {
        'build.gradle'
    }

    @Override
    String getSettingsFileName() {
        'settings.gradle'
    }

    private String setupSimpleIceFile() {
        """
            |module Test
            |{
            |
            |interface Hello
            |{
            |    idempotent void sayHello(int delay);
            |    void shutdown();
            |};
            |
            |};
        """.stripMargin()
    }

    private String setupJavaIceFileWithPackage() {
        """
            |#ifdef __SLICE2JAVA_COMPAT__
            |[[\"java:package:com.zeroc\"]]        [[\"java:package:com.zeroc\"]]
            |#endif
            |
            |module Test
            |{
            |
            |interface Hello
            |{
            |    idempotent void sayHello(int delay);
            |    void shutdown();
            |};
            |
            |};
        """.stripMargin()
    }

    protected void setupBuildfile() {
        buildFile << """
            plugins {
                id 'java'
                id 'org.openmicroscopy.gradle.ice-builder.slice'
            }

            repositories {
                jcenter()
            }
        """
    }

    static String groovySettingsFile() {
        """
            rootProject.name = '$PROJECT_NAME'
        """
    }

}
