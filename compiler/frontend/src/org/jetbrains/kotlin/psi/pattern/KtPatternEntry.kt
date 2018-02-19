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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Errors
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

    private val declaration: KtPatternVariableDeclaration?
        get() = findChildByType(KtNodeTypes.PATTERN_VARIABLE_DECLARATION)

    private val constraint: KtPatternConstraint?
        get() = declaration?.constraint ?: simpleConstraint

    private val expression: KtPatternExpression?
        get() = constraint?.expression

    private val constraintTypeReference: KtPatternTypeReference?
        get() = constraint?.typeReference

    private val typedTuple: KtPatternTypedTuple?
        get() = constraint?.typedTuple

    fun getTypeReference(context: BindingContext): KtTypeReference? =
        constraintTypeReference?.typeReference ?: typedTuple?.typeCallExpression?.getTypeReference(context)

    fun onlyTypeRestrictions(context: BindingContext): Boolean =
        expression == null && typedTuple?.onlyTypeRestrictions(context) ?: true

    fun isSimple(context: BindingContext): Boolean =
        expression == null && typedTuple?.isSimple(context) ?: true

    fun isRestrictionsFree(context: BindingContext): Boolean =
        expression == null && constraintTypeReference == null && typedTuple?.isRestrictionsFree(context) ?: true

    private val isEmptyDeclaration: Boolean
        get() = declaration?.isEmpty ?: false

    val isNotEmptyDeclaration: Boolean
        get() = !isEmptyDeclaration

    val element: KtPatternElement?
        get() = findChildByClass(KtPatternElement::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternEntry(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_ENTRY_ELEMENT
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        element?.getTypeInfo(resolver, state).errorAndReplaceIfNull(this, state, error, patch)
    }
}