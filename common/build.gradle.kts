plugins {
    id("java-library")
    //alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
dependencies {
    // Add Kotlin serialization dependency
    implementation(libs.kotlinx.serialization.json)
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
