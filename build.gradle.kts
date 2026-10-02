plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.1.0" // Paper server for testing/hotloading JVM
}

group = "me.onlyjordon"
version = "2.0.0"
description = "Hide all capes via Paper's native SkinParts API — no external dependencies"

var mainMinecraftVersion = "1.21.11"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mainMinecraftVersion-R0.1-SNAPSHOT")
    // NO Nicknamer API dependency — uses Paper's native SkinParts API
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
    }
    assemble {
        dependsOn(shadowJar)
    }
    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        downloadPlugins {

        }
        minecraftVersion("$mainMinecraftVersion")
    }
    runPaper.folia.registerTask()
}
