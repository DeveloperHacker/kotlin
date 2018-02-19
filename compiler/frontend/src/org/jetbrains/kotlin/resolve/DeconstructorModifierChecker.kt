/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

object DeconstructorModifierChecker {
    fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, trace: BindingTrace) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isDeconstructor) return
        val modifier = declaration.modifierList?.getModifier(KtTokens.DECONSTRUCTOR_KEYWORD) ?: return
        val targetList = AnnotationChecker.getDeclarationSiteActualTargetList(declaration, descriptor, trace)
        if (!functionDescriptor.isExtension && !targetList.contains(KotlinTarget.MEMBER_FUNCTION)) {
            val containingDeclaration = descriptor.containingDeclaration
            val containingDeclarationName = containingDeclaration.fqNameUnsafe.asString()
            trace.report(Errors.INAPPLICABLE_DECONSTRUCTOR_MODIFIER.on(modifier, containingDeclarationName))
        }
        return
    }
}