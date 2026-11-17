pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven("https://repo.repsy.io/buijs-dev/maven2") {
            mavenContent {
                includeGroupByRegex("dev\\.buijs.*")
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}