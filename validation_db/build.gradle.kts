import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.preprint.server"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven("http://maven.icm.edu.pl/artifactory/repo/")
    maven("https://dl.bintray.com/rookies/maven" )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.beust:klaxon:5.0.1")
    implementation("com.jsoniter:jsoniter:0.9.19")
    implementation("org.apache.logging.log4j:log4j-api:2.13.1")
    implementation("org.apache.logging.log4j:log4j-core:2.13.1")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.tukaani:xz:1.5")
    compile("org.apache.commons:commons-compress:1.20")
    compile("org.apache.commons:commons-lang3:3.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.rocksdb:rocksdbjni:6.8.1")
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("validation")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "ValidationMainKt"))
        }
        isZip64 = true
    }
}

tasks {
    build {
        dependsOn("shadowJar")
    }
}