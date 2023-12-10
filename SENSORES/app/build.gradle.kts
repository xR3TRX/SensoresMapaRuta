plugins {
    id("com.android.application")
    //Google services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.xr3trx.sensores"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xr3trx.sensores"
        minSdk = 24
        targetSdk = 33
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
}

dependencies {

    // MAPA:
    implementation("org.osmdroid:osmdroid-android:6.1.17")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))

    //Mas Firebase
    implementation("com.google.firebase:firebase-analytics")

    //Database
    implementation("com.google.firebase:firebase-database")


    //Paul Beltrand de INACAP en youtube
    implementation("androidx.legacy:legacy-support-v4:1.0.0") //Permite mantener conexion abierta
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0") //Permite agregar los permisos


    //MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1"){
        exclude("com.android.support")
        exclude(module = "appcompat-v7")
        exclude(module = "support-v4")

    }

}