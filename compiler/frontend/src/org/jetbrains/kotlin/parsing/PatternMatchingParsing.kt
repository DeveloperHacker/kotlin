/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KtTokens.*

private val GUARD_PREFIX = ANDAND!!

enum class ParsingLocation {
    TUPLE,
    LIST
}

data class ParsingState(val isTopLevel: Boolean, val isExpression: Boolean, val location: ParsingLocation, val isAsterisk: Boolean) {
    val inList get() = location == ParsingLocation.LIST
    fun stepToDepth() = ParsingState(false, isExpression, location, isAsterisk)
    fun goToList() = ParsingState(isTopLevel, isExpression, ParsingLocation.LIST, isAsterisk)
    fun goToTuple() = ParsingState(isTopLevel, isExpression, ParsingLocation.TUPLE, isAsterisk)
    fun replaceAsterisk(isAsterisk: Boolean) = ParsingState(isTopLevel, isExpression, location, isAsterisk)
    fun resetAsterisk() = ParsingState(isTopLevel, isExpression, location, false)
}

class PatternMatchingParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    private val kotlinParsing: KotlinParsing
) : AbstractKotlinParsing(builder) {

    /**
     * pattern
     * : patternEntry (patternGuard)?
     * ;
     *
     * patternEntry
     * : patternVariableDeclaration
     * : patternTypedTuple
     * : patternTypedList
     * : patternExpression
     * ;
     *
     * patternAsteriskEntry
     * : patternVariableDeclaration-asterisk
     * : "*" patternTypedTuple
     * : "*" patternTypedList
     * : "*" patternExpression
     * ;
     *
     * patternVariableDeclaration
     * : "val" identifier ("is" patternTypeReference)?
     * : "val" identifier ("=" patternExpression | patternTypedTuple)?
     * ;
     *
     * patternVariableDeclaration-asterisk
     * : "val" "*" identifier ("is" patternTypeReference)?
     * : "val" "*" identifier ("=" patternExpression | patternTypedTuple)?
     * ;
     *
     * patternExpression
     * : ("eq")? expression)
     * ;
     *
     * patternTypeReference
     * : typeRef
     * ;
     *
     * patternDeconstruction
     * : patternTypeReference? patternTuple
     * : patternTypeReference? patternList
     * ;
     *
     * patternTuple
     * : "(" patternEntry{","}? ")"
     * ;
     *
     * patternList
     * : "[" patternEntry{","}? ("," patternAsteriskEntry)? "]"
     * ;
     *
     * patternGuard
     * : "&&" expression
     * ;
     */
    fun parsePattern(isExpression: Boolean) {
        val state = ParsingState(true, isExpression, ParsingLocation.TUPLE, false)
        val patternMarker = mark()
        parsePatternEntry(state)
        if (atGuard(state)) {
            parsePatternGuard()
        }
        patternMarker.done(PATTERN)
    }

    private fun parsePatternEntry(state: ParsingState) {
        val patternMarker = mark()
        if (atPatternVariableDeclaration()) {
            parsePatternVariableDeclaration(state)
        } else {
            parsePatternValueConstraint(state)
        }
        patternMarker.done(PATTERN_ENTRY)
    }

    private fun parsePatternVariableDeclaration(state: ParsingState) {
        val patternMarker = mark()
        if (!atSingleUnderscore() && !atAsteriskSingleUnderscore()) {
            expect(VAL_KEYWORD, "expected val keyword in begin of variable declaration")
        }
        expectIf(state.isAsterisk, MUL, "expected '*' token in asterisk pattern variable declaration")
        expect(IDENTIFIER, "expected identifier in variable declaration")
        if (at(EQ)) {
            advance() // EQ
            parsePatternValueConstraint(state.resetAsterisk())
        } else if (at(IS_KEYWORD)) {
            advance() // IS
            parsePatternTypeConstraint()
        }
        patternMarker.done(PATTERN_VARIABLE_DECLARATION)
    }

    private fun parsePatternTypeConstraint() {
        val patternMarker = mark()
        parsePatternTypeReference()
        patternMarker.done(PATTERN_CONSTRAINT)
    }

    private fun parsePatternValueConstraint(state: ParsingState) {
        val patternMarker = mark()
        expectIf(state.isAsterisk, MUL, "expected '*' token in asterisk pattern constraint")
        when {
            atTruePatternExpression() -> parsePatternExpression(state)
            atPatternDeconstruction() -> parsePatternTypedDeconstruction(state)
            else -> parsePatternExpression(state)
        }
        patternMarker.done(PATTERN_CONSTRAINT)
    }

    private fun parsePatternTypedDeconstruction(state: ParsingState) {
        val patternMarker = mark()
        if (!at(LPAR) && !at(LBRACKET)) {
            tryPatternTypeCallExpression()
        }
        when {
            at(LPAR) -> parsePatternDeconstruction(state.goToTuple())
            at(LBRACKET) -> parsePatternDeconstruction(state.goToList())
            else -> error("expected start token of deconstruction")
        }
        patternMarker.done(PATTERN_DECONSTRUCTION)
    }

    private fun parsePatternDeconstruction(state: ParsingState) {
        val (startToken, endToken, nodeType) = when (state.location) {
            ParsingLocation.TUPLE -> Triple(LPAR, RPAR, PATTERN_TUPLE)
            ParsingLocation.LIST -> Triple(LBRACKET, RBRACKET, PATTERN_LIST)
        }
        val tupleMarker = mark()
        expect(startToken, "expected '${startToken.value}' token at start of deconstruction")
        while (at(COMMA)) errorAndAdvance("expected pattern parameter before ','")
        while (!at(endToken)) {
            val isAsterisk = at(MUL) || at(VAL_KEYWORD) && at(1, MUL)
            errorIf(isAsterisk && !state.inList, "asterisk entry supported only in pattern deconstruction list")
            parsePatternEntry(state.replaceAsterisk(isAsterisk).stepToDepth())
            if (!at(COMMA)) break
            advance() // COMMA
            errorIf(isAsterisk, "unexpected pattern entry, after asterisk-entry")
        }
        expect(endToken, "expected '${endToken.value}' token at end of deconstruction")
        tupleMarker.done(nodeType)
    }

    private fun parsePatternGuard() {
        val patternMarker = mark()
        expect(GUARD_PREFIX, "expected guard operator '" + GUARD_PREFIX.toString() + "' in begin of guard")
        kotlinParsing.myExpressionParsing.parseExpression()
        patternMarker.done(PATTERN_GUARD)
    }

    private fun parsePatternTypeReference() {
        val patternMarker = mark()
        tryParseTypeReference()
        patternMarker.done(PATTERN_TYPE_REFERENCE)
    }

    private fun tryPatternTypeCallExpression(): Boolean {
        val patternMarker = mark()
        val collapseMarker = mark()
        val isSuccess = tryParseTypeReference()
        collapseMarker.collapse(PATTERN_TYPE_CALL_INSTANCE)
        patternMarker.done(PATTERN_TYPE_CALL_EXPRESSION)
        return isSuccess
    }

    private fun tryParseTypeReference(): Boolean {
        val typeRefMarker = mark()
        var typeElementMarker = mark()
        val isSuccess = tryParseUserType()
        myBuilder.disableJoiningComplexTokens()
        typeElementMarker = kotlinParsing.parseNullableTypeSuffix(typeElementMarker)
        myBuilder.restoreJoiningComplexTokensState()
        typeElementMarker.drop()
        typeRefMarker.done(TYPE_REFERENCE)
        return isSuccess
    }

    private fun tryParseUserType(): Boolean {
        var userType = mark()
        var reference = mark()
        var isSuccess = true
        while (true) {
            isSuccess = isSuccess && expect(IDENTIFIER, "Expecting type name")
            if (!isSuccess) {
                reference.drop()
                break
            }
            reference.done(REFERENCE_EXPRESSION)

            isSuccess = isSuccess && tryParseTypeArgumentList()

            if (!at(DOT)) break

            val precede = userType.precede()
            userType.done(USER_TYPE)
            userType = precede

            advance() // DOT
            reference = mark()
        }
        userType.done(USER_TYPE)
        return isSuccess
    }

    private fun tryParseTypeArgumentList(): Boolean {
        if (!at(LT)) return true
        var isSuccess = true
        val list = mark()
        myBuilder.disableNewlines()
        advance() // LT
        while (true) {
            val projection = mark()
            kotlinParsing.parseTypeArgumentModifierList()
            if (at(MUL))
                advance() // MUL
            else
                isSuccess = isSuccess && tryParseTypeReference()
            projection.done(TYPE_PROJECTION)
            if (!at(COMMA)) break
            advance() // COMMA
        }
        val atGT = at(GT)
        isSuccess = isSuccess && atGT
        if (!atGT)
            error("Expecting a '>'")
        else
            advance() // GT
        myBuilder.restoreNewlinesState()
        list.done(TYPE_ARGUMENT_LIST)
        return isSuccess
    }

    private fun parsePatternExpression(state: ParsingState) {
        val patternMarker = mark()
        errorIf(state.isTopLevel, "pattern expression not allowed in this position")
        if (atDistinguishablePatternExpression())
            advance() // EQ_KEYWORD
        kotlinParsing.myExpressionParsing.parseExpression()
        patternMarker.done(PATTERN_EXPRESSION)
    }

    private fun atPatternDeconstruction(): Boolean {
        val patternMarker = mark()
        val isSuccessTypeParsing = if (!atSet(TokenSet.create(LPAR, LBRACKET))) tryPatternTypeCallExpression() else true
        val existTuple = atSet(TokenSet.create(LPAR, LBRACKET))
        patternMarker.rollbackTo()
        return isSuccessTypeParsing && existTuple
    }

    private fun atGuard(state: ParsingState): Boolean {
        return !state.isExpression && at(GUARD_PREFIX)
    }

    private fun atAsteriskSingleUnderscore(): Boolean {
        if (!at(MUL)) {
            return false
        }
        val marker = mark()
        advance() // MUL
        val isAsteriskUnderscore = atSingleUnderscore()
        marker.rollbackTo()
        return isAsteriskUnderscore
    }

    private fun atPatternVariableDeclaration() = at(VAL_KEYWORD) || atSingleUnderscore() || atAsteriskSingleUnderscore()

    private fun atDistinguishablePatternExpression(): Boolean {
        if (!at(EQ_KEYWORD)) {
            return false
        }
        val marker = mark()
        advance() // EQ_KEYWORD
        val result = atSet(KotlinExpressionParsing.EXPRESSION_FIRST)
        marker.rollbackTo()
        return result
    }

    private fun atTruePatternExpression() = atSet(KotlinExpressionParsing.LITERAL_CONSTANT_FIRST) || atDistinguishablePatternExpression()

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder) = KotlinParsing.createForTopLevel(builder)!!
}
