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
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.Subject

class KtPatternTuple(node: ASTNode) : KtPatternElementImpl(node), KtPatternDeconstruction {

    override val entries: List<KtPatternEntry>
        get() = findChildrenByType(KtNodeTypes.PATTERN_ENTRY)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternTuple(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val info = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val entries = entries
        var hasNonSingleUnderscoreEntries = false
        val componentInfo = entries.mapIndexed { i, entry ->
            hasNonSingleUnderscoreEntries = hasNonSingleUnderscoreEntries || entry.isNotEmptyDeclaration()
            val name = entry.name()
            val type = if (name != null) {
                val errorType = lazy { ErrorUtils.createErrorType("${name.text} return type") }
                resolver.getPropertyType(entry, name.text, state) ?: errorType.value
            } else {
                val componentName = PatternResolver.getComponentName(i)
                val errorType = lazy { ErrorUtils.createErrorType("$componentName() return type") }
                resolver.getComponentType(componentName, entry, state) ?: errorType.value
            }
            val receiverValue = TransientReceiver(type)
            val dataFlowValue = resolver.dataFlowValueFactory.createDataFlowValue(receiverValue, state.context)
            val subject = Subject(entry, receiverValue, dataFlowValue)
            entry.getTypeInfo(resolver, state.replaceSubject(subject))
        }
        if (!hasNonSingleUnderscoreEntries && !entries.isEmpty())
            state.context.trace.report(Errors.USELESS_TUPLE_DECONSTRUCTION.on(this, this))
        (sequenceOf(info) + componentInfo).reduce({ acc, it -> acc.and(it) })
    }
}