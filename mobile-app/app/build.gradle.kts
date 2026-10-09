plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.z23u184.studymate.app"

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.z23u184.studymate.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000/\"")
        buildConfigField("Boolean", "USE_TEST_UI", "false")
    }

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildTypes {
        debug {
            // An opt-in E2E APK is installed side-by-side with the user's normal app.
            // The instrumented test uses the actual StudyMateApplication and Koin graph.
            val lab2E2e = providers.gradleProperty("lab2E2e").orNull == "true"
            // Export the headless command bridge only in the opt-in disposable APK.
            manifestPlaceholders["lab2E2eBridgeEnabled"] = lab2E2e.toString()
            if (lab2E2e) {
                applicationIdSuffix = ".lab2e2e"
            }
            val testBaseUrl = providers.gradleProperty("lab2E2eBaseUrl").orNull
                ?: "http://10.0.2.2:8000/"
            buildConfigField("String", "SERVER_BASE_URL", "\"${testBaseUrl}\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    testImplementation(libs.junit)
    // Only the debug-only IPC bridge invokes the real app graph; the E2E test
    // calls that public contract and uses an independent Retrofit HTTP client.
    implementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.retrofit)
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
