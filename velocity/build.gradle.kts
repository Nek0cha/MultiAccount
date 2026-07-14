plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.5.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.1")
    // Velocity本体が同梱するバージョンに合わせる（実行時はプロキシ側のjarが提供するためshadeしない）
    compileOnly("org.yaml:snakeyaml:2.5")
    compileOnly("com.google.code.gson:gson:2.13.2")
}

tasks {
    shadowJar {
        archiveBaseName.set("multiaccount-velocity")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
