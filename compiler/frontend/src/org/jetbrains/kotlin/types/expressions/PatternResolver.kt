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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.pattern.KtPattern
import org.jetbrains.kotlin.psi.pattern.KtPatternElement
import org.jetbrains.kotlin.psi.pattern.KtPatternTypedTuple
import org.jetbrains.kotlin.psi.pattern.KtPatternVariableDeclaration
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class PatternResolver(
        val builtIns: KotlinBuiltIns,
        private val fakeCallResolver: FakeCallResolver,
        private val typeResolver: TypeResolver,
        private val localVariableResolver: LocalVariableResolver,
        private val overloadChecker: OverloadChecker,
        private val facade: ExpressionTypingInternals
) {
    companion object {
        fun getDeconstructName() = Name.identifier("deconstruct")
        fun getComponentName(index: Int) = DataClassDescriptorResolver.createComponentName(index + 1)
    }

    fun resolve(context: ExpressionTypingContext, pattern: KtPattern, expectedType: KotlinType, allowDefinition: Boolean): Pair<KotlinTypeInfo, LexicalScope> {
        val redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, overloadChecker)
        val scope = PatternScope(context.scope, redeclarationChecker)
        val newContext = context.replaceExpectedType(expectedType)
        val state = PatternResolveState(scope, newContext, allowDefinition, redeclarationChecker)
        val typeInfo = pattern.resolve(this, state)
        state.scope.printStructure(Printer(System.out))
        return typeInfo to scope.flatten()
    }

    fun getComponentsTypeInfoReceiver(tuple: KtPatternTypedTuple, state: PatternResolveState): KotlinType? {
        val receiver = TransientReceiver(state.expectedType)
        val results = fakeCallResolver.resolveFakeCall(
                context = state.context,
                receiver = receiver,
                name = getDeconstructName(),
                callElement = tuple,
                reportErrorsOn = tuple,
                callKind = FakeCallKind.OTHER,
                valueArguments = emptyList()
        )
        return if (results.isSuccess) results.resultingDescriptor.returnType else null
    }

    fun getComponentsTypeInfo(tuple: KtPatternTypedTuple, state: PatternResolveState, length: Int): List<KotlinTypeInfo> {
        val receiver = TransientReceiver(state.expectedType)
        return (0 until length).map {
            getComponentName(it)
        }.map { componentName ->
            val results = fakeCallResolver.resolveFakeCall(
                    context = state.context,
                    receiver = receiver,
                    name = componentName,
                    callElement = tuple,
                    reportErrorsOn = tuple,
                    callKind = FakeCallKind.COMPONENT,
                    valueArguments = emptyList()
            )
            if (results.isSuccess) {
                results.resultingDescriptor.returnType
            }
            else {
                ErrorUtils.createErrorType("$componentName() return type")
            }
        }.map {
            KotlinTypeInfo(it, DataFlowInfo.EMPTY)
        }
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): KotlinTypeInfo {
        val context = state.context
        val typeResolutionContext = TypeResolutionContext(context.scope, context.trace, true, true, context.isDebuggerContext)
        val possiblyBareTarget = typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReference)
        return KotlinTypeInfo(possiblyBareTarget.actualType, context.dataFlowInfo)
    }

    fun getTypeInfo(expression: KtExpression, state: PatternResolveState): KotlinTypeInfo {
        return facade.getTypeInfo(expression, state.context)
    }

    private fun KotlinTypeInfo?.errorIfNull(reportErrorOn: KtElement, state: PatternResolveState): KotlinTypeInfo {
        val error = Errors.UNSPECIFIED_TYPE.on(reportErrorOn, reportErrorOn)
        val emptyInfo = KotlinTypeInfo(null, DataFlowInfo.EMPTY)
        if (this == null) state.context.trace.report(error)
        return this ?: emptyInfo
    }

    private fun KotlinTypeInfo.replaceIfNull(subjectType: KotlinType): KotlinTypeInfo {
        if (type == null)
            return KotlinTypeInfo(subjectType, dataFlowInfo)
        return this
    }

    fun restoreOrCreate(element: KtPatternElement, state: PatternResolveState, creator: PatternResolver.() -> KotlinTypeInfo?): KotlinTypeInfo {
        val cachedTypeInfo = state.context.trace.bindingContext.get(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element)
        cachedTypeInfo?.let { return it }
        val typeInfo = this.creator().errorIfNull(element, state)
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, typeInfo)
        return typeInfo
    }

    fun resolveType(element: KtPatternElement, state: PatternResolveState): KotlinTypeInfo {
        val info = element.getTypeInfo(this, state).errorIfNull(element, state)
        val notNullInfo = info.replaceIfNull(state.expectedType)
        PatternMatchingTypingVisitor.checkTypeCompatibility(state.context, notNullInfo.type!!, state.expectedType, element)
        return notNullInfo
    }

    fun checkExpression(expression: KtExpression?, state: PatternResolveState): DataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        return visitor.checkCondition(expression, context)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState) {
        val trace = state.context.trace
        if (!state.allowDefinition) {
            trace.report(Errors.NOT_ALLOW_PROPERTY_DEFINITION.on(declaration, declaration))
            return
        }
        val scope = state.scope
        val componentType = declaration.getTypeInfo(this, state.expectedType).type
        val variableDescriptor = localVariableResolver.resolveLocalVariableDescriptorWithType(scope, declaration, componentType, trace)
        ExpressionTypingUtils.checkVariableShadowing(scope.flatten(), trace, variableDescriptor)
        scope.add(variableDescriptor)
    }
}

