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

    static class DependencyMap implements Map<File, List<File>> {

        private Map<File, List<File>> sourceToDependencies = [:]

        private Map<File, List<File>> dependenciesToSource = [:]

        Map<File, List<File>> getDependenciesToSource() {
            return dependenciesToSource
        }

        @Override
        int size() {
            return sourceToDependencies.size()
        }

        @Override
        boolean isEmpty() {
            return false
        }

        @Override
        boolean containsKey(Object key) {
            return sourceToDependencies.containsKey(key)
        }

        @Override
        boolean containsValue(Object value) {
            return sourceToDependencies.containsValue(value)
        }

        @Override
        List<File> get(Object key) {
            return sourceToDependencies.get(key)
        }

        @Override
        List<File> put(File source, List<File> dependencies) {
            dependencies.each { File dep ->
                if (dependenciesToSource.containsKey(dep)) {
                    def sources = dependenciesToSource.get(dep)
                    if (!sources.contains(source)) {
                        sources.push(source)
                    }
                } else {
                    dependenciesToSource.put(dep, [source])
                }
            }
            return sourceToDependencies.put(source, dependencies)
        }

        @Override
        List<File> remove(Object source) {
            dependenciesToSource.each { File dependency, List<File> sources ->
                if (sources.remove(source)) {
                    if (sources.isEmpty()) {
                        dependenciesToSource.remove(dependency)
                    }
                }
            }
            return sourceToDependencies.remove(source)
        }

        @Override
        void putAll(Map<? extends File, ? extends List<File>> m) {

        }

        @Override
        void clear() {

        }

        @Override
        Set<File> keySet() {
            return null
        }

        @Override
        Collection<List<File>> values() {
            return null
        }

        @Override
        Set<Entry<File, List<File>>> entrySet() {
            return null
        }
    }

}
