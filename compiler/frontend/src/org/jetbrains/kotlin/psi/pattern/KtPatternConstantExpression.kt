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
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.NotNullKotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver

class KtPatternConstantExpression(node: ASTNode) : KtPatternEntry(node) {

    val constant: KtConstantExpression?
        get() = findChildByClass(KtConstantExpression::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPatternConstantExpression(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        constant?.let { resolver.getTypeInfo(it, state) }
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        constant?.let { resolver.resolveType(it, state) }
        return resolver.resolveType(this, state)
    }
}