/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
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

    private fun <T> createOrNull(text: String?, creator: KtPsiFactory.(String) -> T) =
        try {
            text?.let { KtPsiFactory(project).creator(it) }
        } catch (ex: Exception) {
            null
        } catch (ex: AssertionError) {
            null
        }

    private fun resolvePsiElement(element: PsiElement, state: PatternResolveState) {
        state.context.trace.record(BindingContext.RESOLVED_PSI_ELEMENT, this, element)
//        add(element)
    }

    fun getTypeReference(context: BindingContext) = context.get(BindingContext.RESOLVED_PSI_ELEMENT, this) as? KtTypeReference

    fun getCallExpression(context: BindingContext) = context.get(BindingContext.RESOLVED_PSI_ELEMENT, this) as? KtCallExpression

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternTypeCallExpression(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_TYPE_CALL_EXPRESSION_INSTANCE
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val instance = findChildByType<PsiElement?>(KtNodeTypes.PATTERN_TYPE_CALL_EXPRESSION)
        val callExpression = createOrNull(instance?.text) { createExpression("$it()") as? KtCallExpression }
        val typeReference = createOrNull(instance?.text) { createType(it) }
        val callInfo = callExpression
            ?.let { resolver.getDeconstructType(this, it, state) }
            ?.let { ConditionalTypeInfo.empty(it, state.dataFlowInfo) }
            ?.also { resolvePsiElement(callExpression, state) }
        val info = callInfo ?: typeReference
            ?.let { resolver.getTypeInfo(it, state) }
            ?.also { resolvePsiElement(typeReference, state) }
        info ?: instance?.let {
            state.context.trace.report(Errors.UNRESOLVED_PATTERN_TYPE_CALL_EXPRESSION.on(this, it))
            resolvePsiElement(it, state)
        }
        info.errorAndReplaceIfNull(this, state, error, patch)
    }
}
