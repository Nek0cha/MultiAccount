plugins {
    id("java")
}

subprojects {
    apply(plugin = "java")

    group = "net.ny4n.multiaccount"
    version = "1.0.0"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
