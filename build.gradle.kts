plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.omnivision.astrologiahoraria"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.omnivision.astrologiahoraria"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Localização
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Material Components (necessário para o tema XML)
    implementation("com.google.android.material:material:1.11.0")
}
