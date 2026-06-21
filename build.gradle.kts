

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JNA for memory reading
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    // JavaOSC for sending OSC messages
    implementation("com.illposed.osc:javaosc-core:0.9")

    // SLF4J implementation (required by JavaOSC)
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // Kotlinx Serialization for JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8"
    )
}

// Fat JAR configuration
tasks.shadowJar {
    archiveBaseName.set("THOSC_BOX")
    archiveClassifier.set("all")
    archiveVersion.set(version.toString())
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

// EXE packaging via jpackage (requires JDK 14+)
tasks.register<Exec>("jpackage") {
    dependsOn(tasks.shadowJar)
    val shadowJarFile = tasks.shadowJar.get().archiveFile.get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile

    doFirst {
        if (outputDir.exists()) outputDir.deleteRecursively()
    }

    commandLine(
        "jpackage",
        "--input", shadowJarFile.parentFile.absolutePath,
        "--main-jar", shadowJarFile.name,
        "--main-class", "MainKt",
        "--name", "THOSC_BOX",
        "--type", "app-image",
        "--dest", outputDir.absolutePath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--icon", file("windows_default.ico").absolutePath
    )
}