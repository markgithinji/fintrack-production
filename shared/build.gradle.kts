import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.biometric)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto.ktx)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.tink.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.paging.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.navigation.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.material.icons.extended)
            implementation(libs.cmpcharts)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.paging.common)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.bignum)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.fintrack.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config = files("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true

    // Configure for KMP source sets
    source = files(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin",
        "src/commonTest/kotlin",
    )
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0")
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("0.50.0")
    }
}

// Configure tasks for KMP
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/build/**")
    jvmTarget = "17"

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
    }
}

// Create convenience tasks
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Run all static analysis tools"
    dependsOn("detekt", "spotlessCheck")
}

tasks.register("formatCode") {
    group = "formatting"
    description = "Format all code"
    dependsOn("spotlessApply")
}