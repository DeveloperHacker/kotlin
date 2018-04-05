/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.pattern.KtPattern
import org.jetbrains.kotlin.psi.psiUtil.findByClass


class RandomKotlinWithPatternMatching(seed: Long, project: Project) : RandomKotlin(seed, project) {

    private val maxFileRollingSteps = 100

    override fun generate(name: String): KtFile {
        for (i in 1..maxFileRollingSteps) {
            val file = super.generate(name)
            val pattern = file.findByClass(KtPattern::class.java)
            if (pattern != null) return file
        }
        System.err.println("e: pattern rolling timeout!!!")
        return super.generate(name)
    }
}