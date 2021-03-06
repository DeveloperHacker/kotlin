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
import org.jetbrains.kotlin.psi.KtPostProcessableElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.psiUtil.transferDiagnostics
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.reportAndReplaceIfNull

class KtPatternTypeCallExpression(node: ASTNode) : KtPatternElementImpl(node), KtPostProcessableElement {

    private fun resolvePsiElement(element: PsiElement, state: PatternResolveState) {
        state.context.trace.record(BindingContext.RESOLVED_PSI_ELEMENT, this, element)
        state.context.trace.record(BindingContext.POST_PROCESSABLE_ELEMENT, this)
    }

    val instance: PsiElement?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_CALL_INSTANCE)

    override fun postProcess(trace: BindingTrace) {
        val resolvedElement = trace.get(BindingContext.RESOLVED_PSI_ELEMENT, this) ?: return
        val diagnostics = trace.bindingContext.diagnostics as MutableDiagnosticsWithSuppression
        instance?.let { transferDiagnostics(diagnostics, resolvedElement, it) }
    }

    fun getTypeReference(context: BindingContext) = context.get(BindingContext.RESOLVED_PSI_ELEMENT, this) as? KtTypeReference

    fun getCallExpression(context: BindingContext) = context.get(BindingContext.RESOLVED_PSI_ELEMENT, this) as? KtCallExpression

    fun hasNullableCheck(context: BindingContext) = context.get(BindingContext.NEEDED_NULL_CHECK, this) ?: false

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternTypeCallExpression(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_TYPE_CALL_EXPRESSION_INSTANCE
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val instance = instance
        val callExpression = resolver.createOrNull(instance?.text) { createExpression("$it()") as? KtCallExpression }
        val typeReference = resolver.createOrNull(instance?.text) { createType(it) }
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
        info.reportAndReplaceIfNull(this, state, error, patch)
    }
}
