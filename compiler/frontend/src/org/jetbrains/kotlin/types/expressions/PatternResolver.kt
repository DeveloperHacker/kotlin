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

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.pattern.KtPattern
import org.jetbrains.kotlin.psi.pattern.KtPatternElement
import org.jetbrains.kotlin.psi.pattern.KtPatternVariableDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable


infix fun <F, S, T> Pair<F, S>.to(triple: T) = Triple(first, second, triple)

class ConditionalTypeInfo(val type: KotlinType, val dataFlowInfo: ConditionalDataFlowInfo) {
    fun and(other: ConditionalTypeInfo?) = other?.let {
        ConditionalTypeInfo(type, dataFlowInfo.and(it.dataFlowInfo))
    } ?: this

    fun replaceThenInfo(thenInfo: DataFlowInfo) = ConditionalTypeInfo(type, dataFlowInfo.replaceThenInfo(thenInfo))

    val thenInfo: DataFlowInfo
        get() = dataFlowInfo.thenInfo

    companion object {
        fun empty(type: KotlinType, thenInfo: DataFlowInfo) =
            ConditionalTypeInfo(type, ConditionalDataFlowInfo(thenInfo, DataFlowInfo.EMPTY))
    }
}

class PatternResolver(
    private val psiFactory: KtPsiFactory,
    private val patternMatchingTypingVisitor: PatternMatchingTypingVisitor,
    private val components: ExpressionTypingComponents,
    private val facade: ExpressionTypingInternals
) {
    val builtIns: KotlinBuiltIns
        get() = components.builtIns!!

    val dataFlowValueFactory: DataFlowValueFactory
        get() = facade.components.dataFlowValueFactory!!

    companion object {
        fun getComponentName(index: Int) = DataClassDescriptorResolver.createComponentName(index + 1)
    }

    fun resolve(context: ExpressionTypingContext, pattern: KtPattern, subject: Subject, allowDefinition: Boolean, isNegated: Boolean) =
        run {
            val scope = ExpressionTypingUtils.newWritableScopeImpl(context, LexicalScopeKind.MATCH_EXPRESSION, components.overloadChecker)
            val state = PatternResolveState(scope, context, allowDefinition, isNegated, false, subject)
            val typeInfo = pattern.getTypeInfo(this, state)
            typeInfo to scope
        }

    fun checkIterableConvention(reportOn: KtExpression, state: PatternResolveState): Pair<KotlinType, KotlinType> {
        val subjectExpression = state.subject.expression
        val receiverValue = state.subject.receiverValue
        val context = state.context
        val (iteratorType, elementType) = components.forLoopConventionsChecker.checkIterableConventionWithFullResult(
            receiverValue,
            subjectExpression,
            reportOn,
            context
        )
        val validatedIteratorType = iteratorType ?: ErrorUtils.createErrorType("${subjectExpression.text}.iterator() return type")
        val validatedElementType = elementType ?: ErrorUtils.createErrorType("${subjectExpression.text}.iterator().next() return type")
        return Pair(validatedIteratorType, validatedElementType)
    }

    fun getDeconstructType(reportOn: KtElement, callExpression: KtCallExpression, state: PatternResolveState): KotlinType? {
        val receiver = state.subject.receiverValue
        val results = repairAfterInvoke(state) {
            components.fakeCallResolver.resolveFakeFunctionCall(state.context, receiver, callExpression)
        }
        if (!results.isSuccess) return null
        val resolvedCall = results.resultingCall
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor is ClassConstructorDescriptor) return null
        val containingDeclarationName = descriptor.containingDeclaration.fqNameUnsafe.asString()
        if (!descriptor.isDeconstructor) {
            state.context.trace.report(Errors.DECONSTRUCTOR_MODIFIER_REQUIRED.on(reportOn, descriptor, containingDeclarationName))
        }
        val type = results.resultingDescriptor.returnType
        state.context.trace.record(BindingContext.NEEDED_NULL_CHECK, reportOn, type?.isMarkedNullable ?: false)
        state.context.trace.record(BindingContext.CALL, reportOn, resolvedCall.call)
        state.context.trace.record(BindingContext.RESOLVED_CALL, resolvedCall.call, resolvedCall)
        return type?.makeNotNullable()
    }

    fun getComponentType(componentName: Name, reportOn: KtExpression, state: PatternResolveState): KotlinType? {
        val receiver = state.subject.receiverValue
        val expression = state.subject.expression
        val results = repairAfterInvoke(state) {
            components.fakeCallResolver.resolveFakeCall(
                context = state.context,
                receiver = receiver,
                name = componentName,
                callElement = expression,
                reportErrorsOn = reportOn,
                callKind = FakeCallKind.COMPONENT,
                valueArguments = emptyList()
            )
        }
        if (!results.isSuccess) return null
        val resultType = results.resultingDescriptor.returnType ?: return null
        val resolvedCall = results.resultingCall
        state.context.trace.record(BindingContext.CALL, reportOn, resolvedCall.call)
        state.context.trace.record(BindingContext.RESOLVED_CALL, resolvedCall.call, resolvedCall)
        return resultType
    }

    fun getPropertyType(reportOn: KtElement, name: String, state: PatternResolveState): KotlinType? {
        val receiver = state.subject.receiverValue
        val expression = psiFactory.createSimpleName(name)
        val results = repairAfterInvoke(state) {
            components.fakeCallResolver.resolveFakePropertyCall(state.context, receiver, expression)
        }
        if (!results.isSuccess) return null
        val resultType = results.resultingDescriptor.returnType ?: return null
        val resolvedCall = results.resultingCall
        state.context.trace.record(BindingContext.ELEMENT_NAME_EXPRESSION, reportOn, expression)
        state.context.trace.record(BindingContext.CALL, expression, resolvedCall.call)
        state.context.trace.record(BindingContext.RESOLVED_CALL, resolvedCall.call, resolvedCall)
        return resultType
    }

    private fun <T> repairAfterInvoke(state: PatternResolveState, invokable: () -> T): T {
        val beforeSubjectType = state.context.trace.bindingContext.getType(state.subject.expression)
        val result = invokable()
        val subjectTypeInfo = state.context.trace.get(BindingContext.EXPRESSION_TYPE_INFO, state.subject.expression)
        if (subjectTypeInfo != null && beforeSubjectType != null) {
            val repairedSubjectTypeInfo = subjectTypeInfo.replaceType(beforeSubjectType)
            state.context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, state.subject.expression, repairedSubjectTypeInfo)
        }
        return result
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): ConditionalTypeInfo {
        val context = state.context
        val subjectType = state.subject.type
        val dataFlowValue = state.subject.dataFlowValue
        val isNegated = state.isNegated
        val isTuple = state.isTuple
        val (type, dataFlowInfo, useless) = patternMatchingTypingVisitor.checkTypeForIs(
            context,
            subjectType,
            typeReference,
            !isTuple,
            dataFlowValue
        )
        context.trace.record(BindingContext.USELESS_TYPE_CHECK, typeReference, useless)
        if (useless) {
            context.trace.report(Errors.USELESS_TYPE_CHECK.on(typeReference, !isNegated))
        }
        return ConditionalTypeInfo.empty(type, dataFlowInfo)
    }

    fun getTypeInfo(expression: KtExpression, state: PatternResolveState): ConditionalTypeInfo {
        val subjectType = state.context.trace.bindingContext.get(BindingContext.PATTERN_SUBJECT_TYPE, expression)
        if (subjectType == null) {
            state.context.trace.record(BindingContext.PATTERN_SUBJECT_TYPE, expression, state.subject.type)
        }
        val info = facade.getTypeInfo(expression, state.context)
        val type = info.type ?: ErrorUtils.createErrorType("${expression.text} return type")
        return ConditionalTypeInfo(type, ConditionalDataFlowInfo(info.dataFlowInfo, DataFlowInfo.EMPTY))
    }

    fun restoreOrCreate(element: KtPatternElement, state: PatternResolveState, creator: () -> ConditionalTypeInfo): ConditionalTypeInfo {
        state.context.trace.bindingContext.get(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element)?.let { return it }
        val info = creator()
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, info)
        return info
    }

    fun <T> createOrNull(text: String?, creator: KtPsiFactory.(String) -> T) =
        try {
            text?.let { psiFactory.creator(it) }
        } catch (ex: Exception) {
            null
        } catch (ex: AssertionError) {
            null
        }

    fun checkCondition(expression: KtExpression, state: PatternResolveState): ConditionalDataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        val conditionalInfo = visitor.checkCondition(expression, context)
        return ConditionalDataFlowInfo(conditionalInfo, context.dataFlowInfo)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState): DataFlowInfo {
        if (declaration.isSingleUnderscore) return state.dataFlowInfo
        val trace = state.context.trace
        if (!state.allowDefinition) {
            trace.report(Errors.NOT_ALLOW_PROPERTY_DEFINITION.on(declaration, declaration))
            return state.dataFlowInfo
        }
        val scope = state.scope
        val descriptor = components.localVariableResolver.resolveLocalVariableDescriptorWithType(scope, declaration, null, trace)
        descriptor.setOutType(state.subject.type)
        ExpressionTypingUtils.checkVariableShadowing(scope, trace, descriptor)
        scope.addVariableDescriptor(descriptor)
        val usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor)
        val variableDataFlowValue =
            dataFlowValueFactory.createDataFlowValue(declaration, descriptor, trace.bindingContext, usageModuleDescriptor)
        val subjectDataFlowValue = state.subject.dataFlowValue
        return state.dataFlowInfo.assign(variableDataFlowValue, subjectDataFlowValue, components.languageVersionSettings)
    }
}

