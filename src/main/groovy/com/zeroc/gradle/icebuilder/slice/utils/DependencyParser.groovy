package com.zeroc.gradle.icebuilder.slice.utils

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
//    static Set<Dependency> parseSliceDependencyXML(String text) {
////        def xml = new XmlSlurper().parseText(text)
////        if (xml.name() != "dependencies") {
////            throw new GradleException("malformed XML")
////        }
////
////        Set<Dependency> dependencies = []
////        xml.children().each {
////            if (it.name() == "source") {
////                File source = new File(it.attributes().get("name"))
////                it.children().each {
////                    if (it.name() == "dependsOn") {
////                        File dependsOn = new File(it.attributes().get("name"))
////
////                        def dependency = dependencies.find {
////                            it.self == dependsOn
////                        }
////
////                        if (dependency) {
////                            dependency.sources.add(source)
////                        } else {
////                            List sourceList = [source]
////                            dependencies.add(new Dependency(sourceList, dependsOn))
////                        }
////                    }
////                }
////            }
////        }
////
////        return dependencies
////    }

    static DependencyMap parseAsMap(String text) {
        def xml = new XmlSlurper().parseText(text)
        if (xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        DependencyMap results = new DependencyMap()
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

}
