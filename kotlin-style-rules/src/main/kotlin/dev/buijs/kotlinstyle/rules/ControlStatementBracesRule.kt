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

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * An `if` statement or a loop written on one line gets braces, with its body on a line of its own:
 * never `if (x == null) return 0`. The standard rules `multiline-if-else` and `multiline-loop` do
 * the same for one written over several lines; this rule adds the one-liners they allow.
 *
 * An `if` used as an expression keeps its short form: `val x = if (a) 1 else 2`, a function body
 * `= if (a) 1 else 2`, `return if (a) 1 else 2`, a `when` branch, and the if-else a lambda ends
 * with, which is its value.
 */
class ControlStatementBracesRule :
    Rule(
        RuleId("$RULE_SET_ID:control-statement-braces"),
        About(),
        usesEditorConfigProperties = setOf(INDENT_SIZE_PROPERTY, INDENT_STYLE_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {

    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        // One written over several lines is left to the standard rules.
        if (node.textContains('\n')) {
            return
        }

        val bodies =
            when (node.elementType) {
                ElementType.IF -> if (node.isStatement()) node.branches() else emptyList()
                in LOOPS -> listOfNotNull(node.findChildByType(ElementType.BODY))
                else -> emptyList()
            }.filter { it.isWithoutBraces() }

        if (bodies.isEmpty()) {
            return
        }

        emit(node.startOffset, "Missing { ... } around the body, on a line of its own", true)
            .ifAutocorrectAllowed { bodies.forEach(::wrapInBlock) }
    }

    /**
     * Whether this `if` is a statement, not a value: it stands in a block, and is not the if-else a
     * lambda ends with. An `else if` is reached through the `if` it belongs to, see [branches].
     */
    private fun ASTNode.isStatement(): Boolean {
        if (treeParent.elementType != ElementType.BLOCK) {
            return false
        }

        val endsLambda =
            treeParent.treeParent.elementType == ElementType.FUNCTION_LITERAL &&
                nextCodeSibling() == null

        return !endsLambda || findChildByType(ElementType.ELSE) == null
    }

    /** The `then` and `else` of this `if`, and of every `else if` behind it. */
    private fun ASTNode.branches(): List<ASTNode> {
        val then = listOfNotNull(findChildByType(ElementType.THEN))
        val otherwise = findChildByType(ElementType.ELSE) ?: return then
        val elseIf = otherwise.firstChildNode?.takeIf { it.elementType == ElementType.IF }

        return then + (elseIf?.branches() ?: listOf(otherwise))
    }

    /** A body that holds a statement, not a block; an empty one (`while (next());`) has nothing to wrap. */
    private fun ASTNode.isWithoutBraces(): Boolean =
        firstChildNode != null && firstChildNode.elementType != ElementType.BLOCK

    /** As the standard rules `multiline-if-else` and `multiline-loop` wrap a body. */
    private fun wrapInBlock(body: ASTNode) {
        val before =
            body
                .leaves(forward = false)
                .takeWhile { it.elementType !in BODY_OPENERS }
                .toList()
                .reversed()
        val after =
            body
                .leaves(forward = true)
                .takeWhile { it.isWhiteSpaceWithoutNewline20 || it.isPartOfComment20 }
                .toList()
                .dropLastWhile { it.isWhiteSpaceWithoutNewline20 }

        before.firstOrNull()?.takeIf { it.isWhiteSpace20 }?.replaceTextWith(" ")
        KtBlockExpression(null).apply {
            val statement = body.firstChildNode
            body.replaceChild(statement, this)
            addChild(LeafPsiElement(ElementType.LBRACE, "{"))
            addChild(PsiWhiteSpaceImpl(indentConfig.childIndentOf(body)))
            before.dropWhile { it.isWhiteSpace20 }.forEach(::addChild)
            addChild(statement)
            after.forEach(::addChild)
            addChild(PsiWhiteSpaceImpl(body.indent20))
            addChild(LeafPsiElement(ElementType.RBRACE, "}"))
        }

        // What follows on the line, an `else` or the `while` of a `do`, starts behind the new brace. A comment
        // behind the statement went into the braces with it, so it is never what follows.
        body.treeNext?.upsertWhitespaceBeforeMe(" ")
    }

    private companion object {

        val LOOPS = setOf(ElementType.FOR, ElementType.WHILE, ElementType.DO_WHILE)

        /** What a body follows: the `)` of a condition, `else`, or `do`. */
        val BODY_OPENERS = setOf(ElementType.RPAR, ElementType.ELSE_KEYWORD, ElementType.DO_KEYWORD)
    }
}
