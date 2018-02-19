/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorAndReplaceIfNull

class KtPatternTypeCallExpression(node: ASTNode) : KtPatternElementImpl(node) {

    // To workaround abstract PSI getter test touching us
    @get:JvmName("asCallExpression")
    val asCallExpression: KtCallExpression?
        get() = callExpression

    private val typeReference: KtTypeReference?
        get() = findChildByType(KtNodeTypes.TYPE_REFERENCE)

    private val callExpression: KtCallExpression?
        get() = run {
            val isNotPossibleCallExpression = typeReference?.isMarkedNullable ?: return null
            if (isNotPossibleCallExpression) return null
            val psiFactory = KtPsiFactory(project, markGenerated = false)
            psiFactory.createExpression(text + "()") as? KtCallExpression
        }

    fun isTypeReference(context: BindingContext) = !(context.get(BindingContext.IS_PATTERN_CALL_EXPRESSION, this) ?: false)

    fun isCallExpression(context: BindingContext) = context.get(BindingContext.IS_PATTERN_CALL_EXPRESSION, this) ?: false

    fun getTypeReference(context: BindingContext) = if (isTypeReference(context)) typeReference else null

    fun getCallExpression(context: BindingContext) = if (isCallExpression(context)) callExpression else null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternTypeCallExpression(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_TYPE_REFERENCE_INSTANCE
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val info = resolver.getDeconstructType(this, state)?.let {
            ConditionalTypeInfo.empty(it, state.dataFlowInfo)
        } ?: getTypeReference(state.context.trace.bindingContext)?.let {
            resolver.getTypeInfo(it, state)
        }
        info.errorAndReplaceIfNull(this, state, error, patch)
    }
}