fun KotlinTypeInfo.and(vararg children: KotlinTypeInfo?): KotlinTypeInfo {
    return this.and(children.asSequence())
}

fun KotlinTypeInfo.and(children: Iterable<KotlinTypeInfo?>): KotlinTypeInfo {
    return this.and(children.asSequence())
}

fun KotlinTypeInfo.and(children: Sequence<KotlinTypeInfo?>): KotlinTypeInfo {
    val dataFlowInfo = children.map { it?.dataFlowInfo }
    return KotlinTypeInfo(type, this.dataFlowInfo.and(dataFlowInfo))
}

fun DataFlowInfo.and(vararg dataFlowInfo: DataFlowInfo?): DataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun DataFlowInfo.and(dataFlowInfo: Iterable<DataFlowInfo?>): DataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun DataFlowInfo.and(dataFlowInfo: Sequence<DataFlowInfo?>): DataFlowInfo {
    return (sequenceOf(this) + dataFlowInfo).filterNotNull().reduce { info, it -> info.and(it) }
}

class PatternScope private constructor(
        private val outer: LexicalScope,
        private val owner: PatternScope?,
        redeclarationChecker: TraceBasedLocalRedeclarationChecker
) : LexicalWritableScope(
        outer,
        outer.ownerDescriptor,
        outer.isOwnerDescriptorAccessibleByLabel,
        redeclarationChecker,
        outer.kind
) {
    constructor(outerScope: LexicalScope, redeclarationChecker: TraceBasedLocalRedeclarationChecker) : this(outerScope, null, redeclarationChecker)
    constructor(parent: PatternScope, redeclarationChecker: TraceBasedLocalRedeclarationChecker) : this(parent.outer, parent, redeclarationChecker)

    private val children = mutableListOf<PatternScope>()

    fun add(descriptor: VariableDescriptor) {
        addVariableDescriptor(descriptor)
        owner?.add(descriptor)
    }

    fun child(redeclarationChecker: TraceBasedLocalRedeclarationChecker): PatternScope {
        val child = PatternScope(this, redeclarationChecker)
        children.add(child)
        return child
    }

    fun flatten(): LexicalScope {
        var scope = this
        while (scope.owner != null) {
            scope = scope.owner as PatternScope
        }
        return scope
    }

    override fun printStructure(p: Printer) {
        p.println("{")
        p.pushIndent()
        addedDescriptors.forEach {
            p.println(it.toString())
        }
        children.forEach { it.printStructure(p) }
        p.popIndent()
        p.println("}")
    }
}

class PatternResolveState(
        val scope: PatternScope,
        val context: ExpressionTypingContext,
        val allowDefinition: Boolean,
        private val redeclarationChecker: TraceBasedLocalRedeclarationChecker
) {
    val expectedType
        get() = context.expectedType

    fun replaceScope(scope: PatternScope): PatternResolveState {
        val context = context.replaceScope(scope)
        return PatternResolveState(scope, context, allowDefinition, redeclarationChecker)
    }

    fun replaceType(expectedType: KotlinType): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        return PatternResolveState(scope, context, allowDefinition, redeclarationChecker)
    }

    fun next(expectedType: KotlinType): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        return PatternResolveState(scope.child(redeclarationChecker), context, allowDefinition, redeclarationChecker)
    }
}
