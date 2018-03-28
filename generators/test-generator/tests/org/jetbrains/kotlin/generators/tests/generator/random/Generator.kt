/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPureElement

abstract class Generator(val parent: Generator?) {

    constructor() : this(null)

    var psiFactory: KtPsiFactory? = null

    fun setProject(project: Project) {
        psiFactory = KtPsiFactory(project)
        parent?.setProject(project)
    }

    protected fun <T : KtPureElement> create(initializer: KtPsiFactory.() -> T): T {
        val element = psiFactory!!.initializer()
        element.containingKtFile.beforeAstChange()
        return element
    }

    protected fun createNewLine() = psiFactory!!.createNewLine(1)

    abstract fun generate(name: String): KtFile
}