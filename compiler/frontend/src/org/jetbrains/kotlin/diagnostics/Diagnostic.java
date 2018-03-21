/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Diagnostic {

    @NotNull
    DiagnosticFactory<?> getFactory();

    @NotNull
    Severity getSeverity();

    @NotNull
    PsiElement getPsiElement();

    @NotNull
    List<TextRange> getTextRanges();

    @NotNull
    PsiFile getPsiFile();

    boolean isValid();

    default Diagnostic getOriginal() {
        return this;
    }

    @NotNull
    default Diagnostic transfer(@NotNull PsiElement element, @NotNull List<TextRange> textRanges) {
        Diagnostic self = this;
        return new Diagnostic() {
            @NotNull
            @Override
            public DiagnosticFactory<?> getFactory() {
                return self.getFactory();
            }

            @NotNull
            @Override
            public Severity getSeverity() {
                return self.getSeverity();
            }

            @NotNull
            @Override
            public PsiElement getPsiElement() {
                return element;
            }

            @NotNull
            @Override
            public List<TextRange> getTextRanges() {
                return textRanges;
            }

            @NotNull
            @Override
            public PsiFile getPsiFile() {
                return element.getContainingFile();
            }

            @Override
            public boolean isValid() {
                return element.isValid();
            }

            @Override
            public Diagnostic getOriginal() {
                return self.getOriginal();
            }
        };
    }
}
