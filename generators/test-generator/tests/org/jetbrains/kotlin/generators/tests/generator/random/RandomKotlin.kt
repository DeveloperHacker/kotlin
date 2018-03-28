/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens.KEYWORDS
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.pattern.KtPattern
import org.jetbrains.kotlin.psi.pattern.KtPatternEntry
import org.jetbrains.kotlin.psi.pattern.KtPatternGuard
import org.jetbrains.kotlin.psi.psiUtil.collapseWhiteSpaces
import org.jetbrains.kotlin.psi.psiUtil.replace
import java.util.*


class RandomKotlin : Generator() {

    private val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val lower = upper.toLowerCase()
    private val digits = "0123456789"
    private val alpha = upper + lower + "_"
    private val alphanum = alpha + digits
    private val symbols = "!@#$%^&*()!\"№;%:?'-=+/|'\\ "
    private val fullAlpha = alphanum + symbols
    private val maxIdentifierLength = 10
    private val maxNumberStatementsInBlock = 100
    private val maxNumberConditionsInWhenBlock = 10
    private val maxNumberEntriesInWhenBlock = 10
    private val maxExpressionGenerationDepth = 10
    private val maxNumberExpressionGenerationSteps = 1000
    private val maxIdentifierRollingSteps = 100
    private val seed = 912301L
    private val random = Random(seed)

    private var expressionDepth = 0
    private var expressionSteps = 0

    override fun generate(name: String): KtFile {
        expressionDepth = 0
        expressionSteps = 0
        val file: KtFile = create { createFile(name, "") }
        file.add(generateNamedFunction())
        return psiFactory!!.collapseWhiteSpaces(file)
    }

    private fun generateNamedFunction(): KtNamedFunction {
        val functionName = generateIdentifier()
        val function = create { createFunction("fun $functionName() {}") }
        function.replace(function.bodyExpression!!, generateBlockExpression())
        return function
    }

    private fun generateBlockExpression(): KtBlockExpression {
        val block = create { createBlock("") }
        val numStatements = random.nextInt(maxNumberStatementsInBlock) + 1
        for (i in 0..numStatements) {
            block.addAfter(createNewLine(), block.lBrace!!)
            block.addAfter(generateStatement(), block.lBrace!!)
        }
        return block
    }

    private fun generateStatement() = when (random.nextInt(2)) {
        0 -> generateProperty()
        else -> generateExpression()
    }

    private fun generateProperty(): KtProperty {
        val property = create { createDeclaration("val test = 10") as KtProperty }
        property.initializer = generateExpression()
        return property
    }

    //ToDo(sergei)
    private fun generateExpression(): KtExpression {
        ++expressionDepth
        ++expressionSteps
        val isNotOver = expressionDepth < maxExpressionGenerationDepth && expressionSteps < maxNumberExpressionGenerationSteps
        val expression = if (isNotOver) {
            when (random.nextInt(12)) {
                0 -> generateSimpleNameExpression()
                1 -> generatePatternExpression()
                2 -> generateIsExpression()
//                3 -> generateBinaryExpression()
//                4 -> generatePrefixExpression()
//                5 -> generatePostfixExpression()
//                6 -> generateInfixExpression()
//                7 -> generateConstantExpression()
//                8 -> generateIfExpression()
//                9 -> generateCallExpression()
//                10 -> generateInfixExpression()
                11 -> generateWhenExpression()
                else -> generateParenthesizedExpression()
            }
        } else {
            generateSimpleNameExpression()
        }
        --expressionDepth
        return expression
    }

    private fun generateWhenExpression(): KtWhenExpression {
        val numConditions = random.nextInt(maxNumberConditionsInWhenBlock) + 1
        val fakeEntries = (1..numConditions).map { "is A -> {}" }.reduce { acc, it -> "$acc\n$it" }
        val fakeElse = if (random.nextBoolean()) "\nelse -> {}" else ""
        val expression = create { createExpression("when {$fakeEntries$fakeElse}") as KtWhenExpression }
        for (fakeEntry in expression.entries)
            expression.replace(fakeEntry, generateWhenEntry())
        return expression
    }

    private fun generateWhenEntry(): KtWhenEntry {
        val numEntries = random.nextInt(maxNumberEntriesInWhenBlock) + 1
        val fakeConditions = (1..numEntries).map { "is like A" }.reduce { acc, it -> "$acc, $it" }
        val entry = create { createWhenEntry("$fakeConditions -> {}") }
        for (fakeCondition in entry.conditions)
            entry.replace(fakeCondition, generateWhenCondition())
        return entry
    }

