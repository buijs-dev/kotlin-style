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
package dev.buijs.style.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class KstyleTest {

    @field:TempDir private lateinit var dir: File

    private val out = ByteArrayOutputStream()
    private val err = ByteArrayOutputStream()
    private val cli = Kstyle(PrintStream(out), PrintStream(err))
    private val unformatted = "class A(\n    @A @B val a: Int,\n)\n"

    @Test
    fun `check reports violations and exits 1, format fixes them, check then exits 0`() {
        // given
        val file = File(dir, "A.kt").apply { writeText(unformatted) }

        // expect
        assertThat(cli.run(listOf("check", dir.path))).isEqualTo(1)
        assertThat(out.toString())
            .contains("A.kt:2:")
            .contains("buijs:parameter-annotation-per-line")
        assertThat(cli.run(listOf("format", dir.path))).isEqualTo(0)
        assertThat(file.readText()).isEqualTo("class A(\n    @A\n    @B\n    val a: Int,\n)\n")
        assertThat(cli.run(listOf("check", file.path))).isEqualTo(0)
    }

    @Test
    fun `with a license header, format adds it where missing`() {
        // given
        val header = File(dir, "HEADER.txt").apply { writeText("/** Licensed */\n") }

        val src = File(dir, "src").apply { mkdirs() }

        val file = File(src, "A.kt").apply { writeText("class A\n") }

        // expect
        assertThat(cli.run(listOf("check", "--license-header", header.path, src.path))).isEqualTo(1)
        assertThat(out.toString()).contains("buijs:license-header")
        assertThat(
            cli.run(listOf("format", "--license-header", header.path, src.path)),
        ).isEqualTo(0)
        assertThat(file.readText()).isEqualTo("/** Licensed */\n\nclass A\n")
    }

    @Test
    fun `build and gradle directories are skipped`() {
        // given
        File(dir, "build/generated").mkdirs()
        File(dir, "build/generated/G.kt").writeText(unformatted)
        File(dir, "src").mkdirs()
        File(dir, "src/A.kt").writeText("class A\n")

        // expect
        assertThat(Kstyle.kotlinFiles(dir).map { it.name }).containsExactly("A.kt")
    }

    @Test
    fun `an unknown command prints the usage and exits 2`() {
        assertThat(cli.run(listOf("lint", dir.path))).isEqualTo(2)
    }

    @Test
    fun `without arguments, without paths or with an option missing its value, the usage and exit 2`() {
        val header = File(dir, "HEADER.txt").apply { writeText("/** Licensed */\n") }

        listOf(
            emptyList(),
            listOf("check"),
            listOf("format", "--no-preserve-line-breaks"),
            listOf("check", "--max-line-length"),
            listOf("check", "--max-line-length", "wide", dir.path),
            listOf("check", "--license-header"),
            listOf("check", "--license-header", File(dir, "missing.txt").path, dir.path),
            listOf("check", "--license-header", dir.path, dir.path),
            listOf("check", dir.path, "--license-header", header.path),
        ).forEach { args ->
            err.reset()
            val code = cli.run(args)
            if (args.lastOrNull() == header.path) {
                // A trailing option is still an option: the path before it is checked.
                assertThat(code).describedAs("$args").isEqualTo(0)
            } else {
                assertThat(code).describedAs("$args").isEqualTo(2)
                assertThat(err.toString()).describedAs("$args").startsWith("Usage: kstyle")
            }
        }

        assertThat(out.toString()).doesNotContain("formatted")
    }

    @Test
    fun `check prints every violation with its place and rule, then the total`() {
        // given
        File(dir, "A.kt").writeText(unformatted)
        File(dir, "B.kt").writeText("class B\n")

        // when
        val code = cli.run(listOf("check", dir.path))

        // then
        val lines = out.toString().lines().filter { it.isNotBlank() }

        assertThat(code).isEqualTo(1)
        assertThat(lines.dropLast(1)).allMatch {
            it.matches(Regex(".*A\\.kt:\\d+:\\d+: .+ \\([a-z]+:[a-z-]+\\)"))
        }

        assertThat(lines.last()).isEqualTo("${lines.size - 1} violation(s) in 2 file(s)")
        assertThat(err.toString()).isEmpty()
    }

    @Test
    fun `format names the files it changed and leaves the others untouched`() {
        // given
        val changed = File(dir, "A.kt").apply { writeText(unformatted) }

        val styled = File(dir, "B.kt").apply { writeText("class B\n") }

        val modified = styled.lastModified()

        // when
        val code = cli.run(listOf("format", changed.path, styled.path))

        // then
        assertThat(code).isEqualTo(0)
        assertThat(out.toString().lines())
            .contains("formatted ${changed.path}", "1 of 2 file(s) formatted")
            .doesNotContain("formatted ${styled.path}")
        assertThat(styled.lastModified()).isEqualTo(modified)
    }

    @Test
    fun `max-line-length sets the limit lines are checked against`() {
        // given
        File(dir, "A.kt").writeText("val text = \"" + "x".repeat(50) + "\"\n")

        // expect
        assertThat(cli.run(listOf("check", dir.path))).isEqualTo(0)
        assertThat(cli.run(listOf("check", "--max-line-length", "40", dir.path))).isEqualTo(1)
        assertThat(out.toString()).contains("(standard:max-line-length)")
    }

    @Test
    fun `no-preserve-line-breaks removes a blank line someone wrote, by default it stays`() {
        // given
        val source = "class A {\n\n    val a = 1\n}\n"
        val file = File(dir, "A.kt").apply { writeText(source) }

        // expect
        assertThat(cli.run(listOf("format", file.path))).isEqualTo(0)
        assertThat(file.readText()).isEqualTo(source)
        assertThat(cli.run(listOf("format", "--no-preserve-line-breaks", file.path))).isEqualTo(0)
        assertThat(file.readText()).isEqualTo("class A {\n    val a = 1\n}\n")
    }

    @Test
    fun `scripts are included, the root is entered even when it is called build`() {
        // given
        val root = File(dir, "build").apply { mkdirs() }

        File(root, "a.gradle.kts").writeText("")
        File(root, "B.kt").writeText("")
        File(root, "notes.txt").writeText("")
        File(root, ".gradle").mkdirs()
        File(root, ".gradle/C.kt").writeText("")
        File(root, "nested/build").mkdirs()
        File(root, "nested/build/D.kt").writeText("")

        // expect
        assertThat(
            Kstyle.kotlinFiles(root).map { it.name },
        ).containsExactly("B.kt", "a.gradle.kts")
        assertThat(
            Kstyle.kotlinFiles(File(root, "B.kt")).map { it.name },
        ).containsExactly("B.kt")
        assertThat(Kstyle.kotlinFiles(File(dir, "missing"))).isEmpty()
    }

    @Test
    fun `the kstyle main exits with the code of the command`() {
        // given
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process =
            ProcessBuilder(
                java,
                "-cp",
                System.getProperty("java.class.path"),
                "dev.buijs.style.cli.MainKt",
            ).redirectErrorStream(true)
                .start()

        // when
        val output = process.inputStream.bufferedReader().readText()

        // then
        assertThat(process.waitFor()).isEqualTo(2)
        assertThat(output).startsWith("Usage: kstyle")
    }
}
