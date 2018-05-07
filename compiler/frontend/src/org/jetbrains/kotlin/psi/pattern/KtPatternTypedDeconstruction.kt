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
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.expressions.*

class KtPatternTypedDeconstruction(node: ASTNode) : KtPatternElementImpl(node) {

    val typeCallExpression: KtPatternTypeCallExpression?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_CALL_EXPRESSION)

    val tuple: KtPatternTuple?
        get() = findChildByType(KtNodeTypes.PATTERN_TUPLE)

    val list: KtPatternList?
        get() = findChildByType(KtNodeTypes.PATTERN_LIST)

    val deconstruction: KtPatternDeconstruction?
        get() = tuple ?: list

    private fun getCallExpression(context: BindingContext) = typeCallExpression?.getCallExpression(context)

    fun onlyTypeRestrictions(context: BindingContext): Boolean =
        getCallExpression(context) == null && list == null && tuple?.entries?.all { it.onlyTypeRestrictions(context) } ?: true

    fun isSimple(context: BindingContext): Boolean =
        getCallExpression(context) == null && list == null && tuple?.entries?.all { it.isSimple(context) && it.getTypeReference(context) == null } ?: true

    fun isRestrictionsFree(context: BindingContext): Boolean =
        typeCallExpression == null && list == null && tuple?.entries?.all { it.isRestrictionsFree(context) } ?: true

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternTypedTuple(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val typeCallExpression = typeCallExpression
        val emptyInfo = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val info = typeCallExpression?.getTypeInfo(resolver, state.setIsTuple()) ?: emptyInfo
        val isCallExpression = getCallExpression(state.context.trace.bindingContext) != null
        val receiverValue = TransientReceiver(info.type)
        val dataFlowValue = resolver.dataFlowValueFactory.createDataFlowValue(receiverValue, state.context)
        val subject = Subject(this, receiverValue, dataFlowValue)
        val deconstructionState = (if (isCallExpression) state.replaceSubject(subject) else state).replaceDataFlow(info.thenInfo)
        val error = Errors.EXPECTED_PATTERN_TYPED_DECONSTRUCTION_INSTANCE
        val patch = ConditionalTypeInfo.empty(deconstructionState.subject.type, deconstructionState.dataFlowInfo)
        val deconstructionInfo = deconstruction?.getTypeInfo(resolver, deconstructionState).errorAndReplaceIfNull(this, deconstructionState, error, patch)
        info.and(deconstructionInfo)
    }
}