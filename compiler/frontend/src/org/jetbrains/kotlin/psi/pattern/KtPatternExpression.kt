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
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeIntersector
import org.jetbrains.kotlin.types.expressions.*

class KtPatternExpression(node: ASTNode) : KtPatternEntry(node) {

    val constraints: List<KtPatternConstraint>
        get() = findChildrenByType(KtNodeTypes.PATTERN_CONSTRAINT)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPatternExpression(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val flowsInfo = constraints.map { it.getTypeInfo(resolver, state) }
        val type = flowsInfo.map { it.type }
                .let { TypeIntersector.intersectTypes(it) }
                .errorAndReplaceIfNull(this, state, Errors.NON_DERIVABLE_TYPE, ErrorUtils.createErrorType("$this type"))
        val dataFlowInfo = flowsInfo.asSequence()
                .map { it.dataFlowInfo }
                .reduce { acc, info -> acc.and(info) }
        KotlinTypeInfo(type, dataFlowInfo)
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): KotlinTypeInfo {
        val constraintsTypeInfo = constraints.asSequence().map { it.resolve(resolver, state) }
        val thisTypeInfo = resolver.resolveType(this, state)
        return thisTypeInfo.and(constraintsTypeInfo)
    }
}
