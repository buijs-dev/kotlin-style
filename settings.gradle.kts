pluginManagement {
    repositories {
        mavenLocal()
        maven("https://repo.repsy.io/mvn/buijs-dev/maven")
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("gradle-build")
include(":kotlin-style-rules")
include(":kotlin-style-cli")
include(":kotlin-style-gradle-plugin")
