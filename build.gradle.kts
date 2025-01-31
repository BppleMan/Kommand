import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

val sharedGroup = "com.kgit2"
val sharedVersion = "2.3.0"

group = sharedGroup
version = sharedVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
}

subprojects {
    group = sharedGroup
    version = sharedVersion
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    val nativeTargets = listOf(
        macosX64() to Platform.MACOS_X64,
        macosArm64() to Platform.MACOS_ARM64,
        linuxX64() to Platform.LINUX_X64,
        linuxArm64() to Platform.LINUX_ARM64,
        mingwX64() to Platform.MINGW_X64,
    )

    nativeTargets.forEach { (nativeTarget, targetPlatform) ->
        nativeTarget.apply {
            compilations.getByName("main") {
                cinterops {
                    create("kommandCore") {
                        defFile(project.file("src/nativeInterop/cinterop/${targetPlatform.archName}.def"))
                        packageName("kommand_core")
                    }
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val sourceSetsNeedOptIn = setOf(
            nativeMain,
            appleMain,
            macosMain,
            macosX64Main,
            macosArm64Main,
            linuxMain,
            linuxX64Main,
            linuxArm64Main,
            mingwMain,
            mingwX64Main,
        )

        sourceSetsNeedOptIn.forEach { sourceSet ->
            sourceSet.languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        commonMain {
            languageSettings {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }

            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.23.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        nativeMain {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}

val testString: String by project

tasks {
    withType(Wrapper::class) {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "8.2"
    }

    withType(Test::class) {
        testLogging {
            showStandardStreams = true
        }
    }

    if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")) {
        val replaceLLD by creating {
            group = "mingw-w64"
            doLast {
                System.getenv("USERPROFILE")?.let { userProfile ->
                    val targetFile = File("$userProfile\\.konan\\dependencies\\lld-12.0.1-windows-x64\\ld.lld.exe")
                    File("$userProfile\\.konan\\dependencies\\llvm-16.0.0-x86_64-windows-essentials-56\\bin\\ld.lld.exe")
                        .copyTo(targetFile, true)
                }
            }
        }

        getByName("compileKotlinMingwX64") {
            dependsOn(replaceLLD)
        }
    }
}

val ossrhUrl: String = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
val ossrhUsername = runCatching {
    val ossrhUsername: String by project
    ossrhUsername
}.getOrNull()

val ossrhPassword = runCatching {
    val ossrhPassword: String by project
    ossrhPassword
}.getOrNull()

if (ossrhUsername != null && ossrhPassword != null) {
    nexusPublishing {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(ossrhUsername)
                password.set(ossrhPassword)
            }
        }
    }

    publishing {
        repositories {
            mavenLocal()
            maven {
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
        }
        publications {
            withType<MavenPublication> {
                val dokkaJar = project.tasks.register("${name}DokkaJar", Jar::class) {
                    group = JavaBasePlugin.DOCUMENTATION_GROUP
                    description = "Assembles Kotlin docs with Dokka into a Javadoc jar"
                    archiveClassifier.set("javadoc")
                    from(tasks.dokkaHtml)

                    // Each archive name should be distinct, to avoid implicit dependency issues.
                    // We use the same format as the sources Jar tasks.
                    // https://youtrack.jetbrains.com/issue/KT-46466
                    archiveBaseName.set("${archiveBaseName.get()}-${name}")
                }
                artifact(dokkaJar)
                pom {
                    name.set("kommand")
                    description.set("A simple process library for Kotlin Multiplatform")
                    url.set("https://github.com/kgit2/kommand")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        connection.set("https://github.com/kgit2/kommand.git")
                        url.set("https://github.com/kgit2/kommand")
                    }
                    developers {
                        developer {
                            id.set("BppleMan")
                            name.set("BppleMan")
                            email.set("bppleman@gmail.com")
                        }
                    }
                }
            }
        }
    }

    signing {
        // will default find the
        // - signing.keyId
        // - signing.password
        // - signing.secretKeyRingFile
        sign(publishing.publications)
    }
}

enum class Platform(
    val archName: String,
) {
    MACOS_X64("x86_64-apple-darwin"),
    MACOS_ARM64("aarch64-apple-darwin"),
    LINUX_X64("x86_64-unknown-linux-gnu"),
    LINUX_ARM64("aarch64-unknown-linux-gnu"),
    MINGW_X64("x86_64-pc-windows-gnu"),
    ;
}

val platforms: List<Platform> = listOf(
    Platform.MACOS_X64,
    Platform.MACOS_ARM64,
    Platform.LINUX_X64,
    Platform.LINUX_ARM64,
    Platform.MINGW_X64,
)
