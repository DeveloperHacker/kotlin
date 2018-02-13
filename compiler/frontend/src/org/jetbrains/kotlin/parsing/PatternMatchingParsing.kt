/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KtTokens.*

private val GUARD_PREFIX = ANDAND!!

data class ParsingState(val isExpression: Boolean)

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
     * : patternTuple
     * : "is" patternTypeReference
     * : patternExpression
     * ;
     *
     * patternVariableDeclaration
     * : "val" identifier ("is" patternTypeReference)?
     * : "val" identifier ("=" patternExpression | patternTypedTuple)?
     * ;
     *
     * patternExpression
     * : ("^")? expression)
     * ;
     *
     * patternTypedTuple
     * : patternTypeReference patternTuple
     * ;
     *
     * patternTypeReference
     * : typeRef
     * ;
     *
     * patternTuple
     * : "(" patternEntry{","}? ")"
     * ;
     *
     * patternGuard
     * : "&&" expression
     * ;
     */
    fun parsePattern(isExpression: Boolean) {
        val state = ParsingState(isExpression)
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
            parsePatternConstraint(state)
        }
        patternMarker.done(PATTERN_ENTRY)
    }

    private fun parsePatternVariableDeclaration(state: ParsingState) {
        val patternMarker = mark()
        if (atSingleUnderscore()) {
            advance() // IDENTIFIER
        } else {
            expect(VAL_KEYWORD, "expected val keyword in begin of variable declaration")
            expect(IDENTIFIER, "expected identifier after val keyword")
            if (at(EQ)) {
                advance() // EQ
                parsePatternValueConstraint(state)
            } else if (at(IS_KEYWORD)) {
                parsePatternTypeConstraint()
            }
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
        if (atTruePatternExpression()) {
            parsePatternExpression()
        } else if (atPatternTypedTuple()) {
            parsePatternTypedTuple(state)
        } else {
            parsePatternExpression()
        }
        patternMarker.done(PATTERN_CONSTRAINT)
    }

    private fun parsePatternConstraint(state: ParsingState) {
        if (at(IS_KEYWORD)) {
            parsePatternTypeConstraint()
        } else {
            parsePatternValueConstraint(state)
        }
    }

    private fun parsePatternTypedTuple(state: ParsingState) {
        val patternMarker = mark()
        if (!at(LPAR)) {
            parsePatternTypeReference()
        }
        parsePatternTuple(state)
        patternMarker.done(PATTERN_TYPED_TUPLE)
    }

    private fun parsePatternTuple(state: ParsingState) {
        val tupleMarker = mark()
        expect(LPAR, "expected '(' in begin of tuple")
        while (at(COMMA)) errorAndAdvance("expected pattern parameter before ','")
        while (!at(RPAR)) {
            parsePatternEntry(state)
            if (at(COMMA)) {
                advance() // COMMA
            } else {
                break
            }
        }
        expect(RPAR, "expected ')' token at end of destructing tuple")
        tupleMarker.done(PATTERN_TUPLE)
    }

    private fun parsePatternGuard() {
        val patternMarker = mark()
        expect(GUARD_PREFIX, "expected guard operator '" + GUARD_PREFIX.toString() + "' in begin of guard")
        kotlinParsing.myExpressionParsing.parseExpression()
        patternMarker.done(PATTERN_GUARD)
    }

    private fun parsePatternTypeReference() {
        val patternMarker = mark()
        kotlinParsing.parseTypeRefNoParenthesized()
        patternMarker.done(PATTERN_TYPE_REFERENCE)
    }

    private fun parsePatternExpression() {
        val patternMarker = mark()
        if (atTruePatternExpression()) {
            if (at(EQ_KEYWORD)) {
                advance() // EQ_KEYWORD
            }
        }
        kotlinParsing.myExpressionParsing.parseExpression()
        patternMarker.done(PATTERN_EXPRESSION)
    }

    private fun atPatternTypedTuple(): Boolean {
        val patternMarker = mark()
        if (!at(LPAR)) {
            parsePatternTypeReference()
        }
        val existTuple = at(LPAR)
        patternMarker.rollbackTo()
        return existTuple
    }

    private fun atGuard(state: ParsingState): Boolean {
        return !state.isExpression && at(GUARD_PREFIX)
    }

    private fun atPatternVariableDeclaration(): Boolean {
        return at(VAL_KEYWORD) || atSingleUnderscore()
    }

    private fun atTruePatternExpression(): Boolean {
        if (atSet(KotlinExpressionParsing.LITERAL_CONSTANT_FIRST)) {
            return true
        }
        if (!at(EQ_KEYWORD)) {
            return false
        }
        val marker = mark()
        advance() // EQ_KEYWORD
        val result = atSet(KotlinExpressionParsing.EXPRESSION_FIRST)
        marker.rollbackTo()
        return result
    }

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder) = KotlinParsing.createForTopLevel(builder)!!
}
