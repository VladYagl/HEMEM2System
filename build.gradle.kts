import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
}
group = "me.vlad"
version = "1.0-SNAPSHOT"
val vkSdkVersion = "0.0.8"

repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test-junit"))

//    implementation("dev.kord:kord-core:0.8.0-M5")
    implementation("com.jessecorbett:diskord-bot:2.0.2")


    implementation("com.beust:klaxon:5.5")

    // core module is required
    implementation("com.petersamokhin.vksdk:core:$vkSdkVersion")

    // One of the HTTP clients is also required.
    // You can use pre-defined OkHttp-based client, but only for JVM.
    implementation("com.petersamokhin.vksdk:http-client-jvm-okhttp:$vkSdkVersion")


    // If your project is not JVM-based, or you simply want to use ktor.
    implementation("com.petersamokhin.vksdk:http-client-common-ktor:$vkSdkVersion")

    // In this case, `ktor-client` is required. You can use any.
    implementation("io.ktor:ktor-client-cio:1.6.3")
    implementation("io.ktor:ktor-client-logging-jvm:1.6.3")
    implementation("io.ktor:ktor-client-core:1.6.3")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}