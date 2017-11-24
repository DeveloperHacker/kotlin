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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.types.expressions.NotNullKotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.and

class KtPattern(node: ASTNode) : KtPatternElement(node) {

    val innerNotPatternExpressions: List<KtExpression>
        get() = collectDescendantsOfType({ it is KtPatternElement }, { it !is KtPatternElement })

    val innerVariableDeclarations: List<KtPatternVariableDeclaration>
        get() = collectDescendantsOfType({ it is KtPatternElement }, { true })

    val expression: KtPatternExpression?
        get() = findChildByType(KtNodeTypes.PATTERN_EXPRESSION)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPattern(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        expression?.getTypeInfo(resolver, state)
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        val expressionTypeInfo = expression?.resolve(resolver, state)
        val thisTypeInfo = resolver.resolveType(this, state)
        return thisTypeInfo.and(expressionTypeInfo)
    }
}