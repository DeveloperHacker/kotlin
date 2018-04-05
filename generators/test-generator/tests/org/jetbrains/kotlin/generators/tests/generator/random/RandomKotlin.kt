/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.KEYWORDS
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.pattern.*
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.*


private const val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private val lower = upper.toLowerCase()
private const val digits = "0123456789"
private val alpha = upper + lower + "_"
private val alphanum = alpha + digits
private const val symbols = "!@#$%^&*()!\"№;%:?'-=+/|'\\ "
private val fullAlpha = alphanum + symbols

private const val maxStringLength = 20
private const val maxIdentifierLength = 10
private const val maxNumberTypeArguments = 3
private const val maxNumberValueArguments = 3
private const val maxNumberNullableSuffices = 3
private const val maxIdentifierRollingSteps = 100
private const val maxNumberStatementsInBlock = 20
private const val maxNumberEntriesInWhenBlock = 5
private const val maxNumberConditionsInWhenBlock = 5
private const val maxNumberElementsInPatternList = 3
private const val maxNumberComponentsInPatternTuple = 3
private const val maxTypeReferenceGenerationDepth = 3
private const val maxExpressionGenerationDepth = 3
private const val maxPatternGenerationDepth = 3
private const val maxBlockGenerationDepth = 3

private val BINARY_OPERATIONS = listOf(
    KtTokens.MUL,
    KtTokens.DIV,
    KtTokens.PERC,
    KtTokens.PLUS,
    KtTokens.MINUS,
    KtTokens.ELVIS,
    KtTokens.LT,
    KtTokens.GT,
    KtTokens.LTEQ,
    KtTokens.GTEQ,
    KtTokens.ANDAND,
    KtTokens.OROR,
    KtTokens.IN_KEYWORD,
    KtTokens.NOT_IN,
    KtTokens.RANGE
)

private val PREFIX_OPERATIONS = listOf(
    KtTokens.MINUS,
    KtTokens.PLUS,
    KtTokens.MINUSMINUS,
    KtTokens.PLUSPLUS,
    KtTokens.EXCL
)

private val POSTFIX_OPERATIONS = listOf(
    KtTokens.PLUSPLUS,
    KtTokens.MINUSMINUS,
    KtTokens.EXCLEXCL
)

fun Random.nextString(length: Int, symbols: String): String {
    val string = StringBuilder()
    for (i in 0 until length)
        string.append(symbols[nextInt(symbols.length)])
    return string.toString()
}

enum class ExpressionLocation {
    LEFT, RIGHT
}

data class GenerationPatternState(val depth: Int, val isAsterisk: Boolean) {
    fun isTopLevel() = depth == 0
    fun stepToDepth() = GenerationPatternState(depth + 1, isAsterisk)
    fun replaceAsterisk(isAsterisk: Boolean) = GenerationPatternState(depth, isAsterisk)
    fun resetAsterisk() = replaceAsterisk(false)
}

open class RandomKotlin(seed: Long, project: Project) : Generator(project) {

    private val random = Random(seed)
    private var expressionDepth = 0
    private var blockDepth = 0

    var name = ""

    override fun generate(name: String): KtFile {
        this.name = name
        val file: KtFile = create { createFile(name, "") }
        file.add(generateNamedFunction())
        return psiFactory.collapseWhiteSpaces(file)
    }

    private fun generateNamedFunction(): KtNamedFunction {
        val functionName = generateIdentifier()
        val function = create { createFunction("fun $functionName() {}") }
        function.replace(function.bodyExpression!!, generateBlockExpression())
        return function
    }

    private fun generateBlockExpression(): KtBlockExpression {
        ++blockDepth
        val block = create { createBlock("") }
        val numStatements = if (blockDepth < maxBlockGenerationDepth) random.nextInt(maxNumberStatementsInBlock) + 1 else 0
        for (i in 0..numStatements) {
            block.addAfter(psiFactory.createNewLine(1), block.lBrace!!)
            block.addAfter(generateBlockElement(), block.lBrace!!)
        }
        --blockDepth
        return block
    }

