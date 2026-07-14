plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveBaseName.set("multiaccount-paper")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
