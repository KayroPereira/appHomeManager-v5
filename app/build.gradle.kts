import java.util.Properties

plugins {
    id("com.android.application")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

// Valores locais (fora do git): LOGIN_TESTE_EMAIL / LOGIN_TESTE_SENHA
val localProperties = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) {
        arquivo.inputStream().use { load(it) }
    }
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

        // Credenciais do projeto Cloud da Tuya (local.properties, fora do git).
        // Atenção: o secret embutido no app pode ser extraído do APK; use só em app pessoal.
        buildConfigField("String", "TUYA_ACCESS_ID", "\"${localProperties.getProperty("TUYA_ACCESS_ID", "")}\"")
        buildConfigField("String", "TUYA_ACCESS_SECRET", "\"${localProperties.getProperty("TUYA_ACCESS_SECRET", "")}\"")
        buildConfigField("String", "TUYA_HOME_ID", "\"${localProperties.getProperty("TUYA_HOME_ID", "")}\"")
        buildConfigField("String", "TUYA_BASE_URL", "\"${localProperties.getProperty("TUYA_BASE_URL", "https://openapi.tuyaus.com/")}\"")
    }

    buildTypes {
        debug {
            // Pré-preenchimento do login para agilizar os testes; nunca vai para o release.
            buildConfigField("String", "LOGIN_TESTE_EMAIL", "\"${localProperties.getProperty("LOGIN_TESTE_EMAIL", "")}\"")
            buildConfigField("String", "LOGIN_TESTE_SENHA", "\"${localProperties.getProperty("LOGIN_TESTE_SENHA", "")}\"")
        }
        release {
            buildConfigField("String", "LOGIN_TESTE_EMAIL", "\"\"")
            buildConfigField("String", "LOGIN_TESTE_SENHA", "\"\"")
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
    testOptions {
        // android.util.Log e afins retornam valores padrão nos testes JVM.
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // Import the Firebase BoM (os artefatos Firebase abaixo não precisam de versão)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")

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
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    //JSON
    implementation("com.google.code.gson:gson:2.10.1")

    //Geolocalização e clima
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}