    private fun generateBlockElement() = when (random.nextInt(3)) {
        0 -> generateDeclaration()
        else -> generateStatement()
    }

    private fun generateDeclaration(): KtDeclaration = when (random.nextInt(2)) {
        0 -> generateProperty()
        else -> generateNamedFunction()
    }

    private fun generateStatement(): KtExpression = when (random.nextInt(4)) {
        0 -> generateAssignment()
        1 -> generateWhileLoop()
        2 -> generateForLoop()
        else -> generateExpression()
    }

    private fun generateProperty(): KtProperty {
        val identifier = generateIdentifier()
        val property = create { createDeclaration("val $identifier = 10") as KtProperty }
        property.initializer = generateExpression()
        return property
    }

    private fun generateAssignment(): KtBinaryExpression {
        val identifier = generateIdentifier()
        val assignment = create { createExpression("$identifier = 10") as KtBinaryExpression }
        assignment.replace(assignment.right!!, generateExpression())
        return assignment
    }

    private fun generateWhileLoop(): KtWhileExpression {
        val loop = create { createExpression("while (a) { }") as KtWhileExpression }
        loop.condition!!.replaceSelf(generateExpression())
        val block = if (random.nextBoolean()) generateBlockExpression() else generateStatement()
        loop.body!!.replaceSelf(block)
        return loop
    }

    private fun generateForLoop(): KtForExpression {
        val identifier = generateIdentifier()
        val loop = create { createExpression("for ($identifier in b) { }") as KtForExpression }
        loop.loopRange!!.replaceSelf(generateExpression())
        val block = if (random.nextBoolean()) generateBlockExpression() else generateStatement()
        loop.body!!.replaceSelf(block)
        return loop
    }

    private fun generateExpression(): KtExpression {
        ++expressionDepth
        val expression = if (expressionDepth < maxExpressionGenerationDepth) {
            when (random.nextInt(13)) {
                0 -> generateSimpleNameExpression()
                1 -> generateIsLikeExpression()
                2 -> generateIsExpression()
                3 -> generateBinaryExpression()
                4 -> generatePrefixExpression()
                5 -> generatePostfixExpression()
                6 -> generateConstantExpression()
                7 -> generateIfExpression()
                8 -> generateCallExpression()
                9 -> generateWhenExpression()
                10 -> generateDotExpression()
                11 -> generateSafeExpression()
                else -> generateParenthesizedExpression()
            }
        } else {
            generateSimpleNameExpression()
        }
        --expressionDepth
        return expression
    }

    private fun parenthesizedWrap(expression: KtExpression): KtParenthesizedExpression {
        val result = create { createExpression("(a)") as KtParenthesizedExpression }
        result.replace(result.expression!!, expression)
        return result
    }

    private fun parenthesizedWrapIf(expression: KtExpression, condition: (KtExpression) -> Boolean) =
        if (condition(expression)) parenthesizedWrap(expression) else expression

    private fun parenthesizedWrapIfNeeded(parentExpression: KtExpression, expression: KtExpression, location: ExpressionLocation) =
        parenthesizedWrapIf(expression) {
            when {
                it is KtIfExpression -> location == ExpressionLocation.LEFT
                it is KtIsExpression && it.isPatternExpression && parentExpression is KtIsExpression -> true
                it is KtIsExpression && parentExpression is KtBinaryExpression && parentExpression.operationToken == KtTokens.LT -> true
                location == ExpressionLocation.LEFT -> it.precedence > parentExpression.precedence
                else -> it.precedence >= parentExpression.precedence
            }
        }

