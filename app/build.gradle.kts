
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.relay)

//    id("kotlin-parcelize")
//    alias(libs.plugins.kotlin.parcelize)

    //alias(libs.plugins.kotlin.android.extensions)
}

val abiCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2
)



android {
    namespace = "com.example.kotlinDownloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kotlinDownloader"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

//    applicationVariants.all { variant ->
//
//        val buildType = variant.buildType.name
//
//        variant.outputs.all { output ->
//            val abi = output.filters.find {
//                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI.name
//            }?.identifier
//
//            var fileName = if (abi != null) {
//                "app-${variant.versionCode}-$abi.apk"
//            }
//
//            else {
//                "app-${variant.versionCode}-universal.apk"
//            }
//
//            if (buildType == "release") {
//                fileName += "-release-${variant.versionName}"
//            } else if (buildType == "debug") {
//                fileName += "-debug-${variant.versionName}"
//            }
//
//            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
//                output.outputFileName = fileName
//            }
//
//            true
//
//        }
//    }






    buildTypes {

        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    splits{
        // 基于不同的abi架构配置不同的apk
        abi {

            // 必须为true，打包才会为不同的abi生成不同的apk
            isEnable = true

            // 默认情况下，包含了所有的ABI。
            // 所以使用reset()清空所有的ABI，再使用include指定我们想要生成的架构armeabi-v7a、arm-v8a
            reset()

            // 逗号分隔列表的形式指定 Gradle 应针对哪些 ABI 生成 APK。只与 reset() 结合使用，以指定确切的 ABI 列表。
            // include "armeabi-v7a", "arm64-v8a"

            //noinspection ChromeOsAbiSupport
            include(*abiCodes.keys.toTypedArray())

            // 是否生成通用的apk，也就是包含所有ABI的apk。如果设为 true，那么除了按 ABI 生成的 APK 之外，Gradle 还会生成一个通用 APK。
            isUniversalApk  = false
        }
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {


//    implementation fileTree(dir: 'libs', include: ['*.jar','*.aar'])

    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.jar", "*.aar")
            )
        )
    )


    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.litert.metadata)
    implementation(libs.androidx.work)
    implementation(libs.material.icons.extended)

    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.converter.gson)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okHttp)
    implementation(libs.navigation.compose)
    implementation(libs.data.store)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutine.core)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}