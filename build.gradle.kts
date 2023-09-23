plugins {
    id("java-library")
    id("maven-publish")
    id("checkstyle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.burningtnt"
version = "0.9.0"
description = "An analysis tool specifically developed for Minecraft: Java Edition"

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val javadocJar = tasks.create<Jar>("javadocJar") {
    group = "build"
    archiveClassifier.set("javadoc")
}

tasks.compileJava {
    listOf(
        "jdk.attach/sun.tools.attach"
    ).forEach { string ->
        this.options.compilerArgs.add("--add-exports")
        this.options.compilerArgs.add("${string}=ALL-UNNAMED")
    }
}

checkstyle {
    sourceSets = mutableSetOf()
}

tasks.getByName("build") {
    dependsOn(tasks.getByName("checkstyleMain") {
        group = "build"
    })

    dependsOn(tasks.getByName("checkstyleTest") {
        group = "build"
    })

    dependsOn(tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        manifest {
            attributes(
                "Main-Class" to "net.burningtnt.jstackmc.Main",
                "Add-Opens" to listOf(
                    "jdk.attach/sun.tools.attach"
                ).joinToString(" ")
            )
        }
    })
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")

    testImplementation(project)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}