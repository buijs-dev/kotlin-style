plugins {
    id("dev.buijs.conventions.gradle-plugin")
}

description = "Gradle plugin applying the Kotlin style"

gradlePlugin {
    plugins {
        create("kotlinStyle") {
            id = "dev.buijs.kotlin-style"
            implementationClass = "dev.buijs.kotlinstyle.gradle.KotlinStylePlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    // Only compiled against: at runtime the rules come from the isolated worker classpath, so
    // ktlint and its Kotlin compiler never reach the build's own classpath.
    compileOnly(project(":kotlin-style-rules"))

    // StyleWorkTest runs the work in process, with the rules on the test classpath.
    testImplementation(project(":kotlin-style-rules"))
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.test)
    testImplementation(kotlin("test"))
}

// The plugin resolves kotlin-style-rules in its own version.
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

// The functional tests resolve kotlin-style-rules from a repository; publish it to one inside build/.
val testRepository = layout.buildDirectory.dir("test-repository")

tasks.test {
    dependsOn(":kotlin-style-rules:publishMavenPublicationToTestRepository")
    systemProperty("kotlin.style.testRepository", testRepository.get().asFile.absolutePath)
    systemProperty("kotlin.style.version", project.version.toString())
    systemProperty("kotlin.style.kotlinVersion", libs.versions.kotlin.get())
}
