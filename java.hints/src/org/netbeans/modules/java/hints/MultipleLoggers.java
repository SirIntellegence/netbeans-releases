/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.modules.java.hints.jackpot.code.spi.Hint;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerTreeKind;
import org.netbeans.modules.java.hints.jackpot.spi.HintContext;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbBundle;

/**
 *
 * @author vita
 */
@Hint(category="logging")
public final class MultipleLoggers {

    public MultipleLoggers() {
    }

    @TriggerTreeKind(Tree.Kind.CLASS)
    public static Iterable<ErrorDescription> checkMultipleLoggers(HintContext ctx) {
        Element e = ctx.getInfo().getTrees().getElement(ctx.getPath());
        if (e == null || e.getKind() != ElementKind.CLASS || e.getModifiers().contains(Modifier.ABSTRACT) ||
            (e.getEnclosingElement() != null && e.getEnclosingElement().getKind() != ElementKind.PACKAGE)
        ) {
            return null;
        }

        TypeElement loggerTypeElement = ctx.getInfo().getElements().getTypeElement("java.util.logging.Logger"); // NOI18N
        if (loggerTypeElement == null) {
            return null;
        }
        TypeMirror loggerTypeElementAsType = loggerTypeElement.asType();
        if (loggerTypeElementAsType == null || loggerTypeElementAsType.getKind() != TypeKind.DECLARED) {
            return null;
        }

        List<VariableElement> loggerFields = new LinkedList<VariableElement>();
        List<VariableElement> fields = ElementFilter.fieldsIn(e.getEnclosedElements());
        for(VariableElement f : fields) {
            if (f.getKind() != ElementKind.FIELD) {
                continue;
            }

            if (f.asType().equals(loggerTypeElementAsType)) {
                loggerFields.add(f);
            }
        }

        if (loggerFields.size() > 1) {
            List<ErrorDescription> errors = new LinkedList<ErrorDescription>();
            for(VariableElement f : loggerFields) {
                Tree path = ctx.getInfo().getTrees().getTree(f);
                if (path instanceof VariableTree) {
                    int [] span = ctx.getInfo().getTreeUtilities().findNameSpan((VariableTree)path);
                    if (span != null) {
                        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(
                            Severity.WARNING,
                            NbBundle.getMessage(MultipleLoggers.class, "MSG_MultipleLoggers_checkMultipleLoggers"), //NOI18N
                            Collections.<Fix>emptyList(),
                            ctx.getInfo().getFileObject(),
                            span[0],
                            span[1]
                        );
                        errors.add(ed);
                    }
                }
            }
            return errors;
        } else {
            return null;
        }
    }

}
