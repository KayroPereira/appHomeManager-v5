plugins {
    id("com.android.application")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.home.apphomemanager_v5"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.home.apphomemanager_v5"
        minSdk = 30
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    viewBinding{
        enable = true;
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation ("com.google.code.gson:gson:2.8.9")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.android.material:material:1.11.0")

//    implementation("com.google.dagger:dagger:2.51.1")
//    annotationProcessor("com.google.dagger:dagger-compiler:2.51.1")

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    // Import the BoM for the Firebase platform
//    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database:20.3.0")



    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //Navigation
    val navVersion = "2.7.6"
    implementation("androidx.navigation:navigation-fragment:$navVersion")
    implementation("androidx.navigation:navigation-ui:$navVersion")

    //Lombok
    compileOnly ("org.projectlombok:lombok:1.18.34")
//    implementation ("org.projectlombok:lombok:1.18.28")
    annotationProcessor ("org.projectlombok:lombok:1.18.34")

//    implementation 'org.projectlombok:lombok:1.18.24'
//    annotationProcessor 'org.projectlombok:lombok:1.18.24'


    //Jackson
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.0.1")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation ("com.fasterxml.jackson.core:jackson-core:2.15.0")
    implementation ("com.fasterxml.jackson.core:jackson-annotations:2.15.0")


    //Geolocalização
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")

}