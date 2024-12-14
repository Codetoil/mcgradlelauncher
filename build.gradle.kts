plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

group = "io.codetoil"
version = "0.1.0"

dependencies {
    implementation("org.gradle.java")
}

gradlePlugin {
    plugins {
        create("mcgradlelauncher") {
            id = "io.codetoil.mcgradlelauncher"
            implementationClass = "io.codetoil.mcgradlelauncher.MCGradleLauncherPlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}