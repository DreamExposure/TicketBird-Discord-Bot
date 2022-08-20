import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    java

    kotlin("plugin.spring") version "1.7.0"

    id("com.google.cloud.tools.jib") version "3.2.1"
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"

    id("com.gorylenko.gradle-git-properties") version "2.4.1"
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.12.0")
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
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
//versions
val d4jVersion = "3.2.3"
val d4jStoresVersion = "3.2.1"

val kotlinSrcDir: File = buildDir.resolve("src/main/kotlin")

dependencies {
    // Tools
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Web
    implementation("org.thymeleaf:thymeleaf")
    implementation("org.thymeleaf:thymeleaf-spring5")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    implementation("com.squareup.okhttp3:okhttp")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("dev.miku:r2dbc-mysql:0.8.2.RELEASE")
    implementation("mysql:mysql-connector-java")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Discord
    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jStoresVersion")
    implementation("club.minnced:discord-webhooks:0.8.2")

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
version = "2.0.0"
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
                    // https://github.com/square/kotlinpoet#enums
                    enums.addProperty(
                        PropertySpec.builder("value", String::class)
                            .initializer("value")
                            .build()
                    ).build()
                ).build()

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

    wrapper {
        distributionType = ALL
        gradleVersion = "7.5.1"
    }
}
