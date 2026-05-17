plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.nexora"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.nexora.backend.ApplicationKt")
}

dependencies {
    val ktorVersion = "2.3.12"

    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("ch.qos.logback:logback-classic:1.5.8")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
