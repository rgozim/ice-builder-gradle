package com.zeroc.gradle.icebuilder.slice

class Python {
    final name
    def args = ""
    def files
    def srcDir = "src/main/slice"
    def include

    Python(name) {
        this.name = name
    }
}
