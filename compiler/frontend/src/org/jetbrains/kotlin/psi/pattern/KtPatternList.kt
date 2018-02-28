/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.Subject

class KtPatternList(node: ASTNode) : KtPatternElementImpl(node), KtPatternDeconstruction {

    override val entries: List<KtPatternEntry>
        get() = findChildrenByType(KtNodeTypes.PATTERN_ENTRY)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitPatternList(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val info = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val (iteratorType, elementType) = resolver.checkIterableConvention(this, state)
        val elementsInfo = entries.map {
            val type = if (it.isAsterisk) iteratorType else elementType
            val receiverValue = TransientReceiver(type)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, state.context)
            val subject = Subject(it, receiverValue, dataFlowValue)
            it.getTypeInfo(resolver, state.replaceSubject(subject))
        }
        (sequenceOf(info) + elementsInfo).reduce({ acc, it -> acc.and(it) })
    }
}