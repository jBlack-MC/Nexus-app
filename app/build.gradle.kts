import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

val nexusApiUrl = providers.gradleProperty("NEXUS_API_URL")
    .orElse(providers.environmentVariable("NEXUS_API_URL"))
    .getOrElse("http://10.0.2.2:5263/api/")
require(nexusApiUrl.matches(Regex("https?://[A-Za-z0-9.:-]+(/[A-Za-z0-9._~/-]*)?/"))) {
    "NEXUS_API_URL must be an HTTP(S) base URL ending with /"
}

// Google single sign-on: the *Web* OAuth 2.0 client ID from Google Cloud Console (the same value the
// API uses as GOOGLE_CLIENT_ID). When empty, the "Continue with Google" button is hidden.
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
    .orElse(providers.environmentVariable("GOOGLE_WEB_CLIENT_ID"))
    .getOrElse("")
require(googleWebClientId.isEmpty() || googleWebClientId.matches(Regex("""[A-Za-z0-9.-]+\.apps\.googleusercontent\.com"""))) {
    "GOOGLE_WEB_CLIENT_ID must look like <id>.apps.googleusercontent.com"
}

// Release signing (Play Store upload key). Values come from keystore.properties (git-ignored) or
// environment variables in CI; nothing secret is stored in the repository.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)
val releaseStoreFile = signingValue("storeFile", "NEXUS_KEYSTORE_PATH")

android {
    namespace = "com.example.nexus"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.nexus"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"$nexusApiUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "NEXUS_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "NEXUS_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "NEXUS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$nexusApiUrl\"")
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.google.material)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Google single sign-on through Android Credential Manager
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Notifications: scheduled due-date reminders (WorkManager) and push (Firebase Cloud Messaging)
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("generated") }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    source.setFrom("src/main/java")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
}

// Never produce a release that silently points at an emulator or an invented host.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    // Capture a plain String: referencing the script-level val inside doFirst would capture the
    // build script object, which the configuration cache cannot serialise.
    val releaseApiUrl: String = nexusApiUrl
    doFirst {
        require(releaseApiUrl.startsWith("https://")) {
            "Release builds require -PNEXUS_API_URL=https://your-host/api/"
        }
    }
}

// Firebase is configured by dropping the project's google-services.json into app/. Without it the
// app still builds; push registration is skipped at runtime and local reminders keep working.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
