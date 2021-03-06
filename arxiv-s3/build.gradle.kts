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
    implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.769"))
    implementation("com.amazonaws:aws-java-sdk-s3")
    implementation("org.apache.logging.log4j:log4j-api:2.13.1")
    implementation("org.apache.logging.log4j:log4j-core:2.13.1")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    compile("org.apache.commons:commons-compress:1.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
    compile(project(":core"))
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
        archiveBaseName.set("arxiv-s3")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "ArxivS3MainKt"))
        }
        isZip64 = true
    }
}

tasks {
    build {
        dependsOn("shadowJar")
    }
}