    private fun generateWhenCondition() = when (random.nextInt(4)) {
        0 -> generateWhenPatternCondition()
        1 -> generateWhenTypeCondition()
        2 -> generateWhenConditionInRange()
        else -> generateWhenConditionWithExpression()
    }

    private fun generateWhenTypeCondition(): KtWhenConditionIsPattern {
        val condition = create { createWhenCondition("is Type") as KtWhenConditionIsPattern }
        condition.replace(condition.typeReference!!, generateTypeReference())
        return condition
    }

    private fun generateWhenConditionInRange(): KtWhenConditionInRange {
        val condition = create { createWhenCondition("in b") as KtWhenConditionInRange }
        condition.replace(condition.rangeExpression!!, generateExpression())
        return condition
    }

    private fun generateWhenConditionWithExpression(): KtWhenConditionWithExpression {
        val condition = create { createWhenCondition("10") as KtWhenConditionWithExpression }
        condition.replace(condition.expression!!, generateExpression())
        return condition
    }

    private fun generateWhenPatternCondition(): KtWhenConditionIsPattern {
        val condition = create { createWhenCondition("is like A") as KtWhenConditionIsPattern }
        condition.replace(condition.pattern!!, generatePattern())
        return condition
    }

    private fun generatePatternExpression(): KtIsExpression {
        val expression = create { createExpression("a is like A") as KtIsExpression }
        expression.replace(expression.leftHandSide, generateExpression())
        expression.replace(expression.pattern!!, generatePatternWithoutGuard())
        return expression
    }

    private fun generatePattern(): KtPattern {
        val expression = create { createWhenCondition("is like a() && a") as KtWhenConditionIsPattern }
        val pattern = expression.pattern!!
        pattern.replace(pattern.entry!!, generatePatternEntry())
        pattern.replace(pattern.guard!!, generatePatternGuard())
        return pattern
    }

    private fun generatePatternWithoutGuard(): KtPattern {
        val expression = create { createExpression("a is like a") as KtIsExpression }
        val pattern = expression.pattern!!
        pattern.replace(pattern.entry!!, generatePatternEntry())
        return pattern
    }

    //ToDo(sergei)
    private fun generatePatternEntry(): KtPatternEntry {
        val expression = create { createExpression("a is like Pair(1, 2)") as KtIsExpression }
        return expression.pattern!!.entry!!
    }

    private fun generatePatternGuard(): KtPatternGuard {
        val expression = create { createWhenCondition("is like a() && b") as KtWhenConditionIsPattern }
        val patternGuard = expression.pattern!!.guard!!
        patternGuard.replace(patternGuard.expression!!, generateExpression())
        return patternGuard
    }

    private fun generateIsExpression(): KtIsExpression {
        val expression = create { createExpression("a is A") as KtIsExpression }
        expression.replace(expression.leftHandSide, generateExpression())
        expression.replace(expression.typeReference!!, generateTypeReference())
        return expression
    }

    private fun generateParenthesizedExpression(): KtExpression {
        val expression = create { createExpression("(a)") as KtParenthesizedExpression }
        expression.replace(expression.expression!!, generateExpression())
        return expression
    }

    private fun generateSimpleNameExpression() = create { createSimpleName(generateIdentifier()) }

    //ToDo(sergei)
    private fun generateTypeReference() = create { createType(generateIdentifier()) }

    private fun isValidIdentifier(identifier: String): Boolean {
        if (identifier.isEmpty()) return false
        if (identifier.first() == '`' && identifier.last() == '`')
            if (!identifier.substring(1, identifier.lastIndex - 1).contains('`'))
                return true
        if (identifier.contains("[^$alphanum]".toRegex())) return false
        if (digits.contains(identifier.first())) return false
        if (identifier in KEYWORDS.types.map(IElementType::toString)) return false
        return true
    }

    private fun generateIdentifier(): String {
        for (step in 0..maxIdentifierRollingSteps) {
            val isBacktickGenerated = (0..10).fold(true) { acc, it -> acc && random.nextBoolean() }
            val lengthCorrection = if (isBacktickGenerated) 2 else 0
            val length = random.nextInt(maxIdentifierLength) + 1 + lengthCorrection
            val rawIdentifier = StringBuilder(length)
            val symbols = if (isBacktickGenerated) fullAlpha else alphanum
            if (isBacktickGenerated)
                rawIdentifier.append("`")
            for (i in 0 until length)
                rawIdentifier.append(symbols[random.nextInt(symbols.length)])
            if (isBacktickGenerated)
                rawIdentifier.append("`")
            val identifier = rawIdentifier.toString()
            if (isValidIdentifier(identifier))
                return identifier
        }
        return "`non-enrolled identifier`"
    }
}
