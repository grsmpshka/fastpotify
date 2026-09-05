import org.gradle.internal.os.OperatingSystem

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "rocks.fastpotify.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "rocks.fastpotify.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "ENABLE_DEMO_MODE", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_DEMO_MODE", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("rustJniLibs"))

    packaging {
        jniLibs.useLegacyPackaging = false
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

val repositoryRoot = rootProject.layout.projectDirectory.dir("..").asFile
val rustOutput = layout.buildDirectory.dir("rustJniLibs")
val cargoExecutable = if (OperatingSystem.current().isWindows) "cargo.exe" else "cargo"

fun registerRustBuild(name: String, release: Boolean) = tasks.register<Exec>(name) {
    group = "build"
    description = "Builds the Fastpotify Rust core for Android ABIs."
    workingDir(repositoryRoot)
    outputs.dir(rustOutput)
    val args = mutableListOf(
        "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", rustOutput.get().asFile.absolutePath,
        "build",
        "--locked",
        "--package", "fastpotify-android",
    )
    if (release) args += "--release"
    commandLine(cargoExecutable, *args.toTypedArray())
}

val buildRustDebug = registerRustBuild("buildRustDebug", release = false)
val buildRustRelease = registerRustBuild("buildRustRelease", release = true)

tasks.matching { it.name == "mergeDebugJniLibFolders" }.configureEach {
    dependsOn(buildRustDebug)
}
tasks.matching { it.name == "mergeReleaseJniLibFolders" }.configureEach {
    dependsOn(buildRustRelease)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
