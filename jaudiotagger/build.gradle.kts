plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

group = "com.moriafly.jaudiotagger"
version = "4.0.0-dev01"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
}
