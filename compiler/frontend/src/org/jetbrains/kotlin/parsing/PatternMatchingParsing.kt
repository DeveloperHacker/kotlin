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
    TOP,
    DECLARATION,
    DECONSTRUCTION
}

data class ParsingState(val isExpression: Boolean, val location: ParsingLocation) {
    val isTopLevel get() = location == ParsingLocation.TOP

    fun toDeclaration() = ParsingState(isExpression, ParsingLocation.DECLARATION)
    fun toDeconstruction() = ParsingState(isExpression, ParsingLocation.DECONSTRUCTION)
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
     * : (identifier "=")? patternVariableDeclaration
     * : (identifier "=")? patternTypedTuple
     * : (identifier "=")? patternTypedList
     * : (identifier "=")? patternExpression
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
        val state = ParsingState(isExpression, ParsingLocation.TOP)
        val patternMarker = mark()
        parsePatternEntry(state)
        if (atGuard(state)) {
            parsePatternGuard()
        }
        patternMarker.done(PATTERN)
    }

    private fun parsePatternEntry(state: ParsingState) {
        val patternMarker = mark()
        if (atNamedEntry()) {
            errorIf(state.isTopLevel, "named entry not allowed in this position")
            expect(IDENTIFIER, "expected identifier in begin of named entry")
            expect(EQ, "expected '=' after name of named entry")
        }
        if (atPatternVariableDeclaration()) {
            parsePatternVariableDeclaration(state.toDeclaration())
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
        if (at(MUL)) advance()
        expect(IDENTIFIER, "expected identifier in variable declaration")
        if (at(EQ)) {
            advance() // EQ
            parsePatternValueConstraint(state)
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
        if (at(MUL)) advance()
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
        parsePatternDeconstruction(state.toDeconstruction())
        patternMarker.done(PATTERN_DECONSTRUCTION)
    }

    private fun parsePatternDeconstruction(state: ParsingState) {
        val (isList, isTuple) = when {
            at(LPAR) -> Pair(false, true)
            at(LBRACKET) -> Pair(true, false)
            else -> return error("expected start token of deconstruction")
        }
        val (startToken, endToken, nodeType) = when (isList) {
            false -> Triple(LPAR, RPAR, PATTERN_TUPLE)
            true -> Triple(LBRACKET, RBRACKET, PATTERN_LIST)
        }
        var hasNamedEntry = false
        val tupleMarker = mark()
        expect(startToken, "expected '${startToken.value}' token at start of deconstruction")
        while (at(COMMA)) errorAndAdvance("expected pattern parameter before ','")
        while (!at(endToken)) {
            val atTailEntry = atTailEntry()
            val atNamedEntry = atNamedEntry()
            errorIf(atTailEntry && !isList, "tail entry supported only in pattern deconstruction list")
            errorIf(atNamedEntry && !isTuple, "named entry supported only in pattern deconstruction tuple")
            errorIf(hasNamedEntry && !atNamedEntry, "simple entry unsupported after named entry")
            hasNamedEntry = hasNamedEntry || atNamedEntry
            parsePatternEntry(state)
            if (!at(COMMA)) break
            advance() // COMMA
            errorIf(atTailEntry, "unexpected pattern entry, after asterisk-entry")
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

    private fun atTailEntry(): Boolean {
        return at(MUL) || at(VAL_KEYWORD) && at(1, MUL)
    }

    private fun atNamedEntry(): Boolean {
        return at(IDENTIFIER) && !atSingleUnderscore() && at(1, EQ)
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
