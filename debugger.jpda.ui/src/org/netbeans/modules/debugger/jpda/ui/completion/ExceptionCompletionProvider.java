/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.debugger.jpda.ui.completion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndex.SearchKind;
import org.netbeans.api.java.source.ClassIndex.SearchScopeType;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.util.Exceptions;

/**
 *
 * @author Martin Entlicher
 */
@MimeRegistration(mimeType = ExceptionClassNbDebugEditorKit.MIME_TYPE, service = CompletionProvider.class)
public class ExceptionCompletionProvider implements CompletionProvider {
    
    private static final Set<? extends SearchScopeType> scopeAll = Collections.singleton(new ClassSearchScopeType());

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            
            @Override
            protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
                if (caretOffset < 0) caretOffset = 0;
                String text;
                try {
                    text = doc.getText(0, caretOffset);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                    text = "";
                }
                Set<? extends SearchScopeType> scope = Collections.singleton(new ClassSearchScopeType(text));
                int n = text.length();
                ClasspathInfo cpi = ClassCompletionProvider.getClassPathInfo();
                ClassIndex classIndex = cpi.getClassIndex();
                Set<String> packageNames = classIndex.getPackageNames(text, false, scope);
                Set<String> resultPackages = new HashSet<String>();
                int lastTextDot = text.lastIndexOf('.');
                for (String pn : packageNames) {
                    int dot = pn.indexOf('.', n);
                    if (dot > 0) pn = pn.substring(0, dot);
                    if (lastTextDot > 0) pn = pn.substring(lastTextDot + 1);
                    if (!resultPackages.contains(pn)) {
                        resultSet.addItem(new ClassCompletionItem(pn, caretOffset, true));
                        resultPackages.add(pn);
                    }
                }
                
                String classFilter;
                if (lastTextDot > 0) {
                    classFilter = text.substring(lastTextDot + 1);
                } else {
                    classFilter = text;
                }
                
                ElementHandle<TypeElement> throwable = null;
                Set<ElementHandle<TypeElement>> throwableTypes = classIndex.getDeclaredTypes(
                        Throwable.class.getSimpleName(), ClassIndex.NameKind.PREFIX,
                        Collections.singleton(new ClassSearchScopeType(Throwable.class.getPackage().getName()+".")));
                for (ElementHandle<TypeElement> type : throwableTypes) {
                    String className = type.getQualifiedName();
                    if (className.equals(Throwable.class.getName())) {
                        throwable = type;
                        break;
                    }
                }
                if (throwable != null) {
                    Set<ElementHandle<TypeElement>> throwables = getAllImplementors(classIndex, throwable, scope);//classIndex.getElements(throwable, Collections.singleton(SearchKind.IMPLEMENTORS), scope);
                    Set<String> resultClasses = new HashSet<String>();
                    for (ElementHandle<TypeElement> type : throwables) {
                        String className = type.getQualifiedName();
                        if (lastTextDot > 0) {
                            className = className.substring(lastTextDot + 1);
                        }
                        int dot = className.indexOf('.');
                        if (dot > 0) className = className.substring(0, dot);
                        if (!resultClasses.contains(className)) {
                            resultSet.addItem(new ClassCompletionItem(className, caretOffset, false));
                            resultClasses.add(className);
                        }
                    }
                    
                }
                resultSet.finish();
            }
        }, component);
    }
    
    private static Set<ElementHandle<TypeElement>> getAllImplementors(
            ClassIndex classIndex,
            ElementHandle<TypeElement> elm,
            Set<? extends SearchScopeType> scope) {
        
        Set<ElementHandle<TypeElement>> impls = new HashSet<ElementHandle<TypeElement>>();
        fillAllImplementors(classIndex, elm, scope, impls);
        return impls;
    }
    
    private static void fillAllImplementors(
            ClassIndex classIndex,
            ElementHandle<TypeElement> elm,
            Set<? extends SearchScopeType> scope,
            Set<ElementHandle<TypeElement>> impls) {
        
        Set<ElementHandle<TypeElement>> impl = classIndex.getElements(elm, Collections.singleton(SearchKind.IMPLEMENTORS), scope);
        impls.addAll(impl);
        Set<ElementHandle<TypeElement>> allImpl = classIndex.getElements(elm, Collections.singleton(SearchKind.IMPLEMENTORS), scopeAll);
        for (ElementHandle<TypeElement> eh : allImpl) {
            fillAllImplementors(classIndex, eh, scope, impls);
        }
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return COMPLETION_QUERY_TYPE;
    }
    
}
