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

package dev.buijs.kotlinstyle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The options of [KotlinStyle] at their edges: the license header and the max line length. */
class KotlinStyleOptionsTest {

    private val licensed = KotlinStyle(licenseHeader = HEADER)

    @Test
    fun `a configured header is trimmed before it is compared`() {
        val padded = KotlinStyle(licenseHeader = "\n  $HEADER  \n\n")
        val source = "$HEADER\n\npackage a\n\nclass A\n"

        assertThat(padded.check(source, "A.kt")).isEmpty()
        assertThat(padded.format("package a\n\nclass A\n")).isEqualTo(source)
    }

    @Test
    fun `a header below blank lines counts as present`() {
        assertThat(licensed.check("\n\n$HEADER\n\npackage a\n\nclass A\n", "A.kt")).isEmpty()
    }

    @Test
    fun `another header is not replaced, the configured one goes above it`() {
        val other = "/** Someone else */\n\npackage a\n\nclass A\n"

        assertThat(licensed.check(other, "A.kt").map { it.ruleId })
            .containsExactly(KotlinStyle.LICENSE_HEADER_RULE_ID)
        assertThat(licensed.format(other)).isEqualTo("$HEADER\n\n$other")
    }

    @Test
    fun `the header violation is on the first line and formatting fixes it for good`() {
        val violation = licensed.check("class A\n", "A.kt").single()

        assertThat(violation.line).isEqualTo(1)
        assertThat(violation.column).isEqualTo(1)
        assertThat(violation.message).isEqualTo("Missing license header")

        val once = licensed.format("class A\n", "A.kt")

        assertThat(licensed.format(once, "A.kt")).isEqualTo(once)
        assertThat(licensed.check(once, "A.kt")).isEmpty()
    }

    @Test
    fun `a script is left without a header even when formatting changes it`() {
        assertThat(licensed.format("val a  =  1\n", "extra.gradle.kts")).isEqualTo("val a = 1\n")
    }

    @Test
    fun `the max line length is configurable`() {
        val line = "val text = \"" + "x".repeat(50) + "\"\n"
        val narrow = KotlinStyle(maxLineLength = 40)

        assertThat(narrow.check(line, "A.kt").map { it.ruleId })
            .contains("standard:max-line-length")
        assertThat(KotlinStyle().check(line, "A.kt")).isEmpty()
    }

    private companion object {

        const val HEADER = "/**\n * Copyright (c) Buijs Software\n */"
    }
}
