/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtPsiFactory
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

fun <T : PsiElement> KtPsiFactory.collapseWhiteSpaces(element: T): T {
    var firstWhiteSpace: PsiWhiteSpace? = null
    var currentWhiteSpace = StringBuilder()
    for (child in element.allChildren) {
        if (child is PsiWhiteSpace) {
            val isFirst = firstWhiteSpace == null
            if (isFirst) firstWhiteSpace = child
            currentWhiteSpace.append(child.text)
            if (!isFirst) element.remove(child)
        } else {
            val text = if (currentWhiteSpace.contains('\n')) "\n" else " "
            val whiteSpace = createWhiteSpace(text)
            firstWhiteSpace?.let { element.replace(it, whiteSpace) }
            firstWhiteSpace = null
            currentWhiteSpace = StringBuilder()
            collapseWhiteSpaces(child)
        }
    }
    val text = if (currentWhiteSpace.contains('\n')) "\n" else " "
    val whiteSpace = createWhiteSpace(text)
    firstWhiteSpace?.let { element.replace(it, whiteSpace) }
    return element
}

inline fun <reified T> PsiElement.mapWhile(map: (T) -> PsiElement): PsiElement {
    var node = this
    while (node is T)
        node = map(node)
    return node
}

fun PsiElement.replace(oldChild: PsiElement, newChild: PsiElement) {
    node.replaceChild(oldChild.node, newChild.node)
}

fun PsiElement.replaceSelf(newSelf: PsiElement) {
    parent!!.replace(this, newSelf)
}

fun PsiElement.remove(child: PsiElement) {
    node.removeChild(child.node)
}

fun PsiElement.contains(other: PsiElement): Boolean {
    var node: PsiElement? = other
    while (node != null) {
        if (this == node) return true
        node = node.parent
    }
    return false
}

fun <T> PsiElement.findByClass(elementClass: Class<T>): T? {
    @Suppress("UNCHECKED_CAST")
    if (elementClass.isInstance(this)) return this as T
    for (child in allChildren) {
        val element = child.findByClass(elementClass)
        if (element != null) return element
    }
    return null
}

fun PsiElement.elementsIn(range: TextRange): List<PsiElement> {
    if (range.contains(textRange)) return listOf(this)
    return allChildren.asSequence()
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
    var node: PsiElement? = if (strict) parent else this
    while (node != null && !predicate(node))
        node = node.parent
    return node as? T
}

inline fun <reified T : PsiElement> PsiElement.getParentByType(strict: Boolean, type: IElementType): T? =
    getParent(strict) { it.node.elementType == type }