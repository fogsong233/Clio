import org.jetbrains.kotlin.backend.jvm.jvmPhases

plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "tech.fogsong"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

mirai {
    jvmTarget = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.apache.poi:poi-ooxml:5.2.3")
}
tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dmirai.message.allow.sending.file.message=true", "-Dfile.encoding=UTF-8")
}