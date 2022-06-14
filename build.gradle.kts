import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    java

    kotlin("plugin.spring") version "1.7.0"

    id("com.google.cloud.tools.jib") version "3.2.1"
    id ("org.springframework.boot") version "2.6.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("com.gorylenko.gradle-git-properties") version "2.4.1"
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.11.0")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}
//versions
val d4jVersion = "3.2.1"
val d4jStoresVersion = "3.2.1"

val nettyForcedVersion = "4.1.56.Final"
val reactorCoreVersion = "3.4.14"
val reactorNettyVersion = "1.0.15"

val kotlinSrcDir: File = buildDir.resolve("src/main/kotlin")

dependencies {
    // Tools
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("org.dreamexposure:NovaUtils:1.0.0-SNAPSHOT")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // Web
    implementation("org.thymeleaf:thymeleaf:3.0.14.RELEASE")
    implementation("org.thymeleaf:thymeleaf-spring5:3.0.14.RELEASE")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("dev.miku:r2dbc-mysql") {
        exclude("io.netty", "*")
        exclude("io.projectreactor", "*")
    }
    implementation("mysql:mysql-connector-java")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.json:json:20220320")

    // Discord
    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jStoresVersion") {
        exclude("io.netty", "*")
    }
    implementation("com.github.DiscordBotList:Java-Wrapper:v1.0")
    implementation("club.minnced:discord-webhooks:0.7.4")

    // Forced version nonsense
    implementation("io.netty:netty-all:$nettyForcedVersion")
    implementation("io.projectreactor:reactor-core:$reactorCoreVersion")
    implementation("io.projectreactor.netty:reactor-netty:$reactorNettyVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.ninja-squad:springmockk:3.1.1")
}

group = "org.dreamexposure"
version = "2.0.0-SNAPSHOT"
description = "TicketBird"
java.sourceCompatibility = JavaVersion.VERSION_17

jib {
    var imageVersion = version.toString()
    if (imageVersion.contains("SNAPSHOT")) imageVersion = "latest"

    to.image = "rg.nl-ams.scw.cloud/dreamexposure/ticketbird:$imageVersion"
    from.image = "eclipse-temurin:17-jre-alpine"
    container.creationTime = "USE_CURRENT_TIMESTAMP"
}

gitProperties {
    extProperty = "gitPropertiesExt"

    val versionName = if (System.getenv("BUILD_NUMBER") != null) {
        "$version.${System.getenv("BUILD_NUMBER")}"
    } else {
        "$version.d${System.currentTimeMillis().div(1000)}" //Seconds since epoch
    }

    customProperty("ticketbird.version", versionName)
    customProperty("ticketbird.version.d4j", d4jVersion)
    customProperty("ticketbird.url.base", "https://ticketbird.dreamexposure.org")
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
            jvmTarget = java.targetCompatibility.majorVersion
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    bootJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

}
