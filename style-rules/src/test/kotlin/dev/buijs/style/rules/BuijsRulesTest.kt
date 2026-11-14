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
package dev.buijs.style.rules

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The custom rules on their own, as the ktlint CLI and IDE plugins load them: without the
 * standard rules, so each case shows what one custom rule does.
 */
class BuijsRulesTest {

    private val engine =
        KtLintRuleEngine(
            ruleProviders = BuijsRuleSetProvider().getRuleProviders(),
            editorConfigOverride = EditorConfigOverride.from(MAX_LINE_LENGTH_PROPERTY to 100),
        )

    private fun format(source: String): String =
        engine.format(Code.fromSnippet(source)) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

    private fun violations(source: String): List<String> {
        val ruleIds = mutableListOf<String>()
        engine.lint(Code.fromSnippet(source)) { ruleIds += it.ruleId.value }

        return ruleIds
    }

    private fun assertUnchanged(source: String) {
        assertThat(format(source)).isEqualTo(source)
    }

    // --- single-line-lambda

    @Test
    fun `without the style, every lambda that fits is joined, written over several lines or not`() {
        assertThat(format("val f = listOf(1).map {\n    it\n}\n"))
            .isEqualTo("val f = listOf(1).map { it }\n")
    }

    @Test
    fun `a lambda with parameters is joined behind its arrow`() {
        assertThat(format("val f = listOf(1).fold(0) { acc, x ->\n    acc + x\n}"))
            .isEqualTo("val f = listOf(1).fold(0) { acc, x -> acc + x }")
    }

    @Test
    fun `a lambda holding a comment, parameters over several lines or too long a line stays`() {
        assertUnchanged("val f = listOf(1).map {\n    // nothing\n}\n")
        assertUnchanged("val f = listOf(1).fold(0) { acc,\n    x ->\n    acc + x\n}\n")
        assertUnchanged(
            "val f = listOf(1).map {\n" +
                "    it + 1000000000 + 1000000000 + 1000000000 + " +
                "1000000000 + 1000000000 + 1000000000 + 1000\n" +
                "}\n",
        )
    }

    // --- no-empty-line-comment

    @Test
    fun `an empty line comment goes at the start, at the end and right behind code`() {
        // The line break after it is left to the standard rules.
        assertThat(format("//\nval x = 1\n")).isEqualTo("\nval x = 1\n")
        assertThat(format("val x = 1\n//")).isEqualTo("val x = 1\n")
        assertThat(format("val x = 1//\n")).isEqualTo("val x = 1\n")
    }

    // --- parameter-annotation-per-line

    @Test
    fun `annotations before a modifier, glued to a keyword or behind a comment`() {
        assertThat(format("class A(\n    @A @B private val a: Int,\n)\n"))
            .isEqualTo("class A(\n    @A\n    @B\n    private val a: Int,\n)\n")
        assertThat(format("class A(\n    @A(1)val a: Int,\n)\n"))
            .isEqualTo("class A(\n    @A(1)\n    val a: Int,\n)\n")
        assertUnchanged("class A(\n    @A /* a */@B\n    val a: Int,\n)\n")
    }

    @Test
    fun `a comment between the annotations and a modifier is skipped`() {
        assertThat(format("class A(\n    @A /* a */ private val a: Int,\n)\n"))
            .isEqualTo("class A(\n    @A /* a */\n    private val a: Int,\n)\n")
    }

    // --- blank-line-between-annotated-parameters

    @Test
    fun `a plain parameter followed by an annotated one gets a blank line too`() {
        assertThat(format("class A(\n    val a: Int,\n    @B\n    val b: Int,\n)\n"))
            .isEqualTo("class A(\n    val a: Int,\n\n    @B\n    val b: Int,\n)\n")
    }

    @Test
    fun `a comment right behind the comma keeps the parameters as they are`() {
        assertUnchanged("class A(\n    @A\n    val a: Int,/* a */\n    val b: Int,\n)\n")
        assertUnchanged("class A(\n    @A\n    val a: Int, // a\n    val b: Int,\n)\n")
    }

    // --- blank-line-after-closing-brace

    @Test
    fun `statements on one line or behind a trailing comment`() {
        assertUnchanged("fun f() {\n    val a = run { 1 }; val b = a\n}\n")
        assertThat(format("fun f() {\n    val a = run { 1 } // one\n    val b = a\n}\n"))
            .isEqualTo("fun f() {\n    val a = run { 1 } // one\n\n    val b = a\n}\n")
    }

    @Test
    fun `comments between declarations are not statements`() {
        assertThat(
            format("class A {\n    fun a() {}\n    /** b */\n    /* c */\n    fun b() {}\n}\n"),
        ).isEqualTo(
            "class A {\n    fun a() {}\n\n    /** b */\n    /* c */\n    fun b() {}\n}\n",
        )
    }

    // --- single-line-condition

    @Test
    fun `a condition opening on a line of its own is joined`() {
        assertThat(
            format(
                "fun f(x: Int) {\n    while (\n        x == 1 ||\n        x == 2\n    ) {\n    }\n}\n",
            ),
        ).isEqualTo("fun f(x: Int) {\n    while (x == 1 || x == 2) {\n    }\n}\n")
    }

    @Test
    fun `a condition with a comment, a when, an object or a multi-statement lambda is reported`() {
        listOf(
            "x == 1 || // one\n        x == 2",
            "when (x) {\n            1 -> true\n            else -> false\n        }",
            "object {\n            val y = x\n        }.y == 1",
            "listOf(x).any { val y = it; y > 1 } ||\n        x == 2",
        ).forEach { condition ->
            val source = "fun f(x: Int) {\n    if ($condition) {\n        println(x)\n    }\n}\n"

            assertThat(
                violations(source),
            ).describedAs(condition).contains("buijs:single-line-condition")
            assertThat(format(source)).describedAs(condition).isEqualTo(source)
        }
    }

    @Test
    fun `a condition holding a single-statement lambda is joined`() {
        assertThat(
            format(
                "fun f(x: Int) {\n    if (listOf(x).any { it > 1 } ||\n        x == 2\n    ) {\n    }\n}",
            ),
        ).isEqualTo("fun f(x: Int) {\n    if (listOf(x).any { it > 1 } || x == 2) {\n    }\n}")
    }
}
