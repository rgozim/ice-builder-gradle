package com.zeroc.gradle.icebuilder.slice

class Dependency {

    Set<File> sources = []

    File self

    Dependency(Set<File> sources, File self) {
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