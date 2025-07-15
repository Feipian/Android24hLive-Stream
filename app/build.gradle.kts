import java.util.Properties

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

    val localProperties = Properties()
    val localPropertiesFile = File(rootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            localProperties.load(it)
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )

                buildConfigField("String", "API_KEY", localProperties.getProperty("API_KEY"))
            }

            debug {

                buildConfigField("String", "API_KEY", localProperties.getProperty("API_KEY"))
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        buildFeatures{
            buildConfig = true
            resValues = true
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

        // https://mvnrepository.com/artifact/com.google.oauth-client/google-oauth-client-jetty
        implementation(libs.google.oauth.client.jetty)


        // compile fileTree(dir: 'libs', include: ['*.jar'])
        implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

        // compile 'com.android.support:appcompat-v7:25.0.1'
        // Note: com.android.support is deprecated. You should use androidx libraries
        // if possible. This is the direct conversion.
        implementation(libs.appcompat.v7)

        // compile 'com.google.android.gms:play-services-auth:21.3.0'
        implementation(libs.play.services.auth)

        // compile 'pub.devrel:easypermissions:0.3.0'
        implementation(libs.easypermissions)

        // compile('com.google.api-client:google-api-client-android:1.22.0') {
        // exclude group: 'org.apache.httpcomponents'
        // }

        implementation("com.google.api-client:google-api-client-android:1.22.0") {
            exclude(group = "org.apache.httpcomponents")
        }

        // compile('com.google.apis:google-api-services-youtube:v3-rev183-1.22.0') {
        // exclude group: 'org.apache.httpcomponents'
        // }
        implementation("com.google.apis:google-api-services-youtube:v3-rev183-1.22.0") {
            exclude(group = "org.apache.httpcomponents")

        }


        implementation("com.google.api-client:google-api-client-android:1.32.1") { // Check Maven Central for the absolute latest
            exclude(group = "org.apache.httpcomponents")
        }

        implementation("com.github.pedroSG94.RootEncoder:library:2.6.1")
        implementation("com.github.pedroSG94.RootEncoder:extra-sources:2.6.1")


    }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(files("C:\\Users\\HongMingWang\\Desktop\\Code\\Android24hLive-Stream\\libs\\ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-java:0.2.1")
    //noinspection GradlePath


}

}


