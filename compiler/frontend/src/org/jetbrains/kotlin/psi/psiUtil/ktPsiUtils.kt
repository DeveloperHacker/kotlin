/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtPsiUtil.findChildByType
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression

fun PsiElement.debugPrint() {
    println("---BEGIN PSI STRUCTURE---")
    debugPrint(0)
    println("---END PSI STRUCTURE---")
}

fun PsiElement.debugPrint(indentation: Int) {
    println("|".repeat(indentation) + toString())
    for (child in children)
        child.debugPrint(indentation + 1)
    if (children.isEmpty())
        println("|".repeat(indentation + 1) + "'$text'")
}

fun PsiElement.contains(other: PsiElement): Boolean {
    var node: PsiElement? = other
    while (node != null) {
        if (this == node) return true
        node = node.parent
    }
    return false
}

fun PsiElement.elementsIn(range: TextRange): List<PsiElement> {
    if (range.contains(textRange)) return listOf(this)
    return children.asSequence()
        .filterNot { it.textRange.intersects(range) }
        .map { it.elementsIn(range) }
        .flatten()
        .toList()
}

// this code works thanks to furious fucking hacks
fun transferDiagnostics(diagnostics: MutableDiagnosticsWithSuppression, fromTree: PsiElement, toTree: PsiElement) {
    val fromOffset = fromTree.textOffset
    val toOffset = toTree.textOffset
    val shift = fromOffset - toOffset
    for (diagnostic in diagnostics.noSuppression().asSequence().filter { fromTree.contains(it.psiElement) }) {
        val newTextRanges = diagnostic.textRanges.map { it.shiftLeft(shift) }
        val newElement = newTextRanges.map { toTree.elementsIn(it) }.flatten().firstOrNull() ?: continue
        val newDiagnostic = diagnostic.transfer(newElement, newTextRanges)
        diagnostics.report(newDiagnostic)
    }
}

inline fun <reified T : PsiElement> PsiElement.getParent(strict: Boolean, predicate: (PsiElement) -> Boolean): T? {
    var node: PsiElement? = this
    if (strict) {
        node = node?.parent
    }
    while (node != null && !predicate(node)) {
        node = node.parent
    }
    return node as? T
}

inline fun <reified T : PsiElement> PsiElement.getParentByType(strict: Boolean, type: IElementType): T? {
    var node: PsiElement? = this
    if (strict) {
        node = node?.parent
    }
    while (node != null && node.node.elementType != type) {
        node = node.parent
    }
    return node as? T
}

fun PsiElement.getInstanceReference() = findChildByType(this, KtNodeTypes.REFERENCE_EXPRESSION)
