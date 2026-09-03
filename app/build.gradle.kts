plugins { id("com.android.application") }

android {
    namespace = "com.omega7.messenger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omega7.messenger"
        minSdk = 28
        targetSdk = 36
        versionCode = 12
        versionName = "0.10.1-e2ee"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources { excludes += setOf("libsignal_jni*.dylib", "signal_jni*.dll", "libsignal_jni_testing.so") }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.biometric:biometric:1.4.0-alpha02")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("org.signal:libsignal-client:0.100.0")
    implementation("org.signal:libsignal-android:0.100.0")
    testImplementation("junit:junit:4.13.2")
}