fun <T, E : PsiElement> T?.errorAndReplaceIfNull(element: E, state: PatternResolveState, error: DiagnosticFactory0<E>, patch: T): T {
    if (this != null) return this
    state.context.trace.report(error.on(element))
    return patch
}

fun <T, E : PsiElement> T?.errorAndReplaceIfNull(element: E, state: PatternResolveState, error: DiagnosticFactory1<E, E>, patch: T): T {
    if (this != null) return this
    state.context.trace.report(error.on(element, element))
    return patch
}

data class Subject(val expression: KtExpression, val receiverValue: ReceiverValue, val dataFlowValue: DataFlowValue) {
    val type: KotlinType
        get() = receiverValue.type

    fun replaceType(type: KotlinType): Subject {
        val receiverValue = receiverValue.replaceType(type)
        return Subject(expression, receiverValue, dataFlowValue)
    }
}

class PatternResolveState private constructor(
    val scope: LexicalWritableScope,
    val context: ExpressionTypingContext,
    val allowDefinition: Boolean,
    val isNegated: Boolean,
    val isTuple: Boolean,
    val subject: Subject,
    private val usefulCheckCounter: Ref<Int>
) {
    constructor(
        scope: LexicalWritableScope,
        context: ExpressionTypingContext,
        allowDefinition: Boolean,
        isNegated: Boolean,
        isTuple: Boolean,
        subject: Subject
    ) : this(scope, context, allowDefinition, isNegated, isTuple, subject, Ref.create(0))

    val dataFlowInfo: DataFlowInfo
        get() = context.dataFlowInfo

    fun setIsTuple(): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, true, subject, usefulCheckCounter)
    }

    fun replaceDataFlow(dataFlowInfo: DataFlowInfo): PatternResolveState {
        val context = context.replaceDataFlowInfo(dataFlowInfo)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject, usefulCheckCounter)
    }

    fun replaceSubjectType(type: KotlinType): PatternResolveState {
        val subject = subject.replaceType(type)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject, usefulCheckCounter)
    }

    fun replaceSubject(subject: Subject): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject, usefulCheckCounter)
    }
}
