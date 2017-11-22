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
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.NotNullKotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver

class KtPatternTuple(node: ASTNode) : KtPatternElement(node) {

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPatternTuple(this, data)

    val expressions: List<KtPatternExpression>
        get() = findChildrenByType(KtNodeTypes.PATTERN_EXPRESSION)


    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        throw UnsupportedOperationException("use KtPattenTypedTuple.getTypeInfo")
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        throw UnsupportedOperationException("use KtPattenTypedTuple.resolve")
    }
}