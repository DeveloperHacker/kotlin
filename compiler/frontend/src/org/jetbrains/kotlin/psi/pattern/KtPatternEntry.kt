/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorAndReplaceIfNull

class KtPatternEntry(node: ASTNode) : KtPatternElementImpl(node) {

    private val simpleConstraint: KtPatternConstraint?
        get() = findChildByType(KtNodeTypes.PATTERN_CONSTRAINT)

    val declaration: KtPatternVariableDeclaration?
        get() = findChildByType(KtNodeTypes.PATTERN_VARIABLE_DECLARATION)

    val constraint: KtPatternConstraint?
        get() = declaration?.constraint ?: simpleConstraint

    val expression: KtPatternExpression?
        get() = constraint?.expression

    private val constraintTypeReference: KtTypeReference?
        get() = constraint?.typeReference?.typeReference

    val typedDeconstruction: KtPatternTypedDeconstruction?
        get() = constraint?.typedDeconstruction

    val isTail: Boolean
        get() = declaration?.isTail ?: simpleConstraint?.isTail ?: false

    val element: KtPatternElement?
        get() = findChildByClass(KtPatternElement::class.java)

    fun name() = findChildByType<PsiElement>(KtTokens.IDENTIFIER)

    fun hasName() = name() != null

    fun hasTypeReference(context: BindingContext) = getTypeReference(context) != null

    private fun hasDeconstructionDynamicLimits(context: BindingContext) = typedDeconstruction?.hasDynamicLimits(context) ?: false

    private fun isSimpleDeconstruction(context: BindingContext) = typedDeconstruction?.isSimple(context) ?: true

    private fun hasConstraintTypeReference() = constraintTypeReference != null

    private fun hasExpression() = expression != null

    fun getTypeReference(context: BindingContext) =
        constraintTypeReference ?: typedDeconstruction?.getTypeReference(context)

    fun isSimple(context: BindingContext): Boolean =
        !hasExpression() && isSimpleDeconstruction(context)

    fun hasDynamicLimits(context: BindingContext): Boolean =
        hasExpression() || hasConstraintTypeReference() || hasDeconstructionDynamicLimits(context)

    fun isEmptyDeclaration() = declaration?.isEmpty ?: false

    fun isNotEmptyDeclaration() = !isEmptyDeclaration()

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternEntry(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_ENTRY_ELEMENT
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        element?.getTypeInfo(resolver, state).errorAndReplaceIfNull(this, state, error, patch)
    }
}