/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver

class KtPatternFunctionCall(node: ASTNode) : KtPatternElementImpl(node) {

    val callExpression: KtCallExpression?
        get() = findChildByType(KtNodeTypes.CALL_EXPRESSION)

    private fun <T> throwVisitError(): T = throw IllegalStateException("it is temporal ast node, ")

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = throwVisitError()

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) =
        resolver.restoreOrCreate(this, state, this::throwVisitError)
}
