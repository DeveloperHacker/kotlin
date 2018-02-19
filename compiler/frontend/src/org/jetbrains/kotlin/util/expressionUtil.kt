/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression


fun PsiElement.matchPredecessors(vararg predecessors: IElementType): Boolean {
    return matchPredecessors(predecessors.toList())
}

fun PsiElement.matchPredecessors(predecessors: List<IElementType>): Boolean {
    var node = this
    for (predecessor in predecessors) {
        node = node.parent
        if (node == null) return false
        if (node.node.elementType != predecessor) return false
    }
    return true
}

fun KtExpression.skipBinaryExpression(operation: IElementType): KtExpression {
    var prevNode = this
    var node = parent
    while (node is KtBinaryExpression && node.operationToken == operation) {
        prevNode = node
        node = node.parent
    }
    return prevNode
}

val KtExpression.commonForIsLikeExpression: KtExpression
    get() = skipBinaryExpression(KtTokens.ANDAND)