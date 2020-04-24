package com.preprint.server.utils

import java.io.File

fun File.loadTextFromResources() : String {
    return this.javaClass.getResource(this.path).readText()
}