    private val KtExpression.precedence: Int
        get() = when (this) {
            is KtIfExpression -> Int.MAX_VALUE
            is KtIsExpression -> KotlinExpressionParsing.Precedence.IN_OR_IS.ordinal
            is KtBinaryExpression -> KotlinExpressionParsing.Precedence.values().asSequence()
                .filter { it.operations.contains(operationToken) }
                .map(KotlinExpressionParsing.Precedence::ordinal)
                .max() ?: throw IllegalStateException("unresolved binary expression precedence")
            is KtPrefixExpression -> KotlinExpressionParsing.Precedence.PREFIX.ordinal
            is KtPostfixExpression -> KotlinExpressionParsing.Precedence.POSTFIX.ordinal
            else -> -1
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
        val fakeConditions = (1..numEntries).map { "is like A()" }.reduce { acc, it -> "$acc, $it" }
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
        val condition = create { createWhenCondition("is like A()") as KtWhenConditionIsPattern }
        condition.replace(condition.pattern!!, generatePattern())
        return condition
    }

    private fun generateIsLikeExpression(): KtIsExpression {
        val expression = create { createExpression("a is like A()") as KtIsExpression }
        val left = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        expression.replace(expression.leftHandSide, left)
        expression.replace(expression.pattern!!, generatePatternWithoutGuard())
        return expression
    }

    private fun generatePattern(): KtPattern {
        val state = GenerationPatternState(0, false)
        val expression = create { createWhenCondition("is like A() && a") as KtWhenConditionIsPattern }
        val pattern = expression.pattern!!
        pattern.replace(pattern.entry!!, generatePatternEntry(state))
        pattern.replace(pattern.guard!!, generatePatternGuard())
        return pattern
    }

    private fun generatePatternWithoutGuard(): KtPattern {
        val state = GenerationPatternState(0, false)
        val expression = create { createExpression("a is like A()") as KtIsExpression }
        val pattern = expression.pattern!!
        pattern.replace(pattern.entry!!, generatePatternEntry(state))
        return pattern
    }

    private fun generatePatternEntry(state: GenerationPatternState): KtPatternEntry {
        val expression = create { createExpression("a is like A()") as KtIsExpression }
        val entry = expression.pattern!!.entry!!
        if (random.nextBoolean())
            entry.replace(entry.constraint!!, generatePatternVariableDeclaration(state))
        else
            entry.replace(entry.constraint!!, generatePatternValueConstraint(state))
        return entry
    }

    private fun generatePatternVariableDeclaration(state: GenerationPatternState): KtPatternVariableDeclaration {
        val asterisk = if (state.isAsterisk) "*" else ""
        val isTypeConstraint = random.nextInt(3) == 0
        val textConstraint = if (random.nextBoolean()) if (isTypeConstraint) " is A" else " = a" else ""
        val textDeclaration = if (random.nextBoolean()) "val $asterisk${generateIdentifier()}" else "${asterisk}_"
        val expression = create { createExpression("a is like [$textDeclaration$textConstraint]") as KtIsExpression }
        val declaration = expression.pattern!!.entry!!.constraint!!.typedDeconstruction!!.list!!.entries.first().declaration!!
        declaration.constraint?.let {
            if (isTypeConstraint)
                declaration.replace(it, generatePatternTypeConstraint())
            else
                declaration.replace(it, generatePatternValueConstraint(state.resetAsterisk()))
        }
        return declaration
    }

    private fun generatePatternTypeConstraint(): KtPatternConstraint {
        val expression = create { createExpression("a is like _ is A") as KtIsExpression }
        val constraint = expression.pattern!!.entry!!.constraint!!
        constraint.replace(constraint.typeReference!!, generatePatternTypeReference())
        return constraint
    }

    private fun generatePatternValueConstraint(state: GenerationPatternState): KtPatternConstraint {
        return if (!state.isTopLevel() && random.nextBoolean())
            generatePatternExpressionConstraint(state)
        else if (state.depth >= maxPatternGenerationDepth)
            generatePatternExpressionConstraint(state)
        else
            generatePatternDeconstructionConstraint(state)
    }

