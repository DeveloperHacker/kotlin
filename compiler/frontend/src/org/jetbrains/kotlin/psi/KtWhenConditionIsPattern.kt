/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.pattern.KtPattern

class KtWhenConditionIsPattern(node: ASTNode) : KtWhenCondition(node) {

    val isNegated: Boolean
        get() = node.findChildByType(KtTokens.NOT_IS) != null

    val pattern: KtPattern?
        get() = findChildByType(KtNodeTypes.PATTERN)

    val isSimple: Boolean
        get() = pattern?.isSimple ?: true

    val isRestrictionsFree: Boolean
        get() = pattern?.isRestrictionsFree ?: true

    val onlyTypeRestrictions: Boolean
        get() = pattern?.onlyTypeRestrictions ?: true

    val typeReference: KtTypeReference?
        get() = pattern?.typeReference

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitWhenConditionIsPattern(this, data)
    }
}
