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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorAndReplaceIfNull

class KtPatternGuard(node: ASTNode) : KtPatternElementImpl(node) {

    val expression: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternGuard(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_GUARD_INSTANCE
        val patch = ConditionalDataFlowInfo(state.dataFlowInfo, DataFlowInfo.EMPTY)
        val dataFlow = expression?.let { resolver.checkCondition(it, state) }.errorAndReplaceIfNull(this, state, error, patch)
        ConditionalTypeInfo(resolver.builtIns.booleanType, dataFlow)
    }
}