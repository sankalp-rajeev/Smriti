plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val realGemmaDevModeBuildGate = providers
    .gradleProperty("smriti.realGemmaDevMode")
    .map { it.toBoolean() }
    .getOrElse(false)

val realGemmaSubmissionModeBuildGate = providers
    .gradleProperty("smriti.realGemmaSubmissionMode")
    .map { it.toBoolean() }
    .getOrElse(false)

val finalRecordingUiBuildGate = providers
    .gradleProperty("smriti.finalRecordingUi")
    .map { it.toBoolean() }
    .getOrElse(false)

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.smriti.clinicalscribe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smriti.clinicalscribe"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "REAL_GEMMA_DEV_BUILD_GATE", realGemmaDevModeBuildGate.toString())
        buildConfigField("boolean", "REAL_GEMMA_SUBMISSION_MODE", realGemmaSubmissionModeBuildGate.toString())
        buildConfigField("boolean", "FINAL_RECORDING_UI", finalRecordingUiBuildGate.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.2")

    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
