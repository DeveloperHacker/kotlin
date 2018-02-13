/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;

public interface KtExpression extends KtElement {
    KtExpression[] EMPTY_ARRAY = new KtExpression[0];

    ArrayFactory<KtExpression> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtExpression[count];

    @Override
    <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data);
}
