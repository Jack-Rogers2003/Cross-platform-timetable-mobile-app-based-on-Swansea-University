enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }

    }
}

rootProject.name = "MobileApp"
include(":androidApp")
include(":shared")