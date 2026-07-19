plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.github.rain1208"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.61.0"
val flywayVersion = "10.20.1"
val ktorVersion = "3.0.3"

dependencies {
    // Discord
    implementation("net.dv8tion:JDA:5.6.1") {
        exclude(module = "opus-java")
    }

    // DB
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // DI
    implementation("io.insert-koin:koin-core:4.0.2")

    // 設定
    implementation("com.typesafe:config:1.4.3")

    // ログ
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Web コンソール(Ktor)
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-freemarker:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.github.rain1208.sakenomi.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    // Flyway (flyway-core / flyway-database-postgresql) はどちらも
    // META-INF/services/org.flywaydb.core.extensibility.Plugin を持っており、
    // マージしないと片方が上書きされて SQL マイグレーションが認識されなくなる
    mergeServiceFiles()
}

kotlin {
    jvmToolchain(21)
}
