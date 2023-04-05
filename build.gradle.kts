plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "com.elky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.husnjak:igdb-api-jvm:1.0.10")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}