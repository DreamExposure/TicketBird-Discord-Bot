import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    java

    kotlin("plugin.spring") version "1.5.0"

    id("com.google.cloud.tools.jib") version("3.0.0")
    id ("org.springframework.boot") version ("2.5.0")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.spring.io/libs-release")
    }

    maven {
        url = uri("https://emily.dreamexposure.org/artifactory/dreamexposure-public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}
//versions
val d4jVersion = "3.1.5"
val springVersion = "2.5.0"
val springSecVersion = "5.5.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0")

    implementation("org.dreamexposure:NovaUtils:1.0.0-SNAPSHOT")
    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jVersion")
    implementation("mysql:mysql-connector-java:8.0.15")
    implementation("org.json:json:20210307")
    implementation("org.jetbrains:annotations:21.0.1")
    implementation("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
    implementation("org.thymeleaf:thymeleaf-spring5:3.0.12.RELEASE")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:2.5.3")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-webflux:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc:$springVersion")
    implementation("org.springframework.session:spring-session-data-redis:$springVersion")
    implementation("org.springframework.security:spring-security-core:$springSecVersion")
    implementation("org.springframework.security:spring-security-web:$springSecVersion")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.github.DiscordBotList:Java-Wrapper:v1.0")
    implementation("club.minnced:discord-webhooks:0.5.7")
    implementation("org.flywaydb:flyway-core:7.9.2")
}

group = "org.dreamexposure"
version = "1.0.1-SNAPSHOT"
description = "TicketBird"
java.sourceCompatibility = JavaVersion.VERSION_1_8

jib {
    to.image = "rg.nl-ams.scw.cloud/dreamexposure/ticketbird:$version"
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = targetCompatibility
        }
    }

    bootJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
