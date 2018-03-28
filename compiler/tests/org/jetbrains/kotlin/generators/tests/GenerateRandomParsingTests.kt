/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.parsing.AbstractParsingTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {
        testClass<AbstractParsingTest> {
            random("psi/random")
            model("psi", testMethod = "doParsingTest", pattern = "^(.*)\\.kts?$")
        }
    }
}
