/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPureElement

abstract class Generator(project: Project) {

    protected val psiFactory = KtPsiFactory(project)

    protected fun <T : KtPureElement> create(initializer: KtPsiFactory.() -> T): T {
        val element = psiFactory.initializer()
        element.containingKtFile.beforeAstChange()
        return element
    }

    fun createFile(name: String, text: String) = create { createFile(name, text) }

    abstract fun generate(name: String): KtFile
}