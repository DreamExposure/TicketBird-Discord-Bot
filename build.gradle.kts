
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    java

    kotlin("plugin.spring") version "1.5.0"

    id("com.google.cloud.tools.jib") version("3.0.0")
    id ("org.springframework.boot") version ("2.5.0")

    id("com.gorylenko.gradle-git-properties") version "2.2.3"
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.7.2")
    }
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
val nettyForcedVersion = "4.1.56.Final"
val reactorCoreVersion = "3.4.2"
val reactorNettyVersion = "1.0.3"
val r2dbcMysqlVersion = "0.8.1.RELEASE"
val r2dbcPoolVersion = "0.8.3.RELEASE"

val kotlinSrcDir: File = buildDir.resolve("src/main/kotlin")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0")

    implementation("org.dreamexposure:NovaUtils:1.0.0-SNAPSHOT")

    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jVersion") {
        exclude("io.netty", "*")
    }

    implementation("mysql:mysql-connector-java:8.0.25")
    implementation("org.json:json:20210307")
    implementation("org.jetbrains:annotations:21.0.1")

    implementation("dev.miku:r2dbc-mysql:$r2dbcMysqlVersion") {
        exclude("io.netty", "*")
        exclude("io.projectreactor", "*")
    }
    implementation("io.r2dbc:r2dbc-pool:$r2dbcPoolVersion")

    //Forced version nonsense
    implementation("io.netty:netty-all:$nettyForcedVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.2")
    implementation("io.projectreactor:reactor-core:$reactorCoreVersion")
    implementation("io.projectreactor.netty:reactor-netty:$reactorNettyVersion")

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
version = "1.0.1"
description = "TicketBird"
java.sourceCompatibility = JavaVersion.VERSION_1_8

jib {
    to.image = "rg.nl-ams.scw.cloud/dreamexposure/ticketbird:$version"
}

gitProperties {
    extProperty = "gitPropertiesExt"

    customProperty("ticketbird.version", version)
    customProperty("ticketbird.version.d4j", d4jVersion)
}

kotlin {
    sourceSets {
        all {
            kotlin.srcDir(kotlinSrcDir)
        }
    }
}

tasks {
    generateGitProperties {
        doLast {
            @Suppress("UNCHECKED_CAST")
            val gitProperties = ext[gitProperties.extProperty] as Map<String, String>
            val enumPairs = gitProperties.mapKeys { it.key.replace('.', '_').toUpperCase() }

            val enumBuilder = TypeSpec.enumBuilder("GitProperty")
                    .primaryConstructor(
                            com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                                    .addParameter("value", String::class)
                                    .build()
                    )

            val enums = enumPairs.entries.fold(enumBuilder) { accumulator, (key, value) ->
                accumulator.addEnumConstant(
                        key, TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter("%S", value)
                        .build()
                )
            }

            val enumFile = FileSpec.builder("org.dreamexposure.ticketbird", "GitProperty")
                    .addType(
                            enums // https://github.com/square/kotlinpoet#enums
                                    .addProperty(
                                           PropertySpec.builder("value", String::class)
                                                    .initializer("value")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build()

            enumFile.writeTo(kotlinSrcDir)
        }
    }

    withType<KotlinCompile> {
        dependsOn(generateGitProperties)

        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = targetCompatibility
        }
    }

    bootJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

}