    private fun generatePatternDeconstructionConstraint(state: GenerationPatternState): KtPatternConstraint {
        val asterisk = if (state.isAsterisk) "*" else ""
        val expression = create { createExpression("a is like [${asterisk}A()]") as KtIsExpression }
        val constraint = expression.pattern!!.entry!!.constraint!!.typedDeconstruction!!.list!!.entries.first().constraint!!
        constraint.replace(constraint.typedDeconstruction!!, generatePatternTypedDeconstruction(state))
        return constraint
    }

    private fun generatePatternExpressionConstraint(state: GenerationPatternState): KtPatternConstraint {
        val asterisk = if (state.isAsterisk) "*" else ""
        val expression = create { createExpression("a is like [${asterisk}a]") as KtIsExpression }
        val constraint = expression.pattern!!.entry!!.constraint!!.typedDeconstruction!!.list!!.entries.first().constraint!!
        constraint.replace(constraint.expression!!, generatePatternExpression())
        return constraint
    }

    private fun generatePatternExpression(): KtPatternExpression {
        val expressionIs = create { createExpression("a is like (eq a)") as KtIsExpression }
        val patternExpression = expressionIs.pattern!!.entry!!.typedDeconstruction!!.tuple!!.entries.first().expression!!
        val expression = generateExpression()
        val isExpressionWithFirstLPAR = expression.text.trim().first() == '('
        val isExpressionWithFirstLBRACE = expression.text.trim().first() == '['
        val isCallExpression = expression is KtCallExpression
        val isArrayAccessExpression = expression is KtArrayAccessExpression
        val eqMustHave = isExpressionWithFirstLPAR || isExpressionWithFirstLBRACE || isCallExpression || isArrayAccessExpression
        patternExpression.replace(patternExpression.expression!!, expression)
        if (!eqMustHave && random.nextBoolean()) {
            val eqToken = patternExpression.eqToken!!
            patternExpression.remove(eqToken.nextSibling)
            patternExpression.remove(eqToken)
        }
        return patternExpression
    }

    private fun generatePatternTypeCallExpression(): KtPatternTypeCallExpression {
        val expression = create { createExpression("a is like A()") as KtIsExpression }
        val typeCallExpression = expression.pattern!!.entry!!.typedDeconstruction!!.typeCallExpression!!
        val instance = typeCallExpression.instance!!
        for (child in instance.allChildren)
            instance.remove(child)
        val typeReference = generateTypeReference()
        for (leaf in typeReference.nextLeafs)
            instance.add(leaf)
        return typeCallExpression
    }

    private fun generatePatternTypeReference(): KtPatternTypeReference {
        val expression = create { createExpression("a is like _ is A") as KtIsExpression }
        val typeReference = expression.pattern!!.entry!!.constraint!!.typeReference!!
        typeReference.replace(typeReference.typeReference!!, generateTypeReference())
        return typeReference
    }

    private fun generatePatternTypedDeconstruction(state: GenerationPatternState): KtPatternTypedDeconstruction {
        val expression = create { createExpression("a is like A()") as KtIsExpression }
        val typedDeconstruction = expression.pattern!!.entry!!.typedDeconstruction!!
        val typeCallExpression = typedDeconstruction.typeCallExpression!!
        val deconstruction = typedDeconstruction.deconstruction!!
        if (random.nextBoolean())
            typedDeconstruction.replace(typeCallExpression, generatePatternTypeCallExpression())
        else
            typedDeconstruction.remove(typeCallExpression)
        if (random.nextBoolean())
            typedDeconstruction.replace(deconstruction, generatePatternTuple(state.stepToDepth()))
        else
            typedDeconstruction.replace(deconstruction, generatePatternList(state.stepToDepth()))
        return typedDeconstruction
    }

    private fun generatePatternTuple(state: GenerationPatternState): KtPatternTuple {
        val components = if (random.nextBoolean()) "a${", a".repeat(random.nextInt(maxNumberComponentsInPatternTuple))}" else ""
        val expression = create { createExpression("a is like A($components)") as KtIsExpression }
        val tuple = expression.pattern!!.entry!!.typedDeconstruction!!.tuple!!
        for (entry in tuple.entries)
            tuple.replace(entry, generatePatternEntry(state.resetAsterisk()))
        return tuple
    }

