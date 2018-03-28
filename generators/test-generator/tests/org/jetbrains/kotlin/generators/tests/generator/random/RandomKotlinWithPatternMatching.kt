/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.pattern.KtPattern
import org.jetbrains.kotlin.psi.psiUtil.findByClass


class RandomKotlinWithPatternMatching : Generator(RandomKotlin()) {

    private val maxFileRollingSteps = 1000

    override fun generate(name: String): KtFile {
        for (i in 1..maxFileRollingSteps) {
            val file = parent!!.generate(name)
            val pattern = file.findByClass(KtPattern::class.java)
            if (pattern != null) return file
        }
        System.err.println("e: pattern rolling timeout!!!")
        return parent!!.generate(name)
    }
}