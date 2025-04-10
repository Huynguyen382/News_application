pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://jitpack.io") // ✅ Bắt buộc!
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // ✅ Cũng cần thêm ở đây
    }
}

rootProject.name = "VNNews-app"
include(":app")
 