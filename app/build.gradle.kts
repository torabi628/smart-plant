import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun env(vararg names: String): String? = names.firstNotNullOfOrNull { System.getenv(it)?.trim()?.takeIf { v -> v.isNotBlank() } }

// ───── هوش مصنوعی (از GitHub Secrets؛ همان نام‌های «نامه‌نگار هوشمند» هم پذیرفته می‌شود) ─────
// ۱) هوش مصنوعی قوی داخلی (آوال‌ای) — SP_AVAL_KEY یا SL_AVAL_KEY
val avalUrl: String = env("SP_AVAL_URL", "SL_AVAL_URL") ?: "https://api.avalai.ir/v1"
val avalKey: String = env("SP_AVAL_KEY", "SL_AVAL_KEY") ?: (project.findProperty("AVAL_KEY") as String?) ?: ""
val avalModel: String = env("SP_AVAL_MODEL", "SL_AVAL_MODEL") ?: "gpt-4.1"
// ۲) هوش مصنوعی قوی خارجی (Pollinations) — SP_AI_KEY یا SL_AI_KEY
val aiDirectUrl: String = env("SP_AI_URL", "SL_AI_URL") ?: "https://gen.pollinations.ai/v1"
val aiDirectKey: String = env("SP_AI_KEY", "SL_AI_KEY") ?: (project.findProperty("AI_DIRECT_KEY") as String?) ?: ""
val aiDirectModel: String = env("SP_AI_MODEL", "SL_AI_MODEL") ?: "openai-large"

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun sign(key: String, env: String): String? = keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "ir.aminim.smartplant"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.aminim.smartplant"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        buildConfigField("String", "AVAL_URL", "\"$avalUrl\"")
        buildConfigField("String", "AVAL_KEY", "\"$avalKey\"")
        buildConfigField("String", "AVAL_MODEL", "\"$avalModel\"")
        buildConfigField("String", "AI_DIRECT_URL", "\"$aiDirectUrl\"")
        buildConfigField("String", "AI_DIRECT_KEY", "\"$aiDirectKey\"")
        buildConfigField("String", "AI_DIRECT_MODEL", "\"$aiDirectModel\"")
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        val store = sign("storeFile", "SP_STORE_FILE")
        if (store != null && rootProject.file(store).exists()) {
            create("release") {
                storeFile = rootProject.file(store)
                storePassword = sign("storePassword", "SP_STORE_PASSWORD")
                keyAlias = sign("keyAlias", "SP_KEY_ALIAS")
                keyPassword = sign("keyPassword", "SP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs { useLegacyPackaging = true }
    }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // تبدیل گفتار آفلاین (Vosk)
    implementation("com.alphacephei:vosk-android:0.3.47@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
