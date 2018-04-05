/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.pattern.KtPattern

class KtIsExpression(node: ASTNode) : KtExpressionImpl(node), KtOperationExpression {

    val leftHandSide: KtExpression
        get() = findChildByClass(KtExpression::class.java)!!

    val pattern: KtPattern?
        get() = findChildByType(KtNodeTypes.PATTERN)

    val typeReference: KtTypeReference?
        get() = findChildByType(KtNodeTypes.TYPE_REFERENCE)

    val isPatternExpression: Boolean
        get() = pattern != null

    val isNegated: Boolean
        get() = operationReference.getReferencedNameElementType() === KtTokens.NOT_IS

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitIsExpression(this, data)

    override fun getOperationReference(): KtSimpleNameExpression {
        return findChildByType(KtNodeTypes.OPERATION_REFERENCE)!!
    }
}