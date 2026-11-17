plugins {
    id("dev.buijs.conventions.kotlin")
    id("dev.buijs.conventions.repositories")
    id("dev.buijs.conventions.coverage")
    application
}

dependencies {
    implementation(project(":style-rules"))
    runtimeOnly(libs.slf4j.nop)

    testImplementation(libs.bundles.test)
    testImplementation(kotlin("test"))
}

application {
    applicationName = "kstyle"
    mainClass.set("dev.buijs.style.cli.MainKt")
}

tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "executable jar with all dependencies"
    archiveFileName.set("kstyle.jar")
    manifest { attributes["Main-Class"] = application.mainClass.get() }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Signatures of the merged jars would no longer match.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    from(sourceSets.main.get().output)
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
}