    private fun generatePatternList(state: GenerationPatternState): KtPatternList {
        val elements = if (random.nextBoolean()) "a${", a".repeat(random.nextInt(maxNumberElementsInPatternList))}" else ""
        val asteriskEntry = if (random.nextBoolean()) (if (elements.isEmpty()) "*a" else ", *a") else ""
        val expression = create { createExpression("a is like A[$elements$asteriskEntry]") as KtIsExpression }
        val list = expression.pattern!!.entry!!.typedDeconstruction!!.list!!
        for (entry in list.entries) {
            val entryState = state.replaceAsterisk(entry.isAsterisk)
            list.replace(entry, generatePatternEntry(entryState))
        }
        return list
    }

    private fun generatePatternGuard(): KtPatternGuard {
        val expression = create { createWhenCondition("is like A() && a") as KtWhenConditionIsPattern }
        val patternGuard = expression.pattern!!.guard!!
        patternGuard.replace(patternGuard.expression!!, generateExpression())
        return patternGuard
    }

    private fun generateIsExpression(): KtIsExpression {
        val expression = create { createExpression("a is A") as KtIsExpression }
        val left = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        expression.replace(expression.leftHandSide, left)
        expression.replace(expression.typeReference!!, generateTypeReference())
        return expression
    }

    private fun generateBinaryExpression(): KtBinaryExpression {
        val op = BINARY_OPERATIONS[random.nextInt(BINARY_OPERATIONS.size)]!!
        val operation = if (random.nextInt(BINARY_OPERATIONS.size + 1) == 0) generateIdentifier() else op.value
        val expression = create { createExpression("a $operation a") as KtBinaryExpression }
        val left = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        val right = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.RIGHT)
        expression.replace(expression.left!!, left)
        expression.replace(expression.right!!, right)
        return expression
    }

    private fun generateDotExpression(): KtDotQualifiedExpression {
        val expression = create { createExpression("a.a") as KtDotQualifiedExpression }
        val left = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        val right = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.RIGHT)
        expression.replace(expression.receiverExpression, left)
        expression.replace(expression.selectorExpression!!, right)
        return expression
    }

    private fun generateSafeExpression(): KtSafeQualifiedExpression {
        val expression = create { createExpression("a?.a") as KtSafeQualifiedExpression }
        val left = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        val right = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.RIGHT)
        expression.replace(expression.receiverExpression, left)
        expression.replace(expression.selectorExpression!!, right)
        return expression
    }

    private fun generatePrefixExpression(): KtPrefixExpression {
        val operation = PREFIX_OPERATIONS[random.nextInt(PREFIX_OPERATIONS.size)]!!.value
        val expression = create { createExpression("${operation}a") as KtPrefixExpression }
        val baseExpression = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.RIGHT)
        expression.replace(expression.baseExpression!!, baseExpression)
        return expression
    }

    private fun generatePostfixExpression(): KtPostfixExpression {
        val operation = POSTFIX_OPERATIONS[random.nextInt(POSTFIX_OPERATIONS.size)]!!.value
        val expression = create { createExpression("a$operation") as KtPostfixExpression }
        val baseExpression = parenthesizedWrapIfNeeded(expression, generateExpression(), ExpressionLocation.LEFT)
        expression.replace(expression.baseExpression!!, baseExpression)
        return expression
    }

    private fun generateConstantExpression(): KtExpression = when (random.nextInt(5)) {
        0 -> psiFactory.createExpression("null")
        1 -> psiFactory.createExpression(random.nextInt().toString())
        2 -> psiFactory.createExpression(random.nextFloat().toString())
        3 -> psiFactory.createExpression(random.nextBoolean().toString())
        else -> {
            val string = random.nextString(random.nextInt(maxStringLength) + 1, fullAlpha)
            val correctString = string.replace("\\", "\\\\").replace("\"", "\\\"")
            psiFactory.createStringTemplate(correctString)
        }
    }

    private fun generateIfExpression(): KtIfExpression {
        val existElse = random.nextBoolean()
        val elseText = if (existElse) " else {}" else ""
        val expression = create { createExpression("if (a) {} $elseText") as KtIfExpression }
        expression.condition!!.replaceSelf(generateExpression())
        val thenExpression = if (random.nextBoolean()) generateBlockExpression() else generateExpression()
        val thenBlock = parenthesizedWrapIf(thenExpression) { it is KtIfExpression && existElse }
        expression.then!!.replaceSelf(thenBlock)
        if (existElse) {
            val elseExpression = if (random.nextBoolean()) generateBlockExpression() else generateStatement()
            expression.`else`!!.replaceSelf(elseExpression)
        }
        return expression
    }

    private fun generateCallExpression(): KtCallExpression {
        val identifier = generateIdentifier()
        val valueArguments = if (random.nextBoolean()) "a${", a".repeat(random.nextInt(maxNumberValueArguments))}" else ""
        val typeArguments = if (random.nextBoolean()) "<T${", T".repeat(random.nextInt(maxNumberTypeArguments))}>" else ""
        val expression = create { createExpression("$identifier$typeArguments($valueArguments)") as KtCallExpression }
        expression.typeArgumentList?.arguments?.forEach {
            it.replace(it.typeReference!!, generateTypeReference())
        }
        expression.valueArgumentList!!.arguments.forEach {
            it.replace(it.getArgumentExpression()!!, generateExpression())
        }
        return expression
    }

    private fun generateParenthesizedExpression() = parenthesizedWrap(generateExpression())

    private fun generateSimpleNameExpression() = create { createSimpleName(generateIdentifier()) }

    private fun generateTypeReference(depth: Int = 0): KtTypeReference {
        val typeIdentifier = generateIdentifier()
        val typeArguments = if (random.nextBoolean()) "<T${", T".repeat(random.nextInt(maxNumberTypeArguments))}>" else ""
        val nullableSuffix = if (random.nextBoolean()) "?".repeat(random.nextInt(maxNumberNullableSuffices)) else ""
        val typeReference = create { createType("$typeIdentifier$typeArguments$nullableSuffix") }
        if (depth > maxTypeReferenceGenerationDepth) return typeReference
        val typeElement = typeReference.typeElement!!.mapWhile<KtNullableType> { it.innerType!! }
        (typeElement as KtUserType).typeArgumentList?.arguments?.forEach {
            it.replace(it.typeReference!!, generateTypeReference(depth + 1))
        }
        return typeReference
    }

    private fun isValidIdentifier(identifier: String): Boolean {
        if (identifier.isEmpty()) return false
        if (identifier.first() == '`' && identifier.last() == '`')
            if (!identifier.substring(1, identifier.lastIndex - 1).contains('`'))
                return true
        if ("_".repeat(identifier.length) == identifier) return false
        if (identifier.contains("[^$alphanum]".toRegex())) return false
        if (digits.contains(identifier.first())) return false
        if (identifier in KEYWORDS.types.map(IElementType::toString)) return false
        return true
    }

    private fun generateIdentifier(): String {
        for (step in 0..maxIdentifierRollingSteps) {
            val isBacktickGenerated = (0..10).fold(true) { acc, _ -> acc && random.nextBoolean() }
            val lengthCorrection = if (isBacktickGenerated) 2 else 0
            val length = random.nextInt(maxIdentifierLength) + 1 + lengthCorrection
            val rawIdentifier = StringBuilder(length)
            val symbols = if (isBacktickGenerated) fullAlpha else alphanum
            if (isBacktickGenerated)
                rawIdentifier.append("`")
            rawIdentifier.append(random.nextString(length, symbols))
            if (isBacktickGenerated)
                rawIdentifier.append("`")
            val identifier = rawIdentifier.toString()
            if (isValidIdentifier(identifier))
                return identifier
        }
        return "`non-enrolled identifier`"
    }

    companion object
}
