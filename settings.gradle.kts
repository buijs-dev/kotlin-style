pluginManagement {
    repositories {
        mavenLocal()
        maven("https://repo.repsy.io/buijs-dev/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("gradle-build")
include(":kotlin-style-rules")
include(":kotlin-style-cli")
include(":kotlin-style-gradle-plugin")
