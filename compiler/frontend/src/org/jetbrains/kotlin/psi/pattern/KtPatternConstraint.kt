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
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.reportAndReplaceIfNull

class KtPatternConstraint(node: ASTNode) : KtPatternElementImpl(node) {

    val typeReference: KtPatternTypeReference?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_REFERENCE)

    val typedDeconstruction: KtPatternTypedDeconstruction?
        get() = findChildByType(KtNodeTypes.PATTERN_DECONSTRUCTION)

    val expression: KtPatternExpression?
        get() = findChildByType(KtNodeTypes.PATTERN_EXPRESSION)

    val element: KtPatternElement?
        get() = findChildByClass(KtPatternElement::class.java)

    val isTail: Boolean
        get() = findChildByType<PsiElement?>(KtTokens.MUL) != null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternConstraint(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_CONSTRAINT_ELEMENT
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        element?.getTypeInfo(resolver, state).reportAndReplaceIfNull(this, state, error, patch)
    }
}
