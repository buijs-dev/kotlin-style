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
package dev.buijs.kotlinstyle.rules

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
        assertUnchanged("fun f() {\n    if (a) {\n        b()\n    }; val c = 1\n}\n")
        assertThat(format("fun f() {\n    if (a) {\n        b()\n    } // one\n    val c = 1\n}\n"))
            .isEqualTo("fun f() {\n    if (a) {\n        b()\n    } // one\n\n    val c = 1\n}\n")
    }

    @Test
    fun `braces on one line are not followed by a blank line, one written there is kept`() {
        assertUnchanged("fun f() {\n    val a = run { 1 }\n    val b = a\n}\n")
        assertUnchanged("fun f() {\n    val a = run { 1 } // one\n    val b = a\n}\n")
        assertUnchanged("fun f() {\n    val a = run { 1 }\n\n    val b = a\n}\n")
        assertUnchanged("class A {\n    fun a() {}\n    fun b() {}\n}\n")
        assertThat(violations("fun f() {\n    val a = run { 1 }\n    val b = a\n}\n")).isEmpty()
    }

    @Test
    fun `comments between declarations are not statements`() {
        assertThat(
            format("class A {\n    fun a() {\n    }\n    /** b */\n    /* c */\n    fun b() {}\n}\n"),
        ).isEqualTo(
            "class A {\n    fun a() {\n    }\n\n    /** b */\n    /* c */\n    fun b() {}\n}\n",
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

    // --- control-statement-braces

    @Test
    fun `an if statement on one line gets braces, with its body on a line of its own`() {
        // The blank line behind the new brace is the rule blank-line-after-closing-brace.
        assertThat(format("fun f(x: Int?): Int {\n    if (x == null) return 0\n    return x\n}\n"))
            .isEqualTo("fun f(x: Int?): Int {\n    if (x == null) {\n        return 0\n    }\n\n    return x\n}\n")
        assertThat(violations("fun f(x: Int?): Int {\n    if (x == null) return 0\n    return x\n}\n"))
            .containsExactly("buijs:control-statement-braces")
    }

    @Test
    fun `every branch of an if statement on one line gets braces, an else if stays behind its else`() {
        assertThat(format("fun f(x: Int) {\n    if (x > 1) a() else b()\n}\n"))
            .isEqualTo("fun f(x: Int) {\n    if (x > 1) {\n        a()\n    } else {\n        b()\n    }\n}\n")
        assertThat(format("fun f(x: Int) {\n    if (x > 1) a() else if (x < 0) b() else c()\n}\n"))
            .isEqualTo(
                "fun f(x: Int) {\n    if (x > 1) {\n        a()\n    } else if (x < 0) {\n        b()\n" +
                    "    } else {\n        c()\n    }\n}\n",
            )
        assertThat(format("fun f(x: Int) {\n    if (x > 1) a() else if (x < 0) b()\n}\n"))
            .isEqualTo(
                "fun f(x: Int) {\n    if (x > 1) {\n        a()\n    } else if (x < 0) {\n        b()\n    }\n}\n",
            )
    }

    @Test
    fun `a branch that already has braces is left, the one without gets them`() {
        assertThat(format("fun f(x: Int) {\n    if (x > 1) { a() } else b()\n}\n"))
            .isEqualTo("fun f(x: Int) {\n    if (x > 1) { a() } else {\n        b()\n    }\n}\n")
        assertUnchanged("fun f(x: Int) {\n    if (x > 1) {\n        a()\n    }\n}\n")
        assertUnchanged("fun f(x: Int) {\n    if (x > 1) { a() } else { b() }\n}\n")
    }

    @Test
    fun `a comment behind the statement goes into the braces with it`() {
        assertThat(format("fun f(x: Int?): Int {\n    if (x == null) return 0 // none\n    return x\n}\n"))
            .isEqualTo(
                "fun f(x: Int?): Int {\n    if (x == null) {\n        return 0 // none\n    }\n\n    return x\n}\n",
            )
    }

    @Test
    fun `an empty else is left, a body glued to its condition or behind a comment is wrapped as it is`() {
        assertThat(format("fun f(a: Boolean) {\n    if (a) b() else;\n}\n"))
            .isEqualTo("fun f(a: Boolean) {\n    if (a) {\n        b()\n    } else;\n}\n")
        // The space before the brace is left to the standard rules.
        assertThat(format("fun f(a: Boolean) {\n    if (a)b()\n}\n"))
            .isEqualTo("fun f(a: Boolean) {\n    if (a){\n        b()\n    }\n}\n")
        assertThat(format("fun f(a: Boolean) {\n    if (a)/* why */ b()\n}\n"))
            .isEqualTo("fun f(a: Boolean) {\n    if (a){\n        /* why */ b()\n    }\n}\n")
        assertThat(format("fun f(a: Boolean) {\n    if (a) b() /* b */ else c()\n}\n"))
            .isEqualTo(
                "fun f(a: Boolean) {\n    if (a) {\n        b() /* b */\n    } else {\n        c()\n    }\n}\n",
            )
    }

    @Test
    fun `an if used as an expression keeps its short form`() {
        assertUnchanged("val x = if (a) 1 else 2\n")
        assertUnchanged("fun f(a: Boolean) = if (a) 1 else 2\n")
        assertUnchanged("fun f(a: Boolean): Int {\n    return if (a) 1 else 2\n}\n")
        assertUnchanged("fun f(a: Int) =\n    when (a) {\n        1 -> if (b) 1 else 2\n        else -> 3\n    }\n")
        assertUnchanged("val f = listOf(1).map { if (it > 0) it else 0 }\n")
        assertUnchanged("val f = listOf(1).map {\n    val y = it + 1\n    if (y > 0) y else 0\n}\n")
    }

    @Test
    fun `an if statement in a lambda gets braces, unless it is the if-else the lambda ends with`() {
        assertThat(format("val f = listOf(1).forEach {\n    if (it > 0) println(it)\n}\n"))
            .isEqualTo("val f = listOf(1).forEach {\n    if (it > 0) {\n        println(it)\n    }\n}\n")
        assertThat(format("val f = listOf(1).map {\n    if (it > 0) a() else b()\n    it\n}\n"))
            .isEqualTo(
                "val f = listOf(1).map {\n    if (it > 0) {\n        a()\n    } else {\n        b()\n    }\n\n    it\n}\n",
            )
    }

    @Test
    fun `a loop on one line gets braces, one without a body is left`() {
        assertThat(format("fun f(xs: List<Int>) {\n    for (x in xs) println(x)\n}\n"))
            .isEqualTo("fun f(xs: List<Int>) {\n    for (x in xs) {\n        println(x)\n    }\n}\n")
        assertThat(format("fun f() {\n    while (more()) next()\n}\n"))
            .isEqualTo("fun f() {\n    while (more()) {\n        next()\n    }\n}\n")
        assertThat(format("fun f() {\n    do next() while (more())\n}\n"))
            .isEqualTo("fun f() {\n    do {\n        next()\n    } while (more())\n}\n")
        assertUnchanged("fun f() {\n    while (more());\n}\n")
        assertUnchanged("fun f(xs: List<Int>) {\n    for (x in xs) {\n        println(x)\n    }\n}\n")
    }

    @Test
    fun `an if or loop written over several lines is left to the standard rules`() {
        assertUnchanged("fun f(x: Int?): Int {\n    if (x == null)\n        return 0\n    return x\n}\n")
        assertUnchanged("fun f(xs: List<Int>) {\n    for (x in xs)\n        println(x)\n}\n")
        assertThat(violations("fun f(x: Int?): Int {\n    if (x == null)\n        return 0\n    return x\n}\n"))
            .isEmpty()
    }
}
