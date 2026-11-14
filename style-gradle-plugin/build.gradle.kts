plugins {
    id("buijs.gradle-plugin")
}

description = "Gradle plugin applying the Kotlin style"

gradlePlugin {
    plugins {
        create("kotlinStyle") {
            id = "dev.buijs.style"
            implementationClass = "dev.buijs.style.gradle.KotlinStylePlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    // Only compiled against: at runtime the rules come from the isolated worker classpath, so
    // ktlint and its Kotlin compiler never reach the build's own classpath.
    compileOnly(project(":style-rules"))

    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.test)
    testImplementation(kotlin("test"))
}

// The plugin resolves style-rules in its own version.
val writeVersion =
    tasks.register("writeVersion") {
        val output = layout.buildDirectory.file("generated/version/kotlin-style.properties")
        val version = project.version.toString()
        inputs.property("version", version)
        outputs.file(output)
        doLast { output.get().asFile.writeText("version=$version\n") }
    }

sourceSets.main {
    resources.srcDir(writeVersion.map { it.outputs.files.singleFile.parentFile })
}

// The functional tests resolve style-rules from a repository; publish it to one inside build/.
val testRepository = layout.buildDirectory.dir("test-repository")

tasks.test {
    dependsOn(":style-rules:publishMavenJavaPublicationToTestRepository")
    systemProperty("kotlin.style.testRepository", testRepository.get().asFile.absolutePath)
    systemProperty("kotlin.style.version", project.version.toString())
}
