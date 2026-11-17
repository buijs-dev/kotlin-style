plugins {
    id("dev.buijs.conventions.coverage")
}

val kstyle = configurations.create("kstyle") { isCanBeConsumed = false }

dependencies {
    kstyle(project(":kotlin-style-cli"))

    // One coverage report over every module (build/reports/kover/report.xml, for Codecov).
    kover(project(":kotlin-style-rules"))
    kover(project(":kotlin-style-cli"))
    kover(project(":kotlin-style-gradle-plugin"))
}

// kotlin-style can't apply its own plugin (it is built here), so it formats itself with
// kotlin-style-cli: `kotlinStyleApply` formats, `kotlinStyleCheck` only checks (for CI).
val licenseHeader = layout.projectDirectory.file("gradle/license-header.txt")
val styledModules = listOf("kotlin-style-rules", "kotlin-style-cli", "kotlin-style-gradle-plugin")
val styledSources =
    buildList {
        add("build.gradle.kts")
        add("settings.gradle.kts")
        styledModules.forEach { module ->
            add("$module/src")
            add("$module/build.gradle.kts")
        }
    }

fun registerStyleTask(
    name: String,
    command: String,
) = tasks.register<JavaExec>(name) {
    group = "formatting"
    description = "Runs kstyle $command over this build's own Kotlin sources."
    classpath = kstyle
    mainClass.set("dev.buijs.kotlinstyle.cli.MainKt")
    args(command, "--license-header", licenseHeader.asFile.path)
    args(
        styledSources.map {
            layout.projectDirectory
                .file(it)
                .asFile.path
        },
    )
}

val styleApply = registerStyleTask("kotlinStyleApply", "format")
registerStyleTask("kotlinStyleCheck", "check")

// Unlike the plugin, this can't format before compiling: kstyle is compiled from these sources.
subprojects {
    pluginManager.withPlugin("base") {
        tasks.named("build") {
            dependsOn(styleApply)
        }
    }
}
