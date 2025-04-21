plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.example.vnews"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.vnews"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:none")
    }
    
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.9.0")
    
    // Firebase
    implementation("com.google.firebase:firebase-firestore:24.10.3")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.3.0")
    
    // Glide for image loading (with enhanced caching and performance)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    
    // CircleImageView for circular profile images
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Jsoup for HTML parsing
    implementation("org.jsoup:jsoup:1.16.2")
    
    // Performance monitoring
    implementation("com.google.firebase:firebase-perf:20.5.2")
    
    // DataBinding
    implementation("androidx.databinding:databinding-runtime:7.4.2")
    implementation("androidx.databinding:databinding-common:7.4.2")
    implementation("androidx.databinding:databinding-adapters:7.4.2")
    
    // Desugaring for older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}