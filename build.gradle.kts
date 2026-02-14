@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("jvm") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("io.ktor.plugin") version "2.3.8" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
