import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.google.gms.google.services)
}

kotlin {
    androidTarget()
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        dependencies {
            implementation(libs.androidx.appcompat.v131)
        }
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.mobileapp"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
dependencies {
    implementation(libs.firebase.auth)
    implementation(libs.material)
    implementation(libs.play.services.tasks)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.postgresql)
    implementation(libs.sqlite.jdbc)
    implementation(libs.androidx.appcompat)
    implementation(libs.gson) // Assuming you have gson defined in your version catalog
    implementation(libs.okhttp)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.simple.xml)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.gms.play.services.tasks)
    implementation(libs.kotlinx.coroutines.android.v152)
    implementation(libs.mrudultora.colorpicker)

}
