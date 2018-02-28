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

data class ParsingState(val isExpression: Boolean, val location: ParsingLocation, val isAsterisk: Boolean) {
    val inList get() = location == ParsingLocation.LIST
    fun goToList() = ParsingState(isExpression, ParsingLocation.LIST, isAsterisk)
    fun goToTuple() = ParsingState(isExpression, ParsingLocation.TUPLE, isAsterisk)
    fun replaceAsterisk(isAsterisk: Boolean) = ParsingState(isExpression, location, isAsterisk)
    fun resetAsterisk() = ParsingState(isExpression, location, false)
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
        val state = ParsingState(isExpression, ParsingLocation.TUPLE, false)
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
            parsePatternTypeConstraint()
        }
        patternMarker.done(PATTERN_VARIABLE_DECLARATION)
    }

    private fun parsePatternTypeConstraint() {
        val patternMarker = mark()
        expect(IS_KEYWORD, "expected is keyword before type reference")
        parsePatternTypeReference()
        patternMarker.done(PATTERN_CONSTRAINT)
    }

    private fun parsePatternValueConstraint(state: ParsingState) {
        val patternMarker = mark()
        expectIf(state.isAsterisk, MUL, "expected '*' token in asterisk pattern constraint")
        when {
            atTruePatternExpression() -> parsePatternExpression()
            atPatternDeconstruction() -> parsePatternDeconstruction(state)
            else -> parsePatternExpression()
        }
        patternMarker.done(PATTERN_CONSTRAINT)
    }

    private fun parsePatternDeconstruction(state: ParsingState) {
        val patternMarker = mark()
        if (!at(LPAR) && !at(LBRACKET)) {
            parsePatternTypeCallExpression()
        }
        when {
            at(LPAR) -> parsePatternTuple(state.goToTuple())
            at(LBRACKET) -> parsePatternTuple(state.goToList())
            else -> error("expected start token of deconstruction")
        }
        patternMarker.done(PATTERN_DECONSTRUCTION)
    }

    private fun parsePatternTuple(state: ParsingState) {
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
            parsePatternEntry(state.replaceAsterisk(isAsterisk))
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
        kotlinParsing.parseSimpleTypeRef()
        patternMarker.done(PATTERN_TYPE_REFERENCE)
    }

    private fun parsePatternTypeCallExpression() {
        val patternMarker = mark()
        val collapseMarker = mark()
        kotlinParsing.parseSimpleTypeRef()
        collapseMarker.collapse(PATTERN_TYPE_CALL_EXPRESSION)
        patternMarker.done(PATTERN_TYPE_CALL_EXPRESSION)
    }

    private fun parsePatternExpression() {
        val patternMarker = mark()
        if (atDistinguishablePatternExpression()) {
            advance() // EQ_KEYWORD
        }
        kotlinParsing.myExpressionParsing.parseExpression()
        patternMarker.done(PATTERN_EXPRESSION)
    }

    private fun atPatternDeconstruction(): Boolean {
        val patternMarker = mark()
        if (!atSet(TokenSet.create(LPAR, LBRACKET))) {
            parsePatternTypeCallExpression()
        }
        val existTuple = atSet(TokenSet.create(LPAR, LBRACKET))
        patternMarker.rollbackTo()
        return existTuple
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
