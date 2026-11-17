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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The work the worker process runs, called directly. */
class StyleWorkTest {

    @field:TempDir private lateinit var dir: File

    private val unformatted = "class A(\n    @A @B val a: Int,\n)\n"

    private val formatted = "class A(\n    @A\n    @B\n    val a: Int,\n)\n"

    private val handFix =
        "fun f(aVeryLongVariableName: Int, anotherVeryLongName: Int) {\n" +
            "    if (aVeryLongVariableName == 1 ||\n" +
            "        anotherVeryLongName == 2 ||\n" +
            "        aVeryLongVariableName + anotherVeryLongName == 3\n" +
            "    ) {\n        println(1)\n    }\n}\n"

    private fun work(
        check: Boolean,
        vararg files: File,
        maxLineLength: Int = 100,
        preserveLineBreaks: Boolean = true,
        licenseHeader: String? = null,
    ): StyleWork {
        val parameters =
            ProjectBuilder
                .builder()
                .build()
                .objects
                .newInstance(StyleWork.Parameters::class.java)
        parameters.files.from(*files)
        parameters.maxLineLength.set(maxLineLength)
        parameters.preserveLineBreaks.set(preserveLineBreaks)
        parameters.licenseHeader.set(licenseHeader)
        parameters.check.set(check)
        return object : StyleWork() {
            override fun getParameters() = parameters
        }
    }

    private fun file(
        name: String,
        content: String,
    ) = File(dir, name).apply { writeText(content) }

    @Test
    fun `apply formats what it can and succeeds`() {
        val file = file("A.kt", unformatted)

        work(check = false, file).execute()

        assertThat(file.readText()).isEqualTo(formatted)
    }

    @Test
    fun `apply fails on what is left, after formatting the rest`() {
        val fixable = file("A.kt", unformatted)
        val manual = file("B.kt", handFix)

        assertThatThrownBy { work(check = false, fixable, manual).execute() }
            .isInstanceOf(GradleException::class.java)
            .hasMessageStartingWith("1 style violation(s); these need fixing by hand:")
            .hasMessageContaining("${manual.path}:")
            .hasMessageContaining("(buijs:single-line-condition)")
        assertThat(fixable.readText()).isEqualTo(formatted)
    }

    @Test
    fun `check fails on anything not in style and changes nothing`() {
        val file = file("A.kt", unformatted)

        assertThatThrownBy { work(check = true, file).execute() }
            .isInstanceOf(GradleException::class.java)
            .hasMessageContaining("run kotlinStyleApply to fix what it can")
            .hasMessageContaining("(buijs:parameter-annotation-per-line)")
        assertThat(file.readText()).isEqualTo(unformatted)
    }

    @Test
    fun `check succeeds on sources in style`() {
        work(check = true, file("A.kt", formatted)).execute()
    }

    @Test
    fun `the options reach the style`() {
        val header = "/** Licensed */"
        val file = file("A.kt", "class A {\n\n    val a = 1\n}\n")

        work(check = false, file, preserveLineBreaks = false, licenseHeader = header).execute()

        assertThat(file.readText()).isEqualTo("$header\n\nclass A {\n    val a = 1\n}\n")
        assertThatThrownBy { work(check = true, file, maxLineLength = 10).execute() }
            .hasMessageContaining("(standard:max-line-length)")
    }
}
