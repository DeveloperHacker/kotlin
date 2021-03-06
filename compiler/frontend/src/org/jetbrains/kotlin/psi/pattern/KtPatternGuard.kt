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
import org.jetbrains.kotlin.parsing.GUARD_PREFIX
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.*

class KtPatternGuard(node: ASTNode) : KtPatternElementImpl(node) {

    val expression: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternGuard(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_GUARD_INSTANCE
        val warning = Errors.EXPECTED_PARENTHESISE_GUARD
        val patch = ConditionalTypeInfo.empty(resolver.builtIns.booleanType, state.dataFlowInfo)
        expression?.let {
            if (it.precedence > GUARD_PREFIX.precedence) {
                state.context.trace.report(warning.on(it))
            }
            resolver.checkCondition(it, state)
        }.reportAndReplaceIfNull(this, state, error, patch)
    }
}