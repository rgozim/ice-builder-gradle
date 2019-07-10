package org.openmicroscopy.plugins

import com.zeroc.gradle.icebuilder.slice.SliceExtension
import com.zeroc.gradle.icebuilder.slice.SlicePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.GenerateMavenPom

class SliceDependencyStrategyPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.withType(SlicePlugin) {
            configureForJava(project, project.extensions.getByType(SliceExtension))
        }
    }

    void configureForJava(Project project, SliceExtension slice) {
        project.plugins.withType(JavaPlugin) {
            // Set a resolution strategy for zeroc dependencies
            project.configurations.configureEach { Configuration config ->
                config.resolutionStrategy.eachDependency { DependencyResolveDetails details ->
                    if (details.requested.group == "com.zeroc") {
                        details.useVersion slice.iceVersion
                    }
                }
            }

            // Prevents zeroc dependencies having no version in pom
            project.plugins.withType(MavenPublishPlugin) {
                project.afterEvaluate {
                    project.tasks.withType(GenerateMavenPom).all { GenerateMavenPom task ->
                        task.pom.withXml { XmlProvider xml ->
                            NodeList dependencies = xml.asNode().get("dependencies") as NodeList

                            dependencies.dependency.each { Node dependency ->
                                if (dependency.groupId.text() == "com.zeroc") {
                                    def artifactId = dependency.artifactId as NodeList
                                    insertNode(artifactId, "version", slice.iceVersion)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private insertNode(NodeList node, String name, Object value) {
        Node self = ((Node) node.get(0))
        List tail = getTail(self)
        self.parent().appendNode(name, null, value)
        self.parent().children().addAll(tail)
    }

    private List getTail(Node node) {
        List list = node.parent().children()
        int afterIndex = list.indexOf(node)
        List tail = new ArrayList(list.subList(afterIndex + 1, list.size()))
        list.subList(afterIndex + 1, list.size()).clear()
        return tail
    }

}
