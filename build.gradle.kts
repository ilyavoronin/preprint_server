plugins {
    java
    kotlin("jvm") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.2.0"
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
    implementation("xml-apis:xml-apis:2.0.2")
    compile("org.grobid:grobid-core:0.5.6")
    compile("org.grobid:grobid-trainer:0.5.6")
    implementation("org.allenai:science-parse_2.11:2.0.3")
    implementation("org.apache.logging.log4j:log4j-api:2.13.1")
    implementation("org.apache.logging.log4j:log4j-core:2.13.1")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.neo4j.driver:neo4j-java-driver:4.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("io.mockk:mockk:1.9.3")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    shadowJar {
        isZip64 = true
    }
}