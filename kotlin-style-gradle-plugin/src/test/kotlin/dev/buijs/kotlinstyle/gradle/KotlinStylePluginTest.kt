/**
 * Copyright (c) 2021 - 2026 Buijs Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.buijs.kotlinstyle.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The plugin's wiring, in process: the extension, the tasks, the sources and the build. */
class KotlinStylePluginTest {

    @field:TempDir private lateinit var projectDir: File

    private fun project(vararg plugins: String): Project =
        ProjectBuilder.builder().withProjectDir(projectDir).build().also { project ->
            plugins.forEach { project.pluginManager.apply(it) }

            project.pluginManager.apply(KotlinStylePlugin::class.java)
        }

    private val Project.extension get() = extensions.getByType(KotlinStyleExtension::class.java)

    private fun Project.styleTask(name: String) = tasks.getByName(name) as StyleTask

    private fun Task.dependencyNames() = taskDependencies.getDependencies(this).map { it.name }

    private fun file(path: String) =
        File(projectDir, path).canonicalFile.apply {
            parentFile.mkdirs()
            writeText("class A\n")
        }

    /** The `kotlin` source directory set the Kotlin plugin adds to every source set. */
    private fun Project.kotlinSourceSet(vararg directories: String) {
        val main = extensions.getByType(SourceSetContainer::class.java).getByName("main")
        val kotlin = objects.sourceDirectorySet("kotlin", "Kotlin").srcDirs(*directories)
        main.extensions.add(SourceDirectorySet::class.java, "kotlin", kotlin)
    }

    @Test
    fun `the extension has its defaults and both tasks read it`() {
        val project = project()

        assertThat(project.extension.maxLineLength.get()).isEqualTo(100)
        assertThat(project.extension.applyOnBuild.get()).isTrue()
        assertThat(project.extension.preserveLineBreaks.get()).isTrue()
        assertThat(project.extension.licenseHeader.isPresent).isFalse()

        project.extension.maxLineLength.set(80)
        project.extension.preserveLineBreaks.set(false)
        project.extension.licenseHeader.set("/** Licensed */")

        val apply = project.styleTask(KotlinStylePlugin.APPLY_TASK)
        val check = project.styleTask(KotlinStylePlugin.CHECK_TASK)
        listOf(apply, check).forEach {
            assertThat(it.group).isEqualTo("formatting")
            assertThat(it.maxLineLength.get()).isEqualTo(80)
            assertThat(it.preserveLineBreaks.get()).isFalse()
            assertThat(it.licenseHeader.get()).isEqualTo("/** Licensed */")
        }

        assertThat(apply.check.get()).isFalse()
        assertThat(check.check.get()).isTrue()
    }

    @Test
    fun `the worker classpath defaults to the style rules of the plugin's own version`() {
        val project = project()
        val configuration = project.configurations.getByName(KotlinStylePlugin.CONFIGURATION)

        // The default dependencies are added when the dependencies are asked for.
        val dependencies =
            configuration.incoming.dependencies.map { "${it.group}:${it.name}:${it.version}" }

        assertThat(configuration.isCanBeConsumed).isFalse()
        assertThat(dependencies).hasSize(2)
        assertThat(
            dependencies.first(),
        ).startsWith("dev.buijs.kotlinstyle:kotlin-style-rules:").doesNotEndWith(":null")
        assertThat(dependencies.last()).startsWith("org.slf4j:slf4j-nop:")
    }

    @Test
    fun `the sources are every Kotlin source set plus the build scripts, never the build directory`() {
        val project = project("java")
        project.kotlinSourceSet("src/main/kotlin", "build/generated/kotlin")
        val source = file("src/main/kotlin/A.kt")
        file("build/generated/kotlin/G.kt")
        val script = file("build.gradle.kts")
        file("notes.kts")

        val sources = project.styleTask(KotlinStylePlugin.APPLY_TASK).sources.files

        assertThat(sources).containsExactlyInAnyOrder(source, script)
    }

    @Test
    fun `without a Kotlin source set, or without source sets at all, only the scripts`() {
        file("src/main/kotlin/A.kt")
        val script = file("settings.gradle.kts")

        assertThat(project("java").styleTask(KotlinStylePlugin.CHECK_TASK).sources.files)
            .containsExactly(script)
    }

    @Test
    fun `without the java plugin only the scripts`() {
        val script = file("build.gradle.kts")

        assertThat(project().styleTask(KotlinStylePlugin.CHECK_TASK).sources.files)
            .containsExactly(script)
    }

    @Test
    fun `build runs kotlinStyleApply unless applyOnBuild is off, and never kotlinStyleCheck`() {
        val project = project("base")
        val build = project.tasks.getByName("build")

        assertThat(build.dependencyNames())
            .contains(KotlinStylePlugin.APPLY_TASK)
            .doesNotContain(KotlinStylePlugin.CHECK_TASK)

        project.extension.applyOnBuild.set(false)

        assertThat(build.dependencyNames()).doesNotContain(KotlinStylePlugin.APPLY_TASK)
    }

    @Test
    fun `without the base plugin there is no build to hook into`() {
        assertThat(project().tasks.findByName("build")).isNull()
    }

    @Test
    fun `Kotlin compilation runs after formatting, other tasks are not ordered`() {
        val project = project()
        val compileKotlin = project.tasks.register("compileTestKotlin").get()
        val compileJava = project.tasks.register("compileJava").get()

        assertThat(compileKotlin.mustRunAfter.getDependencies(compileKotlin).map { it.name })
            .containsExactly(KotlinStylePlugin.APPLY_TASK)
        assertThat(compileJava.mustRunAfter.getDependencies(compileJava)).isEmpty()
    }
}
