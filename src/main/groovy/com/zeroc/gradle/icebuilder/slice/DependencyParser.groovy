package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.GradleException

class DependencyParser {

    // Parse the dependency XML which is of the format:
    //
    // <dependencies>
    //   <source name="A.ice">
    //     <dependsOn name="Hello.ice"/>
    //   </source>
    //   <source name="Hello.ice">
    //   </source>
    // </dependencies>
    static Set<Dependency> parseSliceDependencyXML(String text) {
        def xml = new XmlSlurper().parseText(text)
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        Set<Dependency> dependencies = []
        xml.children().each {
            if (it.name() == "source") {
                File source = new File(it.attributes().get("name"))
                it.children().each {
                    if (it.name() == "dependsOn") {
                        File dependsOn = new File(it.attributes().get("name"))

                        def dependency = dependencies.find {
                            it.self == dependsOn
                        }

                        if (dependency) {
                            dependency.sources.add(source)
                        } else {
                            List sourceList = [source]
                            dependencies.add(new Dependency(sourceList, dependsOn))
                        }
                    }
                }
            }
        }

        return dependencies
    }

    static Map<File, List<File>> parseAsMap(String text) {
        def xml = new XmlSlurper().parseText(text)
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        Map<File, List<File>> results = [:]
        xml.children().each {
            if (it.name() == "source") {
                File source = new File(it.attributes().get("name"))
                List<File> dependencies = []
                it.children().each {
                    if (it.name() == "dependsOn") {
                        File dependsOn = new File(it.attributes().get("name"))
                        dependencies.add(dependsOn)
                    }
                }
                results.put(source, dependencies)
            }
        }
        return results
    }

    static class Dependency {
        List<File> sources = []

        File self

        Dependency(List sources, File self) {
            this.sources = sources
            this.self = self
        }

        @Override
        String toString() {
            def s = "Sources with dependency $self : \n"
            sources.each {
                s = s + "$it\n"
            }
            return s
        }
    }

}
