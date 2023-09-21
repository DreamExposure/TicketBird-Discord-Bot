
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Kotlin
    kotlin("jvm") version "1.9.10"

    // Spring
    kotlin("plugin.spring") version "1.9.10"
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"

    // Tooling
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("com.google.cloud.tools.jib") version "3.3.2"
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.14.2")
    }
}

val ticketBirdVersion = "2.1.0"
val gradleWrapperVersion = "7.6"
val javaVersion = "17"
val d4jVersion = "3.2.6"
val d4jStoresVersion = "3.2.2"
val logbackContribVersion = "0.1.5"
val mysqlR2dbcVersion = "0.8.2.RELEASE"
val mySqlConnectorVersion = "8.0.33"
val discordWebhooksVersion = "0.8.4"
val springMockkVersion = "4.0.2"
val commonsIOVersion = "2.13.0"

group = "org.dreamexposure"
version = ticketBirdVersion

val kotlinSrcDir: File = buildDir.resolve("src/main/kotlin")

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = sourceCompatibility
}

kotlin {
    sourceSets {
        all {
            kotlin.srcDir(kotlinSrcDir)
        }
    }
}

repositories {
    mavenCentral()

    maven("https://repo.maven.apache.org/maven2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}


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

    // Logging
    implementation("ch.qos.logback.contrib:logback-json-classic:$logbackContribVersion")
    implementation("ch.qos.logback.contrib:logback-jackson:$logbackContribVersion")

    // Web
    implementation("com.squareup.okhttp3:okhttp")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("dev.miku:r2dbc-mysql:$mysqlR2dbcVersion")
    implementation("mysql:mysql-connector-java:$mySqlConnectorVersion")

    // IO
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("commons-io:commons-io:$commonsIOVersion")

    // Discord
    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jStoresVersion")
    implementation("club.minnced:discord-webhooks:$discordWebhooksVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
}

jib {
    to {
        image = "rg.nl-ams.scw.cloud/dreamexposure/ticketbird"
        tags = mutableSetOf("latest", ticketBirdVersion)
    }

    from.image = "eclipse-temurin:17-jre-alpine"
}

gitProperties {
    extProperty = "gitPropertiesExt"

    val versionName = if (System.getenv("GITHUB_RUN_NUMBER") != null) {
        "$version.b${System.getenv("GITHUB_RUN_NUMBER")}"
    } else {
        "$version.d${System.currentTimeMillis().div(1000)}" //Seconds since epoch
    }

    customProperty("ticketbird.version", versionName)
    customProperty("ticketbird.version.d4j", d4jVersion)
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

    wrapper {
        distributionType = ALL
        gradleVersion = gradleWrapperVersion
    }
}
