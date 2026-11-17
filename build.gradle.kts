plugins {
    id("buijs.coverage")
    id("buijs.dokka")
}

val kstyle = configurations.create("kstyle") { isCanBeConsumed = false }

dependencies {
    kstyle(project(":style-cli"))

    // One coverage report over every module (build/reports/kover/report.xml, for Codecov).
    kover(project(":style-rules"))
    kover(project(":style-cli"))
    kover(project(":style-gradle-plugin"))

    dokka(project(":style-rules"))
    dokka(project(":style-gradle-plugin"))
}

// kotlin-style can't apply its own plugin (it is built here), so it formats itself with
// style-cli: `kotlinStyleApply` formats, `kotlinStyleCheck` only checks (for CI).
val licenseHeader = layout.projectDirectory.file("gradle/license-header.txt")
val styledSources =
    listOf("style-rules", "style-cli", "style-gradle-plugin").flatMap { module ->
        listOf("$module/src", "$module/build.gradle.kts")
    } + listOf("build.gradle.kts", "settings.gradle.kts")

fun registerStyleTask(
    name: String,
    command: String,
) = tasks.register<JavaExec>(name) {
    group = "formatting"
    description = "Runs kstyle $command over this build's own Kotlin sources."
    classpath = kstyle
    mainClass.set("dev.buijs.style.cli.MainKt")
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
subprojects { pluginManager.withPlugin("base") { tasks.named("build") { dependsOn(styleApply) } } }
