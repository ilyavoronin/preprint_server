plugins {
    java
    kotlin("jvm") version "1.3.70"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("http://maven.icm.edu.pl/artifactory/repo/")
    maven("https://dl.bintray.com/rookies/maven" )
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
    implementation("pl.edu.icm.cermine:cermine-impl:1.12")
    implementation("org.apache.pdfbox:pdfbox:2.0.19")
    implementation("com.github.kittinunf.fuel:fuel:2.2.1")
    compile("org.grobid:grobid-core:0.5.6")
    implementation("org.allenai:science-parse_2.11:2.0.3")
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
}