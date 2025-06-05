plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.android24hstream"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.android24hstream"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/*.md",
                "META-INF/*.kotlin_module"
            )
        }
    }


}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.google.api.services.youtube)
    implementation(libs.google.api.client.android)
    implementation(libs.play.services.auth.v2070)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)


    }