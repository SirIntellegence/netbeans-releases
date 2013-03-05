/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.editor.java;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.Modifier.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.tools.Diagnostic;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.*;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.support.ReferencesCount;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.whitelist.WhiteListQuery;
import org.netbeans.modules.java.editor.codegen.GeneratorUtils;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.spi.editor.completion.*;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Dusan Balek
 */
@MimeRegistration(mimeType = "text/x-java", service = CompletionProvider.class, position = 100) //NOI18N
public class JavaCompletionProvider implements CompletionProvider {
    
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        if (typedText != null && typedText.length() == 1
                && (Utilities.getJavaCompletionAutoPopupTriggers().indexOf(typedText.charAt(0)) >= 0
                || (Utilities.autoPopupOnJavaIdentifierPart() && JavaCompletionQuery.isJavaIdentifierPart(typedText)))) {
            if (Utilities.isJavaContext(component, component.getSelectionStart() - 1, true))
                return COMPLETION_QUERY_TYPE;
        }
        return 0;
    }
    
    public CompletionTask createTask(int type, JTextComponent component) {
        if ((type & COMPLETION_QUERY_TYPE) != 0 || type == TOOLTIP_QUERY_TYPE || type == DOCUMENTATION_QUERY_TYPE)
            return new AsyncCompletionTask(new JavaCompletionQuery(type, component.getSelectionStart(), true), component);
        return null;
    }
    
    static CompletionTask createDocTask(ElementHandle element) {
        JavaCompletionQuery query = new JavaCompletionQuery(DOCUMENTATION_QUERY_TYPE, -1, true);
        query.element = element;
        return new AsyncCompletionTask(query, EditorRegistry.lastFocusedComponent());
    }
    
    public static List<? extends CompletionItem> query(Source source, int queryType, int offset, int substitutionOffset) throws Exception {
        assert source != null;
        assert (queryType & COMPLETION_QUERY_TYPE) != 0;
        final JavaCompletionQuery query = new JavaCompletionQuery(queryType, offset, false);
        ParserManager.parse(Collections.singletonList(source), query.getTask());
        if (offset != substitutionOffset) {
            for (JavaCompletionItem jci : query.results) {
                jci.substitutionOffset += (substitutionOffset - offset);
            }
        }
        return query.results;
    }
    
    static final class JavaCompletionQuery extends AsyncCompletionQuery {
        
        static final AtomicBoolean javadocBreak = new AtomicBoolean();
        
        private static final String ERROR = "<error>"; //NOI18N
        private static final String INIT = "<init>"; //NOI18N
        private static final String SPACE = " "; //NOI18N
        private static final String COLON = ":"; //NOI18N
        private static final String SEMI = ";"; //NOI18N
        private static final String EMPTY = ""; //NOI18N
        
        private static final String ABSTRACT_KEYWORD = "abstract"; //NOI18N
        private static final String ASSERT_KEYWORD = "assert"; //NOI18N
        private static final String BOOLEAN_KEYWORD = "boolean"; //NOI18N
        private static final String BREAK_KEYWORD = "break"; //NOI18N
        private static final String BYTE_KEYWORD = "byte"; //NOI18N
        private static final String CASE_KEYWORD = "case"; //NOI18N
        private static final String CATCH_KEYWORD = "catch"; //NOI18N
        private static final String CHAR_KEYWORD = "char"; //NOI18N
        private static final String CLASS_KEYWORD = "class"; //NOI18N
        private static final String CONTINUE_KEYWORD = "continue"; //NOI18N
        private static final String DEFAULT_KEYWORD = "default"; //NOI18N
        private static final String DO_KEYWORD = "do"; //NOI18N
        private static final String DOUBLE_KEYWORD = "double"; //NOI18N
        private static final String ELSE_KEYWORD = "else"; //NOI18N
        private static final String ENUM_KEYWORD = "enum"; //NOI18N
        private static final String EXTENDS_KEYWORD = "extends"; //NOI18N
        private static final String FALSE_KEYWORD = "false"; //NOI18N
        private static final String FINAL_KEYWORD = "final"; //NOI18N
        private static final String FINALLY_KEYWORD = "finally"; //NOI18N
        private static final String FLOAT_KEYWORD = "float"; //NOI18N
        private static final String FOR_KEYWORD = "for"; //NOI18N
        private static final String IF_KEYWORD = "if"; //NOI18N
        private static final String IMPLEMENTS_KEYWORD = "implements"; //NOI18N
        private static final String IMPORT_KEYWORD = "import"; //NOI18N
        private static final String INSTANCEOF_KEYWORD = "instanceof"; //NOI18N
        private static final String INT_KEYWORD = "int"; //NOI18N
        private static final String INTERFACE_KEYWORD = "interface"; //NOI18N
        private static final String LONG_KEYWORD = "long"; //NOI18N
        private static final String NATIVE_KEYWORD = "native"; //NOI18N
        private static final String NEW_KEYWORD = "new"; //NOI18N
        private static final String NULL_KEYWORD = "null"; //NOI18N
        private static final String PACKAGE_KEYWORD = "package"; //NOI18N
        private static final String PRIVATE_KEYWORD = "private"; //NOI18N
        private static final String PROTECTED_KEYWORD = "protected"; //NOI18N
        private static final String PUBLIC_KEYWORD = "public"; //NOI18N
        private static final String RETURN_KEYWORD = "return"; //NOI18N
        private static final String SHORT_KEYWORD = "short"; //NOI18N
        private static final String STATIC_KEYWORD = "static"; //NOI18N
        private static final String STRICT_KEYWORD = "strictfp"; //NOI18N
        private static final String SUPER_KEYWORD = "super"; //NOI18N
        private static final String SWITCH_KEYWORD = "switch"; //NOI18N
        private static final String SYNCHRONIZED_KEYWORD = "synchronized"; //NOI18N
        private static final String THIS_KEYWORD = "this"; //NOI18N
        private static final String THROW_KEYWORD = "throw"; //NOI18N
        private static final String THROWS_KEYWORD = "throws"; //NOI18N
        private static final String TRANSIENT_KEYWORD = "transient"; //NOI18N
        private static final String TRUE_KEYWORD = "true"; //NOI18N
        private static final String TRY_KEYWORD = "try"; //NOI18N
        private static final String VOID_KEYWORD = "void"; //NOI18N
        private static final String VOLATILE_KEYWORD = "volatile"; //NOI18N
        private static final String WHILE_KEYWORD = "while"; //NOI18N
        
        private static final String JAVA_LANG_OBJECT = "java.lang.Object"; //NOI18N
        private static final String JAVA_LANG_ITERABLE = "java.lang.Iterable"; //NOI18N

        private static final String[] PRIM_KEYWORDS = new String[] {
            BOOLEAN_KEYWORD, BYTE_KEYWORD, CHAR_KEYWORD, DOUBLE_KEYWORD,
            FLOAT_KEYWORD, INT_KEYWORD, LONG_KEYWORD, SHORT_KEYWORD
        };
        
        private static final String[] STATEMENT_KEYWORDS = new String[] {
            DO_KEYWORD, IF_KEYWORD, FOR_KEYWORD, SWITCH_KEYWORD, SYNCHRONIZED_KEYWORD, TRY_KEYWORD,
            VOID_KEYWORD, WHILE_KEYWORD
        };
        
        private static final String[] STATEMENT_SPACE_KEYWORDS = new String[] {
            ASSERT_KEYWORD, NEW_KEYWORD, THROW_KEYWORD
        };
        
        private static final String[] BLOCK_KEYWORDS = new String[] {
            ASSERT_KEYWORD, CLASS_KEYWORD, FINAL_KEYWORD, NEW_KEYWORD,
            THROW_KEYWORD
        };

        private static final String[] CLASS_BODY_KEYWORDS = new String[] {
            ABSTRACT_KEYWORD, CLASS_KEYWORD, ENUM_KEYWORD, FINAL_KEYWORD,
            INTERFACE_KEYWORD, NATIVE_KEYWORD, PRIVATE_KEYWORD, PROTECTED_KEYWORD,
            PUBLIC_KEYWORD, STATIC_KEYWORD, STRICT_KEYWORD, SYNCHRONIZED_KEYWORD,
            TRANSIENT_KEYWORD, VOID_KEYWORD, VOLATILE_KEYWORD
        };

        private static final String SKIP_ACCESSIBILITY_CHECK = "org.netbeans.modules.editor.java.JavaCompletionProvider.skipAccessibilityCheck"; //NOI18N
        
        private ArrayList<JavaCompletionItem> results;
        private byte hasAdditionalItems = 0; //no additional items
        private MethodParamsTipPaintComponent toolTip;
        private CompletionDocumentation documentation;
        private int anchorOffset;
        private int toolTipOffset;

        private JTextComponent component;

        private int queryType;
        private int caretOffset;
        private String filterPrefix;
        
        private ElementHandle element;
        private boolean hasTask;
        
        private JavaCompletionQuery(int queryType, int caretOffset, boolean hasTask) {
            this.queryType = queryType;
            this.caretOffset = caretOffset;
            this.hasTask = hasTask;
        }

        @Override
        protected void preQueryUpdate(JTextComponent component) {
            int newCaretOffset = component.getSelectionStart();
            if (newCaretOffset >= caretOffset) {
                try {
                    if (isJavaIdentifierPart(component.getDocument().getText(caretOffset, newCaretOffset - caretOffset)))
                        return;
                } catch (BadLocationException e) {
                }
            }
            Completion.get().hideCompletion();
        }
        
        @Override
        protected void prepareQuery(JTextComponent component) {
            this.component = component;
            if (queryType == TOOLTIP_QUERY_TYPE) {
                this.toolTip = new MethodParamsTipPaintComponent(component);
            }
        }
        
        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                this.caretOffset = caretOffset;
                if (queryType == TOOLTIP_QUERY_TYPE || Utilities.isJavaContext(component, caretOffset, true)) {
                    results = null;
                    documentation = null;
                    if (toolTip != null)
                        toolTip.clearData();
                    anchorOffset = -1;
                    Source source = null;
                    if (queryType == DOCUMENTATION_QUERY_TYPE && element != null) {
                        ClasspathInfo cpInfo = ClasspathInfo.create(doc);
                        if (cpInfo != null) {
                            FileObject fo = SourceUtils.getFile(element, cpInfo);
                            if (fo != null)
                                source = Source.create(fo);
                        }
                    }
                    if (source == null)
                        source = Source.create(doc);
                    if (source != null) {
                        ParserManager.parse(Collections.singletonList(source), getTask());
                        if ((queryType & COMPLETION_QUERY_TYPE) != 0) {
                            if (results != null) {
                                resultSet.addAllItems(results);
                            }
                            resultSet.setHasAdditionalItems(hasAdditionalItems > 0);
                            if (hasAdditionalItems == 1)
                                resultSet.setHasAdditionalItemsText(NbBundle.getMessage(JavaCompletionProvider.class, "JCP-imported-items")); //NOI18N
                            if (hasAdditionalItems == 2)
                                resultSet.setHasAdditionalItemsText(NbBundle.getMessage(JavaCompletionProvider.class, "JCP-instance-members")); //NOI18N
                        } else if (queryType == TOOLTIP_QUERY_TYPE) {
                            if (toolTip != null && toolTip.hasData())
                                resultSet.setToolTip(toolTip);
                        } else if (queryType == DOCUMENTATION_QUERY_TYPE) {
                            if (documentation instanceof JavaCompletionDoc) {
                                while (!isTaskCancelled()) {
                                    try {
                                        if (javadocBreak.getAndSet(false)) {
                                            Completion c = Completion.get();
                                            c.hideDocumentation();
                                            c.showDocumentation();
                                        }
                                        ((JavaCompletionDoc)documentation).getFutureText().get(250, TimeUnit.MILLISECONDS);
                                        resultSet.setDocumentation(documentation);
                                        break;
                                    } catch (TimeoutException timeOut) {/*retry*/}
                                }                                
                            } else if (documentation != null) {
                                resultSet.setDocumentation(documentation);
                            }
                        }
                        if (anchorOffset > -1)
                            resultSet.setAnchorOffset(anchorOffset);
                    }
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } finally {
                resultSet.finish();
            }
        }
        
        @Override
        protected boolean canFilter(JTextComponent component) {
            filterPrefix = null;
            final int newOffset = component.getSelectionStart();
            final Document doc = component.getDocument();
            if ((queryType & COMPLETION_QUERY_TYPE) != 0) {
                final int offset = Math.min(anchorOffset, caretOffset);
                if (offset > -1) {
                    if (newOffset < offset)
                        return true;
                    if (newOffset >= caretOffset) {
                        try {
                            final int len = newOffset - offset;
                            if (len == 0) {
                                filterPrefix = EMPTY;
                            } else if (len > 0) {
                                doc.render(new Runnable() {
                                    @Override
                                    public void run() {
                                        TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(TokenHierarchy.get(doc), offset);
                                        if (ts != null && ts.move(offset) == 0 && ts.moveNext()) {
                                            if ((ts.token().id() == JavaTokenId.IDENTIFIER ||
                                                    ts.token().id().primaryCategory().startsWith("keyword") || //NOI18N
                                                    ts.token().id().primaryCategory().startsWith("string") || //NOI18N
                                                    ts.token().id().primaryCategory().equals("literal")) //NOI18N
                                                    && ts.token().length() >= len) { //TODO: Use isKeyword(...) when available
                                                filterPrefix = ts.token().text().toString().substring(0, len);
                                            }
                                        }
                                    }
                                });
                            }
                            if (filterPrefix == null) {
                                String prefix = doc.getText(offset, newOffset - offset);
                                if (prefix.length() > 0 && Utilities.getJavaCompletionAutoPopupTriggers().indexOf(prefix.charAt(prefix.length() - 1)) >= 0)
                                    return false;
                            } else if (filterPrefix.length() == 0) {
                                anchorOffset = newOffset;
                            }
                        } catch (BadLocationException e) {}
                        return true;
                    }
                }
                return false;
            } else if (queryType == TOOLTIP_QUERY_TYPE) {
                try {
                    if (newOffset == caretOffset)
                        filterPrefix = EMPTY;
                    else if (newOffset - caretOffset > 0)
                        filterPrefix = doc.getText(caretOffset, newOffset - caretOffset);
                    else if (newOffset - caretOffset < 0)
                        filterPrefix = newOffset > toolTipOffset ? doc.getText(newOffset, caretOffset - newOffset) : null;
                } catch (BadLocationException ex) {}
                return (filterPrefix != null && filterPrefix.indexOf(',') == -1 && filterPrefix.indexOf('(') == -1 && filterPrefix.indexOf(')') == -1); // NOI18N
            }
            return false;
        }
        
        @Override
        protected void filter(CompletionResultSet resultSet) {
            try {
                if ((queryType & COMPLETION_QUERY_TYPE) != 0) {
                    if (results != null) {
                        if (filterPrefix != null) {
                            resultSet.addAllItems(getFilteredData(results, filterPrefix));
                            resultSet.setHasAdditionalItems(hasAdditionalItems > 0);
                        } else {
                            Completion.get().hideDocumentation();
                            Completion.get().hideCompletion();
                        }
                    }
                } else if (queryType == TOOLTIP_QUERY_TYPE) {
                    resultSet.setToolTip(toolTip != null && toolTip.hasData() ? toolTip : null);
                }
                resultSet.setAnchorOffset(anchorOffset);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            resultSet.finish();
        }

        public void run(CompilationController controller) throws Exception {
            if (!hasTask || !isTaskCancelled()) {
                if ((queryType & COMPLETION_QUERY_TYPE) != 0) {
                    if (component != null)
                        component.putClientProperty("completion-active", Boolean.TRUE); //NOI18N
                    resolveCompletion(controller);
                    if (component != null && isTaskCancelled())
                        component.putClientProperty("completion-active", Boolean.FALSE); //NOI18N
                } else if (queryType == TOOLTIP_QUERY_TYPE) {
                    resolveToolTip(controller);
                } else if (queryType == DOCUMENTATION_QUERY_TYPE) {
                    resolveDocumentation(controller);
                }
            }
        }

        private UserTask getTask() {
            return new Task();
        }
        
        private void resolveToolTip(final CompilationController controller) throws IOException {
            Env env = getCompletionEnvironment(controller, queryType);
            if (env == null)
                return;
            Tree lastTree = null;
            int offset = env.getOffset();
            TreePath path = env.getPath();
            while (path != null) {
                Tree tree = path.getLeaf();
                if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
                    MethodInvocationTree mi = (MethodInvocationTree)tree;
                    CompilationUnitTree root = env.getRoot();
                    SourcePositions sourcePositions = env.getSourcePositions();
                    int startPos = lastTree != null ? (int)sourcePositions.getStartPosition(root, lastTree) : offset;
                    List<Tree> argTypes = getArgumentsUpToPos(env, mi.getArguments(), (int)sourcePositions.getEndPosition(root, mi.getMethodSelect()), startPos, false);
                    if (argTypes != null) {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        TypeMirror[] types = new TypeMirror[argTypes.size()];
                        int j = 0;
                        for (Tree t : argTypes)
                            types[j++] = controller.getTrees().getTypeMirror(new TreePath(path, t));
                        List<List<String>> params = null;
                        Tree mid = mi.getMethodSelect();
                        path = new TreePath(path, mid);
                        switch (mid.getKind()) {
                            case MEMBER_SELECT: {
                                ExpressionTree exp = ((MemberSelectTree)mid).getExpression();
                                path = new TreePath(path, exp);
                                final Trees trees = controller.getTrees();
                                final TypeMirror type = trees.getTypeMirror(path);
                                final Element element = trees.getElement(path);
                                final boolean isStatic = element != null && (element.getKind().isClass() || element.getKind().isInterface() || element.getKind() == TYPE_PARAMETER);
                                final boolean isSuperCall = element != null && element.getKind().isField() && element.getSimpleName().contentEquals(SUPER_KEYWORD);
                                final Scope scope = env.getScope();
                                TypeElement enclClass = scope.getEnclosingClass();
                                final TypeMirror enclType = enclClass != null ? enclClass.asType() : null;
                                ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                    public boolean accept(Element e, TypeMirror t) {
                                        return (!isStatic || e.getModifiers().contains(STATIC) || e.getKind() == CONSTRUCTOR) && (t.getKind() != TypeKind.DECLARED || trees.isAccessible(scope, e, (DeclaredType)(isSuperCall && enclType != null ? enclType : t)));
                                    }
                                };
                                params = getMatchingParams(controller, type, controller.getElementUtilities().getMembers(type, acceptor), ((MemberSelectTree)mid).getIdentifier().toString(), types, controller.getTypes());
                                break;
                            }
                            case IDENTIFIER: {
                                final Scope scope = env.getScope();
                                final TreeUtilities tu = controller.getTreeUtilities();
                                final Trees trees = controller.getTrees();
                                final TypeElement enclClass = scope.getEnclosingClass();
                                final boolean isStatic = enclClass != null ? (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic())) : false;
                                ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                    public boolean accept(Element e, TypeMirror t) {
                                        switch (e.getKind()) {
                                            case CONSTRUCTOR:
                                                return !e.getModifiers().contains(PRIVATE);
                                            case METHOD:
                                                return (!isStatic || e.getModifiers().contains(STATIC)) && trees.isAccessible(scope, e, (DeclaredType)t);
                                            default:
                                                return false;
                                        }
                                    }
                                };
                                String name = ((IdentifierTree)mid).getName().toString();
                                if (SUPER_KEYWORD.equals(name) && enclClass != null) {
                                    TypeMirror superclass = enclClass.getSuperclass();
                                    params = getMatchingParams(controller, superclass, controller.getElementUtilities().getMembers(superclass, acceptor), INIT, types, controller.getTypes());
                                } else if (THIS_KEYWORD.equals(name) && enclClass != null) {
                                    TypeMirror thisclass = enclClass.asType();
                                    params = getMatchingParams(controller, thisclass, controller.getElementUtilities().getMembers(thisclass, acceptor), INIT, types, controller.getTypes());
                                } else {
                                    params = getMatchingParams(controller, enclClass != null ? enclClass.asType() : null, controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), name, types, controller.getTypes());
                                }
                                break;
                            }
                        }
                        if (params != null)
                            toolTip.setData(params, types.length);
                        startPos = (int)sourcePositions.getEndPosition(env.getRoot(), mi.getMethodSelect());
                        String text = controller.getText().substring(startPos, offset);
                        int idx = text.indexOf('('); //NOI18N
                        anchorOffset = idx < 0 ? startPos : startPos + controller.getSnapshot().getOriginalOffset(idx);
                        idx = text.lastIndexOf(','); //NOI18N
                        toolTipOffset = idx < 0 ? startPos : startPos + controller.getSnapshot().getOriginalOffset(idx);
                        if (toolTipOffset < anchorOffset)
                            toolTipOffset = anchorOffset;
                        return;
                    }
                } else if (tree.getKind() == Tree.Kind.NEW_CLASS) {
                    NewClassTree nc = (NewClassTree)tree;
                    CompilationUnitTree root = env.getRoot();
                    SourcePositions sourcePositions = env.getSourcePositions();
                    int startPos = lastTree != null ? (int)sourcePositions.getStartPosition(root, lastTree) : offset;
                    int pos = (int)sourcePositions.getEndPosition(root, nc.getIdentifier());
                    List<Tree> argTypes = getArgumentsUpToPos(env, nc.getArguments(), pos, startPos, false);
                    if (argTypes != null) {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        TypeMirror[] types = new TypeMirror[argTypes.size()];
                        int j = 0;
                        for (Tree t : argTypes)
                            types[j++] = controller.getTrees().getTypeMirror(new TreePath(path, t));
                        path = new TreePath(path, nc.getIdentifier());
                        final Trees trees = controller.getTrees();                        
                        TypeMirror type = trees.getTypeMirror(path);
                        if (type != null && type.getKind() == TypeKind.ERROR && path.getLeaf().getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
                            path = new TreePath(path, ((ParameterizedTypeTree)path.getLeaf()).getType());
                            type = trees.getTypeMirror(path);
                        }
                        final Element el = trees.getElement(path);
                        final Scope scope = env.getScope();
                        final boolean isAnonymous = nc.getClassBody() != null || el.getKind().isInterface() || el.getModifiers().contains(ABSTRACT);
                        ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                            public boolean accept(Element e, TypeMirror t) {
                                return e.getKind() == CONSTRUCTOR && (trees.isAccessible(scope, e, (DeclaredType)t) || isAnonymous && e.getModifiers().contains(PROTECTED));
                            }
                        };
                        List<List<String>> params = getMatchingParams(controller, type, controller.getElementUtilities().getMembers(type, acceptor), INIT, types, controller.getTypes());
                        if (params != null)
                            toolTip.setData(params, types.length);
                        if (pos < 0) {
                            path = path.getParentPath();
                            pos = (int)sourcePositions.getStartPosition(root, path.getLeaf());
                        }
                        String text = controller.getText().substring(pos, offset);
                        int idx = text.indexOf('('); //NOI18N
                        anchorOffset = idx < 0 ? pos : pos + controller.getSnapshot().getOriginalOffset(idx);
                        idx = text.lastIndexOf(','); //NOI18N
                        toolTipOffset = idx < 0 ? pos : pos + controller.getSnapshot().getOriginalOffset(idx);
                        if (toolTipOffset < anchorOffset)
                            toolTipOffset = anchorOffset;
                        return;
                    }
                }
                lastTree = tree;
                path = path.getParentPath();
            }
        }
        
        private void resolveDocumentation(CompilationController controller) throws IOException {
            controller.toPhase(Phase.RESOLVED);
            Element el = null;
            if (element != null) {
                el = element.resolve(controller);
            } else {
                Env e = getCompletionEnvironment(controller, queryType);
                if (e != null)
                    el = controller.getTrees().getElement(e.getPath());
            }
            if (el != null) {
                switch (el.getKind()) {
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                    if (el.asType().getKind() == TypeKind.ERROR)
                        break;
                case CONSTRUCTOR:
                case ENUM_CONSTANT:
                case FIELD:
                case METHOD:
                    documentation = JavaCompletionDoc.create(controller, el, new Callable<Boolean>() {
                        public Boolean call() {
                            if (isTaskCancelled()) {
                                return true;
                            }
                            if (javadocBreak.getAndSet(false)) {
                                Completion c = Completion.get();
                                c.hideDocumentation();
                                c.showDocumentation();
                            }
                            return false;
                        }
                    });
                }
            }
        }
        
        private void resolveCompletion(CompilationController controller) throws IOException {
            Env env = getCompletionEnvironment(controller, queryType);
            if (env == null)
                return;
            results = new ArrayList<JavaCompletionItem>();
            anchorOffset = controller.getSnapshot().getOriginalOffset(env.getOffset());
            TreePath path = env.getPath();
            switch(path.getLeaf().getKind()) {
                case COMPILATION_UNIT:
                    insideCompilationUnit(env);
                    break;
                case IMPORT:
                    insideImport(env);
                    break;
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                    insideClass(env);
                    break;
                case VARIABLE:
                    insideVariable(env);
                    break;
                case METHOD:
                    insideMethod(env);
                    break;
                case MODIFIERS:
                    insideModifiers(env, path);
                    break;
                case ANNOTATION:
                    insideAnnotation(env);
                    break;
                case TYPE_PARAMETER:
                    insideTypeParameter(env);
                    break;
                case PARAMETERIZED_TYPE:
                    insideParameterizedType(env, path);
                    break;
                case UNBOUNDED_WILDCARD:
                case EXTENDS_WILDCARD:
                case SUPER_WILDCARD:
                    TreePath parentPath = path.getParentPath();
                    if (parentPath.getLeaf().getKind() == Tree.Kind.PARAMETERIZED_TYPE)
                        insideParameterizedType(env, parentPath);
                    break;
                case BLOCK:
                    insideBlock(env);
                    break;
                case MEMBER_SELECT:
                    insideMemberSelect(env);
                    break;
                case MEMBER_REFERENCE:
                    insideMemberReference(env);
                    break;
                case LAMBDA_EXPRESSION:
                    insideLambdaExpression(env);
                    break;
                case METHOD_INVOCATION:
                    insideMethodInvocation(env);
                    break;
                case NEW_CLASS:
                    insideNewClass(env);
                    break;
                case ASSERT:
                case RETURN:
                case THROW:
                    localResult(env);
                    addValueKeywords(env);
                    break;
                case TRY:
                    insideTry(env);
                    break;
                case CATCH:
                    insideCatch(env);
                    break;
                case UNION_TYPE:
                    insideUnionType(env);
                    break;
                case IF:
                    insideIf(env);
                    break;
                case WHILE_LOOP:
                    insideWhile(env);
                    break;
                case DO_WHILE_LOOP:
                    insideDoWhile(env);
                    break;
                case FOR_LOOP:
                    insideFor(env);
                    break;
                case ENHANCED_FOR_LOOP:
                    insideForEach(env);
                    break;
                case SWITCH:
                    insideSwitch(env);
                    break;
                case CASE:
                    insideCase(env);
                    break;
                case LABELED_STATEMENT:
                    localResult(env);
                    addKeywordsForStatement(env);
                    break;
                case PARENTHESIZED:
                    insideParens(env);
                    break;
                case TYPE_CAST:
                    insideExpression(env, path);
                    break;
                case INSTANCE_OF:
                    insideTypeCheck(env);
                    break;
                case ARRAY_ACCESS:
                    insideArrayAccess(env);
                    break;
                case NEW_ARRAY:
                    insideNewArray(env);
                    break;
                case ASSIGNMENT:
                    insideAssignment(env);
                    break;
                case MULTIPLY_ASSIGNMENT:
                case DIVIDE_ASSIGNMENT:
                case REMAINDER_ASSIGNMENT:
                case PLUS_ASSIGNMENT:
                case MINUS_ASSIGNMENT:
                case LEFT_SHIFT_ASSIGNMENT:
                case RIGHT_SHIFT_ASSIGNMENT:
                case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                case AND_ASSIGNMENT:
                case XOR_ASSIGNMENT:
                case OR_ASSIGNMENT:
                    insideCompoundAssignment(env);
                    break;
                case PREFIX_INCREMENT:
                case PREFIX_DECREMENT:
                case UNARY_PLUS:
                case UNARY_MINUS:
                case BITWISE_COMPLEMENT:
                case LOGICAL_COMPLEMENT:
                    localResult(env);
                    break;
                case AND:
                case CONDITIONAL_AND:
                case CONDITIONAL_OR:
                case DIVIDE:
                case EQUAL_TO:
                case GREATER_THAN:
                case GREATER_THAN_EQUAL:
                case LEFT_SHIFT:
                case LESS_THAN:
                case LESS_THAN_EQUAL:
                case MINUS:
                case MULTIPLY:
                case NOT_EQUAL_TO:
                case OR:
                case PLUS:
                case REMAINDER:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                case XOR:
                    insideBinaryTree(env);
                    break;
                case CONDITIONAL_EXPRESSION:
                    insideConditionalExpression(env);
                    break;
                case EXPRESSION_STATEMENT:
                    insideExpressionStatement(env);
                    break;
                case BREAK:
                    insideBreak(env);
                    break;
                case STRING_LITERAL:
                    insideStringLiteral(env);
                    break;
            }
        }
        
        private void insideCompilationUnit(Env env) throws IOException {
            int offset = env.getOffset();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            Tree pkg = root.getPackageName();
            if (pkg == null || offset <= sourcePositions.getStartPosition(root, root)) {
                addKeywordsForCU(env);
                return;
            }
            if (offset <= sourcePositions.getStartPosition(root, pkg)) {
                addPackages(env, null, true);
            } else {
                TokenSequence<JavaTokenId> first = findFirstNonWhitespaceToken(env, (int)sourcePositions.getEndPosition(root, pkg), offset);
                if (first != null && first.token().id() == JavaTokenId.SEMICOLON)
                    addKeywordsForCU(env);
            }
        }
        
        private void insideImport(Env env) {
            int offset = env.getOffset();
            String prefix = env.getPrefix();
            ImportTree im = (ImportTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            if (offset <= sourcePositions.getStartPosition(root, im.getQualifiedIdentifier())) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, im, offset);
                if (last != null && last.token().id() == JavaTokenId.IMPORT && Utilities.startsWith(STATIC_KEYWORD, prefix))
                    addKeyword(env, STATIC_KEYWORD, SPACE, false);
                addPackages(env, null, false);
            }            
        }
        
        private void insideClass(Env env) throws IOException {
            int offset = env.getOffset();
            env.insideClass(true);
            TreePath path = env.getPath();
            ClassTree cls = (ClassTree)path.getLeaf();
            CompilationController controller = env.getController();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int startPos = (int)sourcePositions.getEndPosition(root, cls.getModifiers());
            if (startPos <= 0)
                startPos = (int)sourcePositions.getStartPosition(root, cls);
            String headerText = controller.getText().substring(startPos, offset);
            int idx = headerText.indexOf('{'); //NOI18N
            if (idx >= 0) {
                addKeywordsForClassBody(env);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                addElementCreators(env);
                return;
            }
            TreeUtilities tu = controller.getTreeUtilities();
            Tree lastImpl = null;
            for (Tree impl : cls.getImplementsClause()) {
                int implPos = (int)sourcePositions.getEndPosition(root, impl);
                if (implPos == Diagnostic.NOPOS || offset <= implPos)
                    break;
                lastImpl = impl;
                startPos = implPos;
            }
            if (lastImpl != null) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, startPos, offset);
                if (last != null && last.token().id() == JavaTokenId.COMMA) {
                    controller.toPhase(Phase.ELEMENTS_RESOLVED);
                    env.addToExcludes(controller.getTrees().getElement(path));
                    addTypes(env, EnumSet.of(INTERFACE, ANNOTATION_TYPE), null);
                }
                return;
            }
            Tree ext = cls.getExtendsClause();
            if (ext != null) {
                int extPos = (int)sourcePositions.getEndPosition(root, ext);
                if (extPos != Diagnostic.NOPOS && offset > extPos) {
                    TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, extPos + 1, offset);
                    if (last != null && last.token().id() == JavaTokenId.IMPLEMENTS) {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        env.addToExcludes(controller.getTrees().getElement(path));
                        addTypes(env, EnumSet.of(INTERFACE, ANNOTATION_TYPE), null);
                    } else {
                        addKeyword(env, IMPLEMENTS_KEYWORD, SPACE, false);
                    }
                    return;
                }
            }
            TypeParameterTree lastTypeParam = null;
            for (TypeParameterTree tp : cls.getTypeParameters()) {
                int tpPos = (int)sourcePositions.getEndPosition(root, tp);
                if (tpPos == Diagnostic.NOPOS || offset <= tpPos)
                    break;
                lastTypeParam = tp;
                startPos = tpPos;
            }
            if (lastTypeParam != null) {
                TokenSequence<JavaTokenId> first = findFirstNonWhitespaceToken(env, startPos, offset);
                if (first != null && first.token().id() == JavaTokenId.GT) {
                    first = nextNonWhitespaceToken(first);
                    if (first != null && first.offset() < offset) {
                        if (first.token().id() == JavaTokenId.EXTENDS) {
                            controller.toPhase(Phase.ELEMENTS_RESOLVED);
                            env.afterExtends();
                            env.addToExcludes(controller.getTrees().getElement(path));
                            addTypes(env, tu.isInterface(cls) ? EnumSet.of(INTERFACE, ANNOTATION_TYPE) : EnumSet.of(CLASS), null);
                            return;
                        }
                        if (first.token().id() == JavaTokenId.IMPLEMENTS) {
                            controller.toPhase(Phase.ELEMENTS_RESOLVED);
                            env.addToExcludes(controller.getTrees().getElement(path));
                            addTypes(env, EnumSet.of(INTERFACE, ANNOTATION_TYPE), null);
                            return;
                        }
                    }
                    if (!tu.isAnnotation(cls)) {
                        if (!tu.isEnum(cls))
                            addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                        if (!tu.isInterface(cls))
                            addKeyword(env, IMPLEMENTS_KEYWORD, SPACE, false);
                    }
                } else {
                    if (lastTypeParam.getBounds().isEmpty()) {
                        addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                    }
                }
                return;
            }            
            TokenSequence<JavaTokenId> lastNonWhitespaceToken = findLastNonWhitespaceToken(env, startPos, offset);
            if (lastNonWhitespaceToken != null) {
                switch (lastNonWhitespaceToken.token().id()) {
                    case EXTENDS:
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        env.afterExtends();
                        env.addToExcludes(controller.getTrees().getElement(path));
                        addTypes(env, tu.isInterface(cls) ? EnumSet.of(INTERFACE, ANNOTATION_TYPE) : EnumSet.of(CLASS), null);
                        break;
                    case IMPLEMENTS:
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        env.addToExcludes(controller.getTrees().getElement(path));
                        addTypes(env, EnumSet.of(INTERFACE, ANNOTATION_TYPE), null);
                        break;
                    case IDENTIFIER:
                        if (!tu.isAnnotation(cls)) {
                            if (!tu.isEnum(cls))
                                addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                            if (!tu.isInterface(cls))
                                addKeyword(env, IMPLEMENTS_KEYWORD, SPACE, false);
                        }
                        break;
                }
                return;
            }
            lastNonWhitespaceToken = findLastNonWhitespaceToken(env, (int)sourcePositions.getStartPosition(root, cls), offset);
            if (lastNonWhitespaceToken != null && lastNonWhitespaceToken.token().id() == JavaTokenId.AT) {
                addKeyword(env, INTERFACE_KEYWORD, SPACE, false);
                addTypes(env, EnumSet.of(ANNOTATION_TYPE), null);
            } else if (path.getParentPath().getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                addClassModifiers(env, cls.getModifiers().getFlags());
            } else {
                addMemberModifiers(env, cls.getModifiers().getFlags(), false);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
            }
        }
        
        private void insideVariable(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            VariableTree var = (VariableTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            Tree type = var.getType();
            int typePos = type.getKind() == Tree.Kind.ERRONEOUS && ((ErroneousTree)type).getErrorTrees().isEmpty() ?
                (int)sourcePositions.getEndPosition(root, type) : (int)sourcePositions.getStartPosition(root, type);            
            if (offset <= typePos) {
                Tree parent = path.getParentPath().getLeaf();
                if (parent.getKind() == Tree.Kind.CATCH) {
                    CompilationController controller = env.getController();
                    if (queryType == COMPLETION_QUERY_TYPE) {
                        TreePath tryPath = Utilities.getPathElementOfKind(Tree.Kind.TRY, path);
                        Set<TypeMirror> exs = controller.getTreeUtilities().getUncaughtExceptions(tryPath);
                        Elements elements = controller.getElements();
                        for (TypeMirror ex : exs)
                            if (ex.getKind() == TypeKind.DECLARED && startsWith(env, ((DeclaredType)ex).asElement().getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(((DeclaredType)ex).asElement())))
                                results.add(JavaCompletionItem.createTypeItem(controller, (TypeElement)((DeclaredType)ex).asElement(), (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(((DeclaredType)ex).asElement()), false, false, false, true, false, env.getWhiteList()));
                    }
                    TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                    if (te != null)
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
                } else if (parent.getKind() == Tree.Kind.TRY) {
                    CompilationController controller = env.getController();
                    TypeElement te = controller.getElements().getTypeElement("java.lang.AutoCloseable"); //NOI18N
                    if (te != null)
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
                } else {
                    boolean isLocal = !TreeUtilities.CLASS_TREE_KINDS.contains(parent.getKind());
                    addMemberModifiers(env, var.getModifiers().getFlags(), isLocal);
                    addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                    ModifiersTree mods = var.getModifiers();
                    if (mods.getFlags().isEmpty() && mods.getAnnotations().isEmpty())
                        addElementCreators(env);
                }
                return;
            }
            Tree init = unwrapErrTree(var.getInitializer());
            if (init == null) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, (int)sourcePositions.getEndPosition(root, type), offset);
                if (last == null || last.token().id() == JavaTokenId.COMMA) {
                    insideExpression(env, new TreePath(path, type));
                } else if (last.token().id() == JavaTokenId.EQ) {
                    localResult(env);
                    addValueKeywords(env);
                }
            } else {
                int pos = (int)sourcePositions.getStartPosition(root, init);
                if (pos < 0)
                    return;
                if (offset <= pos) {
                    TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, (int)sourcePositions.getEndPosition(root, type), offset);
                    if (last == null) {
                        insideExpression(env, new TreePath(path, type));
                    } else if (last.token().id() == JavaTokenId.EQ) {
                        localResult(env);
                        addValueKeywords(env);
                    }
                } else {
                    insideExpression(env, new TreePath(path, init));
                }
            }
        }
        
        private void insideMethod(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            MethodTree mth = (MethodTree)path.getLeaf();
            CompilationController controller = env.getController();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int startPos = (int)sourcePositions.getStartPosition(root, mth);
            Tree lastTree = null;
            int state = 0;
            for (Tree thr: mth.getThrows()) {
                int thrPos = (int)sourcePositions.getEndPosition(root, thr);
                if (thrPos == Diagnostic.NOPOS || offset <= thrPos)
                    break;
                lastTree = thr;
                startPos = thrPos;
                state = 4;
            }
            if (lastTree == null) {
                for (VariableTree param : mth.getParameters()) {
                    int parPos = (int)sourcePositions.getEndPosition(root, param);
                    if (parPos == Diagnostic.NOPOS || offset <= parPos)
                        break;
                    lastTree = param;
                    startPos = parPos;
                    state = 3;
                }
            }
            if (lastTree == null) {
                Tree retType = mth.getReturnType();
                if (retType != null) {
                    int retPos = (int)sourcePositions.getEndPosition(root, retType);
                    if (retPos != Diagnostic.NOPOS && offset > retPos) {
                        lastTree = retType;
                        startPos = retPos;
                        state = 2;
                    }
                }
            }
            if (lastTree == null) {
                for (TypeParameterTree tp : mth.getTypeParameters()) {
                    int tpPos = (int)sourcePositions.getEndPosition(root, tp);
                    if (tpPos == Diagnostic.NOPOS || offset <= tpPos)
                        break;
                    lastTree = tp;
                    startPos = tpPos;
                    state = 1;
                }
            }
            if (lastTree == null) {
                Tree mods = mth.getModifiers();
                if (mods != null) {
                    int modsPos = (int)sourcePositions.getEndPosition(root, mods);
                    if (modsPos != Diagnostic.NOPOS && offset > modsPos) {
                        lastTree = mods;
                        startPos = modsPos;
                    }
                }
            }
            TokenSequence<JavaTokenId> lastToken = findLastNonWhitespaceToken(env, startPos, offset);
            if (lastToken != null) {
                switch (lastToken.token().id()) {
                    case LPAREN:
                        addMemberModifiers(env, Collections.<Modifier>emptySet(), true);
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                        break;
                    case RPAREN:
                        Tree mthParent = path.getParentPath().getLeaf();
                        switch (mthParent.getKind()) {
                            case ANNOTATION_TYPE:
                                addKeyword(env, DEFAULT_KEYWORD, SPACE, false);
                                break;
                            default:
                                addKeyword(env, THROWS_KEYWORD, SPACE, false);
                        }
                        break;
                    case THROWS:
                        if (queryType == COMPLETION_QUERY_TYPE && mth.getBody() != null) {
                            controller.toPhase(Phase.RESOLVED);
                            Set<TypeMirror> exs = controller.getTreeUtilities().getUncaughtExceptions(new TreePath(path, mth.getBody()));
                            Elements elements = controller.getElements();
                            for (TypeMirror ex : exs)
                                if (ex.getKind() == TypeKind.DECLARED && startsWith(env, ((DeclaredType)ex).asElement().getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(((DeclaredType)ex).asElement())))
                                    results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)((DeclaredType)ex).asElement(), (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(((DeclaredType)ex).asElement()), false, false, false, true, false, env.getWhiteList()));
                        }
                        TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                        if (te != null)
                            addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
                        break;
                    case DEFAULT:
                        addLocalConstantsAndTypes(env);
                        break;
                    case GT:
                    case GTGT:
                    case GTGTGT:
                        addPrimitiveTypeKeywords(env);
                        addKeyword(env, VOID_KEYWORD, SPACE, false);
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                        break;
                    case COMMA:
                        switch (state) {
                            case 3:
                                addMemberModifiers(env, Collections.<Modifier>emptySet(), true);
                                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                                break;
                            case 4:
                                if (queryType == COMPLETION_QUERY_TYPE && mth.getBody() != null) {
                                    controller.toPhase(Phase.RESOLVED);
                                    Set<TypeMirror> exs = controller.getTreeUtilities().getUncaughtExceptions(new TreePath(path, mth.getBody()));
                                    Trees trees = controller.getTrees();
                                    Types types = controller.getTypes();
                                    for (ExpressionTree thr : mth.getThrows()) {
                                        TypeMirror t = trees.getTypeMirror(new TreePath(path, thr));
                                        for (Iterator<TypeMirror> it = exs.iterator(); it.hasNext();)
                                            if (types.isSubtype(it.next(), t))
                                                it.remove();
                                        if (thr == lastTree)
                                            break;
                                    }
                                    Elements elements = controller.getElements();
                                    for (TypeMirror ex : exs)
                                        if (ex.getKind() == TypeKind.DECLARED && startsWith(env, ((DeclaredType)ex).asElement().getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(((DeclaredType)ex).asElement())))
                                            results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)((DeclaredType)ex).asElement(), (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(((DeclaredType)ex).asElement()), false, false, false, true, false, env.getWhiteList()));
                                }
                                te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                                if (te != null)
                                    addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
                                break;
                        }
                        break;
                }
                return;
            }
            switch (state) {
                case 0:
                    addMemberModifiers(env, mth.getModifiers().getFlags(), false);
                    addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                    break;
                case 1:
                    if (((TypeParameterTree)lastTree).getBounds().isEmpty()) {
                        addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                    }
                    break;
                case 2:
                    insideExpression(env, new TreePath(path, lastTree));
                    break;
            }
        }
        
        private void insideModifiers(Env env, TreePath modPath) throws IOException {
            int offset = env.getOffset();
            ModifiersTree mods = (ModifiersTree)modPath.getLeaf();
            Set<Modifier> m = EnumSet.noneOf(Modifier.class);
            TokenSequence<JavaTokenId> ts = env.getController().getTreeUtilities().tokensFor(mods, env.getSourcePositions());
            JavaTokenId lastNonWhitespaceTokenId = null;
            while(ts.moveNext() && ts.offset() < offset) {
                lastNonWhitespaceTokenId = ts.token().id();
                switch (lastNonWhitespaceTokenId) {
                    case PUBLIC:
                        m.add(PUBLIC);
                        break;
                    case PROTECTED:
                        m.add(PROTECTED);
                        break;
                    case PRIVATE:
                        m.add(PRIVATE);
                        break;
                    case STATIC:
                        m.add(STATIC);
                        break;
                    case ABSTRACT:
                        m.add(ABSTRACT);
                        break;
                    case FINAL:
                        m.add(FINAL);
                        break;
                    case SYNCHRONIZED:
                        m.add(SYNCHRONIZED);
                        break;
                    case NATIVE:
                        m.add(NATIVE);
                        break;
                    case STRICTFP:
                        m.add(STRICTFP);
                        break;
                    case TRANSIENT:
                        m.add(TRANSIENT);
                        break;
                    case VOLATILE:
                        m.add(VOLATILE);
                        break;
                }                
            }
            if (lastNonWhitespaceTokenId == JavaTokenId.AT) {
                addKeyword(env, INTERFACE_KEYWORD, SPACE, false);
                addTypes(env, EnumSet.of(ANNOTATION_TYPE), null);
                return;
            }            
            TreePath parentPath = modPath.getParentPath();
            Tree parent = parentPath.getLeaf();
            TreePath grandParentPath = parentPath.getParentPath();
            Tree grandParent = grandParentPath != null ? grandParentPath.getLeaf() : null;
            if (isTopLevelClass(parent, env.getRoot())) {
                addClassModifiers(env, m);
            } else if (parent.getKind() != Tree.Kind.VARIABLE || grandParent == null || TreeUtilities.CLASS_TREE_KINDS.contains(grandParent.getKind())) {
                addMemberModifiers(env, m, false);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
            } else if (parent.getKind() == Tree.Kind.VARIABLE && grandParent.getKind() == Tree.Kind.METHOD) {
                addMemberModifiers(env, m, true);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
            } else {
                localResult(env);
                addKeywordsForBlock(env);
            }
        }
        
        private void insideAnnotation(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            AnnotationTree ann = (AnnotationTree)path.getLeaf();
            CompilationController controller = env.getController();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int typeEndPos = (int)sourcePositions.getEndPosition(root, ann.getAnnotationType());
            if (offset <= typeEndPos) {                
                TreePath parentPath = path.getParentPath();
                if (parentPath.getLeaf().getKind() == Tree.Kind.MODIFIERS)
                    addKeyword(env, INTERFACE_KEYWORD, SPACE, false);
                if (queryType == CompletionProvider.COMPLETION_QUERY_TYPE) {
                    controller.toPhase(Phase.ELEMENTS_RESOLVED);
                    Set<? extends TypeMirror> smarts = env.getSmartTypes();
                    if (smarts != null) {
                        Elements elements = controller.getElements();
                        for (TypeMirror smart : smarts) {
                            if (smart.getKind() == TypeKind.DECLARED) {
                                TypeElement elem = (TypeElement)((DeclaredType)smart).asElement();
                                if (elem.getKind() == ANNOTATION_TYPE && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)))
                                    results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, (DeclaredType)smart, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), false, false, false, true, false, env.getWhiteList()));
                            }
                        }
                    }
                }
                addTypes(env, EnumSet.of(ANNOTATION_TYPE), null);
                return;
            }
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, ann, offset);
            if (ts == null || (ts.token().id() != JavaTokenId.LPAREN && ts.token().id() != JavaTokenId.COMMA))
                return;
            controller.toPhase(Phase.ELEMENTS_RESOLVED);
            Trees trees = controller.getTrees();
            Element annTypeElement = trees.getElement(new TreePath(path, ann.getAnnotationType()));
            if (annTypeElement != null && annTypeElement.getKind() == ANNOTATION_TYPE) {
                HashSet<String> names = new HashSet<String>();
                for(ExpressionTree arg : ann.getArguments()) {
                    if (arg.getKind() == Tree.Kind.ASSIGNMENT && sourcePositions.getEndPosition(root, arg) < offset) {
                        ExpressionTree var = ((AssignmentTree)arg).getVariable();
                        if (var.getKind() == Tree.Kind.IDENTIFIER)
                            names.add(((IdentifierTree)var).getName().toString());
                    }
                }
                Elements elements = controller.getElements();
                ExecutableElement valueElement = null;
                for(Element e : ((TypeElement)annTypeElement).getEnclosedElements()) {
                    if (e.getKind() == METHOD) {
                        String name = e.getSimpleName().toString();
                        if ("value".equals(name)) { //NOI18N
                            valueElement = (ExecutableElement)e;
                        } else if (((ExecutableElement)e).getDefaultValue() == null) {
                            valueElement = null;
                        }
                        if (!names.contains(name) && startsWith(env, name) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)))
                            results.add(JavaCompletionItem.createAttributeItem(env.getController(), (ExecutableElement)e, (ExecutableType)e.asType(), anchorOffset, elements.isDeprecated(e)));
                    }
                }
                if (valueElement != null && names.isEmpty()) {
                    Element el = null;
                    TreePath pPath = path.getParentPath();
                    if (pPath.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                        el = trees.getElement(pPath);
                    } else {
                        pPath = pPath.getParentPath();
                        Tree.Kind pKind = pPath.getLeaf().getKind();
                        if (TreeUtilities.CLASS_TREE_KINDS.contains(pKind) || pKind == Tree.Kind.METHOD || pKind == Tree.Kind.VARIABLE) {
                            el = trees.getElement(pPath);
                        }
                    }
                    if (el != null) {
                        AnnotationMirror annotation = null;
                        for (AnnotationMirror am : el.getAnnotationMirrors()) {
                            if (annTypeElement == am.getAnnotationType().asElement()) {
                                annotation = am;
                                break;
                            }
                        }
                        if (annotation != null)
                            addAttributeValues(env, el, annotation, valueElement);
                    }
                    addLocalConstantsAndTypes(env);
                }
            }
        }

        private void insideAnnotationAttribute(Env env, TreePath annotationPath, Name attributeName) throws IOException {
            CompilationController controller = env.getController();
            controller.toPhase(Phase.ELEMENTS_RESOLVED);
            Trees trees = controller.getTrees();
            AnnotationTree at = (AnnotationTree)annotationPath.getLeaf();
            Element annTypeElement = trees.getElement(new TreePath(annotationPath, at.getAnnotationType()));
            Element el = null;
            TreePath pPath = annotationPath.getParentPath();
            if (pPath.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                el = trees.getElement(pPath);
            } else {
                pPath = pPath.getParentPath();
                Tree.Kind pKind = pPath.getLeaf().getKind();
                if (TreeUtilities.CLASS_TREE_KINDS.contains(pKind) || pKind == Tree.Kind.METHOD || pKind == Tree.Kind.VARIABLE) {
                    el = trees.getElement(pPath);
                }
            }
            if (el != null && annTypeElement != null && annTypeElement.getKind() == ANNOTATION_TYPE) {
                ExecutableElement memberElement = null;
                for (Element e : ((TypeElement)annTypeElement).getEnclosedElements()) {
                    if (e.getKind() == METHOD && attributeName.contentEquals(e.getSimpleName())) {
                        memberElement = (ExecutableElement)e;
                        break;
                    }
                }
                if (memberElement != null) {
                    AnnotationMirror annotation = null;
                    for (AnnotationMirror am : el.getAnnotationMirrors()) {
                        if (annTypeElement == am.getAnnotationType().asElement()) {
                            annotation = am;
                            break;
                        }
                    }
                    if (annotation != null)
                        addAttributeValues(env, el, annotation, memberElement);
                }
            }
        }
        
        private void insideTypeParameter(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            TypeParameterTree tp = (TypeParameterTree)path.getLeaf();
            CompilationController controller = env.getController();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, tp, offset);
            if (ts != null) {
                switch(ts.token().id()) {
                    case EXTENDS:
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        env.addToExcludes(controller.getTrees().getElement(path.getParentPath()));
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ANNOTATION_TYPE), null);
                        break;
                    case AMP:
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);
                        env.addToExcludes(controller.getTrees().getElement(path.getParentPath()));
                        addTypes(env, EnumSet.of(INTERFACE, ANNOTATION_TYPE), null);
                        break;
                    case IDENTIFIER:
                        if (ts.offset() == env.getSourcePositions().getStartPosition(env.getRoot(), tp))
                            addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                        break;
                }
            }
        }
        
        private void insideParameterizedType(Env env, TreePath ptPath) throws IOException {
            int offset = env.getOffset();
            ParameterizedTypeTree ta = (ParameterizedTypeTree)ptPath.getLeaf();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, ta, offset);
            if (ts != null) {
                switch (ts.token().id()) {
                    case EXTENDS:
                    case SUPER:
                    case LT:
                    case COMMA:
                        if (queryType == COMPLETION_QUERY_TYPE) {
                            CompilationController controller = env.getController();
                            SourcePositions sourcePositions = env.getSourcePositions();
                            CompilationUnitTree root = env.getRoot();
                            int index = 0;
                            for (Tree arg : ta.getTypeArguments()) {
                                int parPos = (int)sourcePositions.getEndPosition(root, arg);
                                if (parPos == Diagnostic.NOPOS || offset <= parPos)
                                    break;
                                index++;
                            }
                            Elements elements = controller.getElements();
                            Types types = controller.getTypes();
                            TypeMirror tm = controller.getTrees().getTypeMirror(new TreePath(ptPath, ta.getType()));
                            List<? extends TypeMirror> bounds = null;
                            if (tm.getKind() == TypeKind.DECLARED) {
                                TypeElement te = (TypeElement)((DeclaredType)tm).asElement();
                                List<? extends TypeParameterElement> typeParams = te.getTypeParameters();
                                if (index < typeParams.size()) {
                                    TypeParameterElement typeParam = typeParams.get(index);
                                    bounds = typeParam.getBounds();
                                }
                            }
                            Set<? extends TypeMirror> smarts = env.getSmartTypes();
                            if (smarts != null) {
                                for (TypeMirror smart : smarts) {
                                    if (smart != null) {
                                        if (smart.getKind() == TypeKind.DECLARED && types.isSubtype(tm, types.erasure(smart))) {
                                            List<? extends TypeMirror> typeArgs = ((DeclaredType)smart).getTypeArguments();
                                            if (index < typeArgs.size()) {
                                                TypeMirror lowerBound = typeArgs.get(index);
                                                TypeMirror upperBound = null;
                                                if (lowerBound.getKind() == TypeKind.WILDCARD) {
                                                    upperBound = ((WildcardType)lowerBound).getSuperBound();
                                                    lowerBound = ((WildcardType)lowerBound).getExtendsBound();
                                                }
                                                if (lowerBound != null && lowerBound.getKind() == TypeKind.TYPEVAR) {
                                                    lowerBound = ((TypeVariable)lowerBound).getUpperBound();
                                                }
                                                if (upperBound != null && upperBound.getKind() == TypeKind.TYPEVAR) {
                                                    upperBound = ((TypeVariable)upperBound).getUpperBound();
                                                }
                                                if (upperBound != null && upperBound.getKind() == TypeKind.DECLARED) {
                                                    while(upperBound.getKind() == TypeKind.DECLARED) {
                                                        TypeElement elem = (TypeElement)((DeclaredType)upperBound).asElement();
                                                        if (startsWith(env, elem.getSimpleName().toString()) && withinBounds(env, upperBound, bounds)
                                                                && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)))
                                                            results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, (DeclaredType)upperBound, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), false, true, false, true, false, env.getWhiteList()));
                                                        env.addToExcludes(elem);
                                                        upperBound = elem.getSuperclass();
                                                    }
                                                } else if (lowerBound != null && lowerBound.getKind() == TypeKind.DECLARED) {
                                                    for (DeclaredType subtype : getSubtypesOf(env, (DeclaredType)lowerBound)) {
                                                        TypeElement elem = (TypeElement)subtype.asElement();
                                                        if (withinBounds(env, subtype, bounds) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)))
                                                            results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, subtype, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), false, true, false, true, false, env.getWhiteList()));
                                                        env.addToExcludes(elem);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (bounds != null && !bounds.isEmpty()) {
                                TypeMirror lowerBound = bounds.get(0);
                                bounds = bounds.subList(0, bounds.size());
                                for (DeclaredType subtype : getSubtypesOf(env, (DeclaredType)lowerBound)) {
                                    TypeElement elem = (TypeElement)subtype.asElement();
                                    if (withinBounds(env, subtype, bounds) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)))
                                        results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, subtype, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), false, true, false, true, false, env.getWhiteList()));
                                    env.addToExcludes(elem);
                                }
                            }
                        }
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                        break;
                    case QUESTION:
                        addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                        addKeyword(env, SUPER_KEYWORD, SPACE, false);
                        break;
                }
            }
        }
        
        private void insideBlock(Env env) throws IOException {
            int offset = env.getOffset();
            BlockTree bl = (BlockTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int blockPos = (int)sourcePositions.getStartPosition(root, bl);
            String text = env.getController().getText().substring(blockPos, offset);
            if (text.indexOf('{') < 0) { //NOI18N
                addMemberModifiers(env, Collections.singleton(STATIC), false);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                return;
            }
            StatementTree last = null;
            for(StatementTree stat : bl.getStatements()) {
                int pos = (int)sourcePositions.getStartPosition(root, stat);
                if (pos == Diagnostic.NOPOS || offset <= pos)
                    break;
                last = stat;
            }
            if (last == null) {
                ExecutableElement enclMethod = env.getScope().getEnclosingMethod();
                if (enclMethod != null && enclMethod.getKind() == ElementKind.CONSTRUCTOR) {
                    String prefix = env.getPrefix();
                    if (Utilities.startsWith(THIS_KEYWORD, prefix)) {
                        Element element = enclMethod.getEnclosingElement();
                        addThisOrSuperConstructor(env, element.asType(), element, THIS_KEYWORD, enclMethod);
                    }
                    if (Utilities.startsWith(SUPER_KEYWORD, prefix)) {
                        Element element = enclMethod.getEnclosingElement();
                        element = ((DeclaredType)((TypeElement)element).getSuperclass()).asElement();
                        addThisOrSuperConstructor(env, element.asType(), element, SUPER_KEYWORD, enclMethod);
                    }
                }
            } else if (last.getKind() == Tree.Kind.TRY) {
                if (((TryTree)last).getFinallyBlock() == null) {
                    addKeyword(env, CATCH_KEYWORD, null, false);
                    addKeyword(env, FINALLY_KEYWORD, null, false);
                    if (((TryTree)last).getCatches().size() == 0)
                        return;
                }
            } else if (last.getKind() == Tree.Kind.IF) {
                if (((IfTree)last).getElseStatement() == null)
                    addKeyword(env, ELSE_KEYWORD, null, false);
            }
            localResult(env);
            addKeywordsForBlock(env);
        }
        
        private void insideMemberSelect(Env env) throws IOException {
            int offset = env.getOffset();
            String prefix = env.getPrefix();
            TreePath path = env.getPath();
            MemberSelectTree fa = (MemberSelectTree)path.getLeaf();
            CompilationController controller = env.getController();
            CompilationUnitTree root = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            int expEndPos = (int)sourcePositions.getEndPosition(root, fa.getExpression());
            boolean afterDot = false;
            boolean afterLt = false;
            int openLtNum = 0;
            JavaTokenId lastNonWhitespaceTokenId = null;
            TokenSequence<JavaTokenId> ts = controller.getTokenHierarchy().tokenSequence(JavaTokenId.language());
            ts.move(expEndPos);
            while (ts.moveNext()) {
                if (ts.offset() >= offset) {
                    break;
                }
                switch (ts.token().id()) {
                    case DOUBLE_LITERAL:
                    case FLOAT_LITERAL:
                    case FLOAT_LITERAL_INVALID:
                    case LONG_LITERAL:
                    case ELLIPSIS:
                        if (ts.offset() != expEndPos || ts.token().text().charAt(0) != '.')
                            break;
                    case DOT:
                        afterDot = true;
                        break;
                    case LT:
                        afterLt = true;
                        openLtNum++;
                        break;
                    case GT:
                        openLtNum--;
                        break;
                    case GTGT:
                        openLtNum -= 2;
                        break;
                    case GTGTGT:
                        openLtNum -= 3;
                        break;
                }
                switch (ts.token().id()) {
                    case WHITESPACE:
                    case LINE_COMMENT:
                    case BLOCK_COMMENT:
                    case JAVADOC_COMMENT:
                        break;
                    default:
                        lastNonWhitespaceTokenId = ts.token().id();
                }
            }
            if (!afterDot) {
                if (expEndPos <= offset)
                    insideExpression(env, new TreePath(path, fa.getExpression()));
                return;
            }
            if (openLtNum > 0) {
                switch (lastNonWhitespaceTokenId) {
                    case QUESTION:
                        addKeyword(env, EXTENDS_KEYWORD, SPACE, false);
                        addKeyword(env, SUPER_KEYWORD, SPACE, false);
                        break;
                    case LT:
                    case COMMA:
                    case EXTENDS:
                    case SUPER:
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                        break;
                }
            } else if (lastNonWhitespaceTokenId != JavaTokenId.STAR) {
                controller.toPhase(Phase.RESOLVED);
                TreePath parentPath = path.getParentPath();
                Tree parent = parentPath != null ? parentPath.getLeaf() : null;
                TreePath grandParentPath = parentPath != null ? parentPath.getParentPath() : null;
                Tree grandParent = grandParentPath != null ? grandParentPath.getLeaf() : null;
                ExpressionTree exp = fa.getExpression();
                TreePath expPath = new TreePath(path, exp);
                TypeMirror type = controller.getTrees().getTypeMirror(expPath);
                if (type != null) {
                    EnumSet<ElementKind> kinds;
                    DeclaredType baseType = null;
                    Set<TypeMirror> exs = null;
                    boolean inImport = false;
                    boolean insideNew = false;
                    if (TreeUtilities.CLASS_TREE_KINDS.contains(parent.getKind()) && ((ClassTree)parent).getExtendsClause() == fa) {
                        kinds = EnumSet.of(CLASS);
                        env.afterExtends();
                    } else if (TreeUtilities.CLASS_TREE_KINDS.contains(parent.getKind()) && ((ClassTree)parent).getImplementsClause().contains(fa)) {
                        kinds = EnumSet.of(INTERFACE);
                    } else if (parent.getKind() == Tree.Kind.IMPORT) {
                        inImport = true;
                        kinds = ((ImportTree)parent).isStatic() ? EnumSet.of(CLASS, ENUM, INTERFACE, ANNOTATION_TYPE, FIELD, METHOD, ENUM_CONSTANT) : EnumSet.of(CLASS, ANNOTATION_TYPE, ENUM, INTERFACE);
                    } else if (parent.getKind() == Tree.Kind.NEW_CLASS && ((NewClassTree)parent).getIdentifier() == fa) {
                        insideNew = true;
                        kinds = EnumSet.of(CLASS, INTERFACE, ANNOTATION_TYPE);
                        if (grandParent.getKind() == Tree.Kind.THROW) {
                            TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                            if (te != null)
                                baseType = controller.getTypes().getDeclaredType(te);
                        }
                    } else if (parent.getKind() == Tree.Kind.PARAMETERIZED_TYPE && ((ParameterizedTypeTree)parent).getTypeArguments().contains(fa)) {
                        kinds = EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE);
                    } else if (parent.getKind() == Tree.Kind.ANNOTATION) {
                        if (((AnnotationTree)parent).getAnnotationType() == fa) {
                            kinds = EnumSet.of(ANNOTATION_TYPE);
                        } else {
                            Iterator<? extends ExpressionTree> it = ((AnnotationTree)parent).getArguments().iterator();
                            if (it.hasNext()) {
                                ExpressionTree et = it.next();
                                if (et == fa || (et.getKind() == Tree.Kind.ASSIGNMENT && ((AssignmentTree)et).getExpression() == fa)) {
                                    Element el = controller.getTrees().getElement(expPath);
                                    if (type.getKind() == TypeKind.ERROR && el.getKind().isClass()) {
                                        el = controller.getElements().getPackageElement(((TypeElement)el).getQualifiedName());
                                    }
                                    if (el instanceof PackageElement)
                                        addPackageContent(env, (PackageElement)el, EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE), null, false, false);
                                    else if (type.getKind() == TypeKind.DECLARED)
                                        addMemberConstantsAndTypes(env, (DeclaredType)type, el);
                                    return;
                                }
                            }
                            kinds = EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE, FIELD, METHOD, ENUM_CONSTANT);
                        }
                    } else if (parent.getKind() == Tree.Kind.ASSIGNMENT && ((AssignmentTree)parent).getExpression() == fa && grandParent != null && grandParent.getKind() == Tree.Kind.ANNOTATION) {
                        Element el = controller.getTrees().getElement(expPath);
                        if (type.getKind() == TypeKind.ERROR && el.getKind().isClass()) {
                            el = controller.getElements().getPackageElement(((TypeElement)el).getQualifiedName());
                        }
                        if (el instanceof PackageElement)
                            addPackageContent(env, (PackageElement)el, EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE), null, false, false);
                        else if (type.getKind() == TypeKind.DECLARED)
                            addMemberConstantsAndTypes(env, (DeclaredType)type, el);
                        return;
                    } else if (parent.getKind() == Tree.Kind.VARIABLE && ((VariableTree)parent).getType() == fa) {
                        if (grandParent.getKind() == Tree.Kind.CATCH) {
                            kinds = EnumSet.of(CLASS, INTERFACE);
                            if (queryType == COMPLETION_QUERY_TYPE)
                                exs = controller.getTreeUtilities().getUncaughtExceptions(grandParentPath.getParentPath());
                            TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                            if (te != null)
                                baseType = controller.getTypes().getDeclaredType(te);
                        } else {
                            kinds = EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE);
                        }
                    } else if (parent.getKind() == Tree.Kind.METHOD && ((MethodTree)parent).getThrows().contains(fa)) {
                        Types types = controller.getTypes();
                        if (queryType == COMPLETION_QUERY_TYPE && ((MethodTree)parent).getBody() != null) {
                            controller.toPhase(Phase.RESOLVED);
                            exs = controller.getTreeUtilities().getUncaughtExceptions(new TreePath(path, ((MethodTree)parent).getBody()));
                            Trees trees = controller.getTrees();
                            for (ExpressionTree thr : ((MethodTree)parent).getThrows()) {
                                if (sourcePositions.getEndPosition(root, thr) >= offset)
                                    break;
                                TypeMirror t = trees.getTypeMirror(new TreePath(path, thr));
                                for (Iterator<TypeMirror> it = exs.iterator(); it.hasNext();)
                                    if (types.isSubtype(it.next(), t))
                                        it.remove();
                            }
                        }
                        kinds = EnumSet.of(CLASS, INTERFACE);
                        TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                        if (te != null)
                            baseType = controller.getTypes().getDeclaredType(te);
                    } else if (parent.getKind() == Tree.Kind.METHOD && ((MethodTree)parent).getDefaultValue() == fa) {
                        Element el = controller.getTrees().getElement(expPath);
                        if (type.getKind() == TypeKind.ERROR && el.getKind().isClass()) {
                            el = controller.getElements().getPackageElement(((TypeElement)el).getQualifiedName());
                        }
                        if (el instanceof PackageElement)
                            addPackageContent(env, (PackageElement)el, EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE), null, false, false);
                        else if (type.getKind() == TypeKind.DECLARED)
                            addMemberConstantsAndTypes(env, (DeclaredType)type, el);
                        return;
                    } else if (afterLt) {
                        kinds = EnumSet.of(METHOD);
                    } else if (parent.getKind() == Tree.Kind.ENHANCED_FOR_LOOP && ((EnhancedForLoopTree)parent).getExpression() == fa) {
                        env.insideForEachExpression();
                        kinds = EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE, FIELD, METHOD, ENUM_CONSTANT);
                    } else {
                        kinds = EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE, FIELD, METHOD, ENUM_CONSTANT);
                    }
                    switch (type.getKind()) {
                        case TYPEVAR:
                            while(type != null && type.getKind() == TypeKind.TYPEVAR)
                                type = ((TypeVariable)type).getUpperBound();
                            if (type == null)
                                return;
                            type = controller.getTypes().capture(type);
                        case ARRAY:
                        case DECLARED:
                        case UNION:
                        case BOOLEAN:
                        case BYTE:
                        case CHAR:
                        case DOUBLE:
                        case FLOAT:
                        case INT:
                        case LONG:
                        case SHORT:
                        case VOID:
                            boolean b = exp.getKind() == Tree.Kind.PARENTHESIZED || exp.getKind() == Tree.Kind.TYPE_CAST;
                            while(b) {
                                if (exp.getKind() == Tree.Kind.PARENTHESIZED) {
                                    exp = ((ParenthesizedTree)exp).getExpression();
                                    expPath = new TreePath(expPath, exp);
                                } else if (exp.getKind() == Tree.Kind.TYPE_CAST) {
                                    exp = ((TypeCastTree)exp).getExpression();
                                    expPath = new TreePath(expPath, exp);
                                } else {
                                    b = false;
                                }
                            }
                            Element el = controller.getTrees().getElement(expPath);
                            if (el != null && (el.getKind().isClass() || el.getKind().isInterface())) {
                                if (parent.getKind() == Tree.Kind.NEW_CLASS && ((NewClassTree)parent).getIdentifier() == fa && prefix != null) {
                                    String typeName = Utilities.getElementName(el, true) + "." + prefix; //NOI18N
                                    TypeMirror tm = controller.getTreeUtilities().parseType(typeName, env.getScope().getEnclosingClass());
                                    if (tm != null && tm.getKind() == TypeKind.DECLARED)
                                        addMembers(env, tm, ((DeclaredType)tm).asElement(), EnumSet.of(CONSTRUCTOR), null, inImport, insideNew, false);
                                }
                            }
                            if (exs != null && !exs.isEmpty()) {
                                Elements elements = controller.getElements();
                                for (TypeMirror ex : exs)
                                    if (ex.getKind() == TypeKind.DECLARED) {
                                        Element e = ((DeclaredType)ex).asElement();
                                        if (e.getEnclosingElement() == el && startsWith(env, e.getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)))
                                            results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, (DeclaredType)ex, anchorOffset, null, elements.isDeprecated(e), insideNew, insideNew || env.isInsideClass(), true, true, false, env.getWhiteList()));
                                    }
                            } else {
                                if (el == null) {
                                    if (exp.getKind() == Tree.Kind.ARRAY_TYPE) {
                                        TypeMirror tm = type;
                                        while (tm.getKind() == TypeKind.ARRAY)
                                            tm = ((ArrayType)tm).getComponentType();
                                        if (tm.getKind().isPrimitive())
                                            el = controller.getTypes().boxedClass((PrimitiveType)tm);
                                        else if (tm.getKind() == TypeKind.DECLARED)
                                            el = ((DeclaredType)tm).asElement();
                                    } else if (exp.getKind() == Tree.Kind.PRIMITIVE_TYPE)
                                        el = controller.getTypes().boxedClass((PrimitiveType)type);
                                }
                                addMembers(env, type, el, kinds, baseType, inImport, insideNew, false);
                            }
                            break;
                        default:
                            el = controller.getTrees().getElement(expPath);
                            if (type.getKind() == TypeKind.ERROR && el != null && el.getKind().isClass()) {
                                el = controller.getElements().getPackageElement(((TypeElement)el).getQualifiedName());
                            }
                            if (el != null && el.getKind() == PACKAGE) {                                
                                if (parent.getKind() == Tree.Kind.NEW_CLASS && ((NewClassTree)parent).getIdentifier() == fa && prefix != null) {
                                    String typeName = Utilities.getElementName(el, true) + "." + prefix; //NOI18N
                                    TypeMirror tm = controller.getTreeUtilities().parseType(typeName, env.getScope().getEnclosingClass());
                                    if (tm != null && tm.getKind() == TypeKind.DECLARED)
                                        addMembers(env, tm, ((DeclaredType)tm).asElement(), EnumSet.of(CONSTRUCTOR), null, inImport, insideNew, false);
                                }
                                if (exs != null && !exs.isEmpty()) {
                                    Elements elements = controller.getElements();
                                    for (TypeMirror ex : exs)
                                        if (ex.getKind() == TypeKind.DECLARED) {
                                            Element e = ((DeclaredType)ex).asElement();
                                            if (e.getEnclosingElement() == el && startsWith(env, e.getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)))
                                                results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(e), false, env.isInsideClass(), true, true, false, env.getWhiteList()));
                                        }
                                }
                                addPackageContent(env, (PackageElement)el, kinds, baseType, insideNew, false);
                                if (results.isEmpty() && ((PackageElement)el).getQualifiedName() == el.getSimpleName()) {
                                    // no package content? Check for unimported class
                                    ClassIndex ci = controller.getClasspathInfo().getClassIndex();
                                    if (el.getEnclosedElements().isEmpty() && ci.getPackageNames(el.getSimpleName() + ".", true, EnumSet.allOf(ClassIndex.SearchScope.class)).isEmpty()) {
                                        Trees trees = controller.getTrees();
                                        Scope scope = env.getScope();
                                        for (ElementHandle<TypeElement> teHandle : ci.getDeclaredTypes(el.getSimpleName().toString(), ClassIndex.NameKind.SIMPLE_NAME, EnumSet.allOf(ClassIndex.SearchScope.class))) {
                                            TypeElement te = teHandle.resolve(controller);
                                            if (te != null && trees.isAccessible(scope, te))
                                                addMembers(env, te.asType(), te, kinds, baseType, inImport, insideNew, true);
                                        }
                                    }
                                }
                            }
                    }
                } else if (parent.getKind() == Tree.Kind.COMPILATION_UNIT && ((CompilationUnitTree)parent).getPackageName() == fa) {
                    PackageElement pe = controller.getElements().getPackageElement(fullName(exp));
                    if (pe != null)
                        addPackageContent(env, pe, EnumSet.of(ElementKind.PACKAGE), null, false, true);
                }
            }
        }
        
        private void insideMemberReference(Env env) throws IOException {
            TreePath path = env.getPath();
            MemberReferenceTree mr = (MemberReferenceTree)path.getLeaf();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, mr, env.getOffset());
            if (ts != null && ts.token().id() == JavaTokenId.COLONCOLON) {
                CompilationController controller = env.getController();
                controller.toPhase(Phase.RESOLVED);
                ExpressionTree exp = mr.getQualifierExpression();
                TreePath expPath = new TreePath(path, exp);
                Trees trees = controller.getTrees();
                TypeMirror type = trees.getTypeMirror(expPath);
                if (type != null && type.getKind() == TypeKind.DECLARED) {
                    Element e = trees.getElement(path);
                    addMethodReferences(env, type, e);
                    addKeyword(env, NEW_KEYWORD, SPACE, false);
                }
            }
        }
        
        private void insideLambdaExpression(Env env) throws IOException {
            TreePath path = env.getPath();
            LambdaExpressionTree let = (LambdaExpressionTree) path.getLeaf();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, let, env.getOffset());
            if (ts != null) {
                switch (ts.token().id()) {
                    case ARROW:
                        localResult(env);
                        addValueKeywords(env);
                        break;
                    case COMMA:
                        if (let.getParameters().isEmpty()
                                || env.getController().getTrees().getSourcePositions().getStartPosition(path.getCompilationUnit(), let.getParameters().get(0).getType()) >= 0) {
                            addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                            addPrimitiveTypeKeywords(env);
                            addKeyword(env, FINAL_KEYWORD, SPACE, false);
                        }
                        break;
                }
            }
        }
        
        private void insideMethodInvocation(Env env) throws IOException {
            TreePath path = env.getPath();
            MethodInvocationTree mi = (MethodInvocationTree)path.getLeaf();
            int offset = env.getOffset();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, mi, offset);
            if (ts == null || (ts.token().id() != JavaTokenId.LPAREN && ts.token().id() != JavaTokenId.COMMA)) {
                SourcePositions sp = env.getSourcePositions();
                CompilationUnitTree root = env.getRoot();
                int lastTokenEndOffset = ts.offset() + ts.token().length();
                for (ExpressionTree arg : mi.getArguments()) {
                    int pos = (int)sp.getEndPosition(root, arg);
                    if (lastTokenEndOffset == pos) {
                        insideExpression(env, new TreePath(path, arg));
                        break;
                    }
                    if (offset <= pos)
                        break;
                }
                return;
            }
            String prefix = env.getPrefix();
            if (prefix == null || prefix.length() == 0)
                addMethodArguments(env, mi);
            addLocalMembersAndVars(env);
            addValueKeywords(env);
            addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
            addPrimitiveTypeKeywords(env);
        }
        
        private void insideNewClass(Env env) throws IOException {
            TreePath path = env.getPath();
            NewClassTree nc = (NewClassTree)path.getLeaf();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, nc, env.getOffset());
            if (ts != null) {
                switch(ts.token().id()) {
                    case NEW:
                        String prefix = env.getPrefix();
                        CompilationController controller = env.getController();
                        controller.toPhase(Phase.RESOLVED);
                        TypeElement tel = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                        DeclaredType base = path.getParentPath().getLeaf().getKind() == Tree.Kind.THROW && tel != null ?
                            controller.getTypes().getDeclaredType(tel) : null;
                        TypeElement toExclude = null;
                        if (nc.getIdentifier().getKind() == Tree.Kind.IDENTIFIER && prefix != null) {
                            TypeMirror tm = controller.getTreeUtilities().parseType(prefix, env.getScope().getEnclosingClass());
                            if (tm != null && tm.getKind() == TypeKind.DECLARED) {
                                TypeElement te = (TypeElement)((DeclaredType)tm).asElement();
                                addMembers(env, tm, te, EnumSet.of(CONSTRUCTOR), base, false, true, false);
                                if ((te.getTypeParameters().isEmpty() || SourceVersion.RELEASE_5.compareTo(controller.getSourceVersion()) > 0)
                                        && !Utilities.hasAccessibleInnerClassConstructor(te, env.getScope(), controller.getTrees()))
                                    toExclude = te;
                            }
                        }
                        boolean insideNew = true;
                        ExpressionTree encl = nc.getEnclosingExpression();
                        if (queryType == COMPLETION_QUERY_TYPE) {
                            Set<? extends TypeMirror> smarts = env.getSmartTypes();
                            if (smarts != null) {
                                Elements elements = env.getController().getElements();
                                for (TypeMirror smart : smarts) {
                                    if (smart != null) {
                                        if (smart.getKind() == TypeKind.DECLARED) {
                                            if (encl == null) {
                                                for (DeclaredType subtype : getSubtypesOf(env, (DeclaredType)smart)) {
                                                    TypeElement elem = (TypeElement)subtype.asElement();
                                                    if (toExclude != elem && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)))
                                                        results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, (DeclaredType)Utilities.resolveCapturedType(controller, subtype), anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), true, true, false, true, false, env.getWhiteList()));
                                                    env.addToExcludes(elem);
                                                }
                                            }
                                        } else if (smart.getKind() == TypeKind.ARRAY) {
                                            insideNew = false;
                                            try {
                                                TypeMirror tm = smart;
                                                while(tm.getKind() == TypeKind.ARRAY) {
                                                    tm = ((ArrayType)tm).getComponentType();
                                                }
                                                if (tm.getKind().isPrimitive() && startsWith(env, tm.toString())) {
                                                    results.add(JavaCompletionItem.createArrayItem(env.getController(), (ArrayType)smart, anchorOffset, env.getReferencesCount(), env.getController().getElements(), env.getWhiteList()));
                                                } else if ((tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ERROR) && startsWith(env, ((DeclaredType)tm).asElement().getSimpleName().toString())) {
                                                    results.add(JavaCompletionItem.createArrayItem(env.getController(), (ArrayType)smart, anchorOffset, env.getReferencesCount(), env.getController().getElements(),env.getWhiteList()));
                                                }
                                            } catch (IllegalArgumentException iae) {}
                                        }
                                    }
                                }
                            }
                        }
                        if (toExclude != null)
                            env.addToExcludes(toExclude);
                        if (insideNew)
                            env.insideNew();
                        if (encl == null) {
                            addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE), base);
                        } else {
                            TypeMirror enclType = controller.getTrees().getTypeMirror(new TreePath(path, nc.getEnclosingExpression()));
                            if (enclType != null && enclType.getKind() == TypeKind.DECLARED)
                                addMembers(env, enclType, ((DeclaredType)enclType).asElement(), EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE), base, false, insideNew, false);
                        }
                        break;
                    case LPAREN:
                    case COMMA:
                        prefix = env.getPrefix();
                        if (prefix == null || prefix.length() == 0)
                            addConstructorArguments(env, nc);
                        addLocalMembersAndVars(env);
                        addValueKeywords(env);
                        addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                        addPrimitiveTypeKeywords(env);
                        break;
                    case GT:
                    case GTGT:
                    case GTGTGT:
                        controller = env.getController();
                        TypeMirror tm = controller.getTrees().getTypeMirror(new TreePath(path, nc.getIdentifier()));
                        addMembers(env, tm, ((DeclaredType)tm).asElement(), EnumSet.of(CONSTRUCTOR), null, false, false, false);
                        break;
                }
            }
        }
        
        private void insideTry(Env env) throws IOException {
            CompilationController controller = env.getController();
            addKeyword(env, FINAL_KEYWORD, SPACE, false);
            TypeElement te = controller.getElements().getTypeElement("java.lang.AutoCloseable"); //NOI18N
            if (te != null)
                addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
        }

        private void insideCatch(Env env) throws IOException {
            TreePath path = env.getPath();
            CatchTree ct = (CatchTree)path.getLeaf();
            CompilationController controller = env.getController();
            TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, ct, env.getOffset());
            if (last != null && last.token().id() == JavaTokenId.LPAREN) {
                addKeyword(env, FINAL_KEYWORD, SPACE, false);
                if (queryType == COMPLETION_QUERY_TYPE) {
                    TreePath tryPath = Utilities.getPathElementOfKind(Tree.Kind.TRY, path);
                    Set<TypeMirror> exs = controller.getTreeUtilities().getUncaughtExceptions(tryPath);
                    Elements elements = controller.getElements();
                    for (TypeMirror ex : exs)
                        if (ex.getKind() == TypeKind.DECLARED && startsWith(env, ((DeclaredType)ex).asElement().getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(((DeclaredType)ex).asElement())))
                            results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)((DeclaredType)ex).asElement(), (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(((DeclaredType)ex).asElement()), false, false, false, true, false, env.getWhiteList()));
                }
                TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                if (te != null)
                    addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
            }
        }

        private void insideUnionType(Env env) throws IOException {
            TreePath path = env.getPath();
            UnionTypeTree dtt = (UnionTypeTree)path.getLeaf();
            CompilationController controller = env.getController();
            TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, dtt, env.getOffset());
            if (last != null && last.token().id() == JavaTokenId.BAR) {
                if (queryType == COMPLETION_QUERY_TYPE) {
                    TreePath tryPath = Utilities.getPathElementOfKind(Tree.Kind.TRY, path);
                    Set<TypeMirror> exs = controller.getTreeUtilities().getUncaughtExceptions(tryPath);
                    Elements elements = controller.getElements();
                    for (TypeMirror ex : exs)
                        if (ex.getKind() == TypeKind.DECLARED && startsWith(env, ((DeclaredType)ex).asElement().getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(((DeclaredType)ex).asElement())))
                            results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)((DeclaredType)ex).asElement(), (DeclaredType)ex, anchorOffset, env.getReferencesCount(), elements.isDeprecated(((DeclaredType)ex).asElement()), false, false, false, true, false, env.getWhiteList()));
                }
                TypeElement te = controller.getElements().getTypeElement("java.lang.Throwable"); //NOI18N
                if (te != null)
                    addTypes(env, EnumSet.of(CLASS, INTERFACE, TYPE_PARAMETER), controller.getTypes().getDeclaredType(te));
            }
        }
        
        private void insideIf(Env env) throws IOException {
            IfTree iff = (IfTree)env.getPath().getLeaf();
            if (env.getSourcePositions().getEndPosition(env.getRoot(), iff.getCondition()) <= env.getOffset()) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, iff, env.getOffset());
                if (last != null && (last.token().id() == JavaTokenId.RPAREN || last.token().id() == JavaTokenId.ELSE)) {
                    localResult(env);
                    addKeywordsForStatement(env);
                }
            }
        }
        
        private void insideWhile(Env env) throws IOException {
            WhileLoopTree wlt = (WhileLoopTree)env.getPath().getLeaf();
            if (env.getSourcePositions().getEndPosition(env.getRoot(), wlt.getCondition()) <= env.getOffset()) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, wlt, env.getOffset());
                if (last != null && last.token().id() == JavaTokenId.RPAREN) {
                    localResult(env);
                    addKeywordsForStatement(env);
                }
            }
        }
        
        private void insideDoWhile(Env env) throws IOException {
            DoWhileLoopTree dwlt = (DoWhileLoopTree)env.getPath().getLeaf();
            if (env.getSourcePositions().getEndPosition(env.getRoot(), dwlt.getStatement()) <= env.getOffset()) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, dwlt, env.getOffset());
                if (last != null && (last.token().id() == JavaTokenId.RBRACE || last.token().id() == JavaTokenId.SEMICOLON)) {
                    addKeyword(env, WHILE_KEYWORD, null, false);
                }
            }
        }

        private void insideFor(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            ForLoopTree fl = (ForLoopTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            Tree lastTree = null;
            int lastTreePos = offset;
            for (Tree update : fl.getUpdate()) {
                int pos = (int)sourcePositions.getEndPosition(root, update);
                if (pos == Diagnostic.NOPOS || offset <= pos)
                    break;
                lastTree = update;
                lastTreePos = pos;
            }
            if (lastTree == null) {
                int pos = (int)sourcePositions.getEndPosition(root, fl.getCondition());
                if (pos != Diagnostic.NOPOS && pos < offset) {
                    lastTree = fl.getCondition();
                    lastTreePos = pos;
                }
            }
            if (lastTree == null) {
                for (Tree init : fl.getInitializer()) {
                    int pos = (int)sourcePositions.getEndPosition(root, init);
                    if (pos == Diagnostic.NOPOS || offset <= pos)
                        break;
                    lastTree = init;
                    lastTreePos = pos;
                }
            }
            if (lastTree == null) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, fl, offset);
                if (last != null && last.token().id() == JavaTokenId.LPAREN) {
                    addLocalFieldsAndVars(env);
                    addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                    addPrimitiveTypeKeywords(env);
                }
            } else {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, lastTreePos, offset);
                if (last != null && last.token().id() == JavaTokenId.SEMICOLON) {
                    localResult(env);
                    addValueKeywords(env);
                } else if (last != null && last.token().id() == JavaTokenId.RPAREN) {
                    localResult(env);
                    addKeywordsForStatement(env);
                } else {
                    switch (lastTree.getKind()) {
                        case VARIABLE:
                            Tree var = ((VariableTree)lastTree).getInitializer();
                            if (var != null)
                                insideExpression(env, new TreePath(new TreePath(path, lastTree), var));
                            break;
                        case EXPRESSION_STATEMENT:
                            Tree exp = unwrapErrTree(((ExpressionStatementTree)lastTree).getExpression());
                            if (exp != null)
                                insideExpression(env, new TreePath(new TreePath(path, lastTree), exp));
                            break;
                        default:
                            insideExpression(env, new TreePath(path, lastTree));
                    }
                }
            }
        }

        private void insideForEach(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            EnhancedForLoopTree efl = (EnhancedForLoopTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            CompilationController controller = env.getController();
            if (sourcePositions.getStartPosition(root, efl.getExpression()) >= offset) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, (int)sourcePositions.getEndPosition(root, efl.getVariable()), offset);
                if (last != null && last.token().id() == JavaTokenId.COLON) {
                    env.insideForEachExpression();
                    addKeyword(env, NEW_KEYWORD, SPACE, false);
                    localResult(env);
                }
                return;
            }
            TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, (int)sourcePositions.getEndPosition(root, efl.getExpression()), offset);
            if (last != null && last.token().id() == JavaTokenId.RPAREN) {
                addKeywordsForStatement(env);
            } else {
                env.insideForEachExpression();
                addKeyword(env, NEW_KEYWORD, SPACE, false);
            }
            localResult(env);
            
        }
        
        private void insideSwitch(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            SwitchTree st = (SwitchTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            if (sourcePositions.getStartPosition(root, st.getExpression()) < offset) {
                CaseTree lastCase = null;
                for (CaseTree t : st.getCases()) {
                    int pos = (int)sourcePositions.getStartPosition(root, t);
                    if (pos == Diagnostic.NOPOS || offset <= pos)
                        break;
                    lastCase = t;
                }
                if (lastCase != null) {
                    StatementTree last = null;
                    for(StatementTree stat : lastCase.getStatements()) {
                        int pos = (int)sourcePositions.getStartPosition(root, stat);
                        if (pos == Diagnostic.NOPOS || offset <= pos)
                            break;
                        last = stat;
                    }
                    if (last != null) {
                        if (last.getKind() == Tree.Kind.TRY) {
                            if (((TryTree)last).getFinallyBlock() == null) {
                                addKeyword(env, CATCH_KEYWORD, null, false);
                                addKeyword(env, FINALLY_KEYWORD, null, false);
                                if (((TryTree)last).getCatches().size() == 0)
                                    return;
                            }
                        } else if (last.getKind() == Tree.Kind.IF) {
                            if (((IfTree)last).getElseStatement() == null)
                                addKeyword(env, ELSE_KEYWORD, null, false);
                        }
                    }
                    localResult(env);
                    addKeywordsForBlock(env);
                } else {
                    TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, st, offset);
                    if (ts != null && ts.token().id() == JavaTokenId.LBRACE) {
                        addKeyword(env, CASE_KEYWORD, SPACE, false);
                        addKeyword(env, DEFAULT_KEYWORD, COLON, false);
                    }
                }
            }
        }
        
        private void insideCase(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            CaseTree cst = (CaseTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            CompilationController controller = env.getController();
            if (cst.getExpression() != null && ((sourcePositions.getStartPosition(root, cst.getExpression()) >= offset) ||
                    (cst.getExpression().getKind() == Tree.Kind.ERRONEOUS && ((ErroneousTree)cst.getExpression()).getErrorTrees().isEmpty() && sourcePositions.getEndPosition(root, cst.getExpression()) >= offset))) {
                TreePath path1 = path.getParentPath();
                if (path1.getLeaf().getKind() == Tree.Kind.SWITCH) {
                    TypeMirror tm = controller.getTrees().getTypeMirror(new TreePath(path1, ((SwitchTree)path1.getLeaf()).getExpression()));
                    if (tm.getKind() == TypeKind.DECLARED && ((DeclaredType)tm).asElement().getKind() == ENUM) {
                        addEnumConstants(env, (TypeElement)((DeclaredType)tm).asElement());
                    } else {
                        addLocalConstantsAndTypes(env);
                    }
                }
            } else {
                TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, cst, offset);
                if (ts != null && ts.token().id() != JavaTokenId.DEFAULT) {
                    localResult(env);
                    addKeywordsForBlock(env);
                }
            }
        }
        
        private void insideParens(Env env) throws IOException {
            TreePath path = env.getPath();
            ParenthesizedTree pa = (ParenthesizedTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            Tree exp = unwrapErrTree(pa.getExpression());
            if (exp == null || env.getOffset() <= sourcePositions.getStartPosition(root, exp)) {
                if (queryType == COMPLETION_QUERY_TYPE && path.getParentPath().getLeaf().getKind() != Tree.Kind.SWITCH) {
                    Set<? extends TypeMirror> smarts = env.getSmartTypes();
                    if (smarts != null) {
                        Elements elements = env.getController().getElements();
                        for (TypeMirror smart : smarts) {
                            if (smart != null) {
                                if (smart.getKind() == TypeKind.DECLARED) {
                                    for (DeclaredType subtype : getSubtypesOf(env, (DeclaredType)smart)) {
                                        TypeElement elem = (TypeElement)subtype.asElement();
                                        if (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem))
                                            results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, subtype, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), false, false, false, true, false, env.getWhiteList()));
                                        env.addToExcludes(elem);
                                    }
                                } else if (smart.getKind() == TypeKind.ARRAY) {
                                    try {
                                        TypeMirror tm = smart;
                                        while(tm.getKind() == TypeKind.ARRAY) {
                                            tm = ((ArrayType)tm).getComponentType();
                                        }
                                        if (tm.getKind().isPrimitive() && startsWith(env, tm.toString())) {
                                            results.add(JavaCompletionItem.createArrayItem(env.getController(), (ArrayType)smart, anchorOffset, env.getReferencesCount(), env.getController().getElements(),env.getWhiteList()));
                                        } else if ((tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ERROR) && startsWith(env, ((DeclaredType)tm).asElement().getSimpleName().toString())) {
                                            results.add(JavaCompletionItem.createArrayItem(env.getController(), (ArrayType)smart, anchorOffset, env.getReferencesCount(), env.getController().getElements(), env.getWhiteList()));
                                        }
                                    } catch (IllegalArgumentException iae) {}
                                }
                            }
                        }
                    }
                }
                addLocalMembersAndVars(env);
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
                addPrimitiveTypeKeywords(env);
                addValueKeywords(env);
            } else {
                insideExpression(env, new TreePath(path, exp));
            }
        }
        
        private void insideTypeCheck(Env env) throws IOException {
            InstanceOfTree iot = (InstanceOfTree)env.getPath().getLeaf();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, iot, env.getOffset());
            if (ts != null && ts.token().id() == JavaTokenId.INSTANCEOF)
                addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
        }
        
        private void insideArrayAccess(Env env) throws IOException {
            int offset = env.getOffset();
            ArrayAccessTree aat = (ArrayAccessTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int aaTextStart = (int)sourcePositions.getEndPosition(root, aat.getExpression());
            if (aaTextStart != Diagnostic.NOPOS) {
                Tree expr = unwrapErrTree(aat.getIndex());
                if (expr == null || offset <= (int)sourcePositions.getStartPosition(root, expr)) {
                    String aatText = env.getController().getText().substring(aaTextStart, offset);
                    int bPos = aatText.indexOf('['); //NOI18N
                    if (bPos > -1) {
                        localResult(env);
                        addValueKeywords(env);
                    }
                }
            }
        }
        
        private void insideNewArray(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            NewArrayTree nat = (NewArrayTree)path.getLeaf();
            if (nat.getInitializers() != null) { // UFFF!!!!
                SourcePositions sourcePositions = env.getSourcePositions();
                CompilationUnitTree root = env.getRoot();
                Tree last = null;
                int lastPos = offset;
                for (Tree init : nat.getInitializers()) {
                    int pos = (int)sourcePositions.getEndPosition(root, init);
                    if (pos == Diagnostic.NOPOS || offset <= pos)
                        break;
                    last = init;
                    lastPos = pos;
                }
                if (last != null) {
                    TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, lastPos, offset);
                    if (ts != null && ts.token().id() == JavaTokenId.COMMA) {
                        TreePath parentPath = path.getParentPath();
                        TreePath gparentPath = parentPath.getParentPath();
                        if (gparentPath.getLeaf().getKind() == Tree.Kind.ANNOTATION && parentPath.getLeaf().getKind() == Tree.Kind.ASSIGNMENT) {
                            ExpressionTree var = ((AssignmentTree)parentPath.getLeaf()).getVariable();
                            if (var.getKind() == Tree.Kind.IDENTIFIER) {
                                insideAnnotationAttribute(env, gparentPath, ((IdentifierTree)var).getName());
                                addLocalConstantsAndTypes(env);
                            }
                        } else {
                            localResult(env);
                            addValueKeywords(env);
                        }
                    }
                    return;
                }
            }
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, nat, offset);
            switch (ts.token().id()) {
                case LBRACKET:
                case LBRACE:
                    TreePath parentPath = path.getParentPath();
                    TreePath gparentPath = parentPath.getParentPath();
                    if (gparentPath.getLeaf().getKind() == Tree.Kind.ANNOTATION && parentPath.getLeaf().getKind() == Tree.Kind.ASSIGNMENT) {
                        ExpressionTree var = ((AssignmentTree)parentPath.getLeaf()).getVariable();
                        if (var.getKind() == Tree.Kind.IDENTIFIER) {
                            insideAnnotationAttribute(env, gparentPath, ((IdentifierTree)var).getName());
                            addLocalConstantsAndTypes(env);
                        }
                    } else {
                        localResult(env);
                        addValueKeywords(env);
                    }
                    break;
                case RBRACKET:
                    if (nat.getDimensions().size() > 0)
                        insideExpression(env, path);
                    break;
            }
        }
        
        private void insideAssignment(Env env) throws IOException {
            int offset = env.getOffset();
            TreePath path = env.getPath();
            AssignmentTree as = (AssignmentTree)path.getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int asTextStart = (int)sourcePositions.getEndPosition(root, as.getVariable());
            if (asTextStart != Diagnostic.NOPOS) {
                Tree expr = unwrapErrTree(as.getExpression());
                if (expr == null || offset <= (int)sourcePositions.getStartPosition(root, expr)) {
                    CompilationController controller = env.getController();
                    String asText = controller.getText().substring(asTextStart, offset);
                    int eqPos = asText.indexOf('='); //NOI18N
                    if (eqPos > -1) {
                        TreePath parentPath = path.getParentPath();
                        if (parentPath.getLeaf().getKind() != Tree.Kind.ANNOTATION) {
                            localResult(env);
                            addValueKeywords(env);
                        } else if (as.getVariable().getKind() == Tree.Kind.IDENTIFIER) {
                            insideAnnotationAttribute(env, parentPath, ((IdentifierTree)as.getVariable()).getName());
                            addLocalConstantsAndTypes(env);
                        }
                    }
                } else {
                    insideExpression(env, new TreePath(path, expr));
                }
            }
        }
        
        private void insideCompoundAssignment(Env env) throws IOException {
            int offset = env.getOffset();
            CompoundAssignmentTree cat = (CompoundAssignmentTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int catTextStart = (int)sourcePositions.getEndPosition(root, cat.getVariable());
            if (catTextStart != Diagnostic.NOPOS) {
                Tree expr = unwrapErrTree(cat.getExpression());
                if (expr == null || offset <= (int)sourcePositions.getStartPosition(root, expr)) {
                    String catText = env.getController().getText().substring(catTextStart, offset);
                    int eqPos = catText.indexOf('='); //NOI18N
                    if (eqPos > -1) {
                        localResult(env);
                        addValueKeywords(env);
                    }
                }
            }
        }
        
        private void insideStringLiteral(Env env) throws IOException {
            TreePath path = env.getPath();
            TreePath parentPath = path.getParentPath();
            TreePath grandParentPath = parentPath.getParentPath();
            if (grandParentPath != null && grandParentPath.getLeaf().getKind() == Tree.Kind.ANNOTATION
                    && parentPath.getLeaf().getKind() == Tree.Kind.ASSIGNMENT
                    && ((AssignmentTree)parentPath.getLeaf()).getExpression() == path.getLeaf()) {
                ExpressionTree var = ((AssignmentTree)parentPath.getLeaf()).getVariable();
                if (var.getKind() == Tree.Kind.IDENTIFIER)
                    insideAnnotationAttribute(env, grandParentPath, ((IdentifierTree)var).getName());
            }
        }
        
        private void insideBinaryTree(Env env) throws IOException {
            int offset = env.getOffset();
            BinaryTree bi = (BinaryTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int pos = (int)sourcePositions.getEndPosition(root, bi.getRightOperand());
            if (pos != Diagnostic.NOPOS && pos < offset)
                return;
            pos = (int)sourcePositions.getEndPosition(root, bi.getLeftOperand());
            if (pos != Diagnostic.NOPOS) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, pos, offset);
                if (last != null) {
                    localResult(env);
                    addValueKeywords(env);
                }
            }
        }

        private void insideConditionalExpression(Env env) throws IOException {
            ConditionalExpressionTree co = (ConditionalExpressionTree)env.getPath().getLeaf();
            SourcePositions sourcePositions = env.getSourcePositions();
            CompilationUnitTree root = env.getRoot();
            int coTextStart = (int)sourcePositions.getStartPosition(root, co);
            if (coTextStart != Diagnostic.NOPOS) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, coTextStart, env.getOffset());
                if (last != null && (last.token().id() == JavaTokenId.QUESTION || last.token().id() == JavaTokenId.COLON)) {
                    localResult(env);
                    addValueKeywords(env);
                }
            }
        }
        
        private void insideExpressionStatement(Env env) throws IOException {
            TreePath path = env.getPath();
            ExpressionStatementTree est = (ExpressionStatementTree)path.getLeaf();
            CompilationController controller = env.getController();
            Tree t = est.getExpression();
            if (t.getKind() == Tree.Kind.ERRONEOUS) {
                Iterator<? extends Tree> it = ((ErroneousTree)t).getErrorTrees().iterator();
                if (it.hasNext()) {
                    t = it.next();
                } else {
                    localResult(env);
                    Tree parentTree = path.getParentPath().getLeaf();
                    switch (parentTree.getKind()) {
                        case FOR_LOOP:
                            if (((ForLoopTree)parentTree).getStatement() == est)
                                addKeywordsForStatement(env);
                            else
                                addValueKeywords(env);
                            break;
                        case ENHANCED_FOR_LOOP:
                            if (((EnhancedForLoopTree)parentTree).getStatement() == est)
                                addKeywordsForStatement(env);
                            else
                                addValueKeywords(env);
                            break;
                        case VARIABLE:
                            addValueKeywords(env);
                            break;
                        case LAMBDA_EXPRESSION:
                            addValueKeywords(env);
                            break;
                        default:
                            addKeywordsForStatement(env);
                            break;
                    }
                    return;
                }
            }
            TreePath tPath = new TreePath(path, t);
            if (t.getKind() == Tree.Kind.MODIFIERS) {
                insideModifiers(env, tPath);
            } else if (t.getKind() == Tree.Kind.MEMBER_SELECT && ERROR.contentEquals(((MemberSelectTree)t).getIdentifier())) {
                controller.toPhase(Phase.ELEMENTS_RESOLVED);
                TreePath expPath = new TreePath(tPath, ((MemberSelectTree)t).getExpression());
                TypeMirror type = controller.getTrees().getTypeMirror(expPath);
                switch (type.getKind()) {
                    case TYPEVAR:
                        type = ((TypeVariable)type).getUpperBound();
                        if (type == null)
                            return;
                        type = controller.getTypes().capture(type);
                    case ARRAY:
                    case DECLARED:
                    case BOOLEAN:
                    case BYTE:
                    case CHAR:
                    case DOUBLE:
                    case FLOAT:
                    case INT:
                    case LONG:
                    case SHORT:
                    case VOID:
                        addMembers(env, type, controller.getTrees().getElement(expPath), EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE, FIELD, METHOD, ENUM_CONSTANT), null, false, false, false);
                        break;
                    default:
                        Element el = controller.getTrees().getElement(expPath);
                        if (el instanceof PackageElement) {
                            addPackageContent(env, (PackageElement)el, EnumSet.of(CLASS, ENUM, ANNOTATION_TYPE, INTERFACE, FIELD, METHOD, ENUM_CONSTANT), null, false, false);
                        }
                }
            } else {
                insideExpression(env, tPath);
            }
            
        }
        
        private void insideExpression(Env env, TreePath exPath) throws IOException {
            int offset = env.getOffset();
            String prefix = env.getPrefix();
            Tree et = exPath.getLeaf();
            Tree parent = exPath.getParentPath().getLeaf();
            CompilationController controller = env.getController();
            int endPos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), et);
            if (endPos != Diagnostic.NOPOS && endPos < offset) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, endPos, offset);
                if (last != null && last.token().id() != JavaTokenId.COMMA)
                    return;
            }
            controller.toPhase(Phase.ELEMENTS_RESOLVED);
            boolean isConst = parent.getKind() == Tree.Kind.VARIABLE && ((VariableTree)parent).getModifiers().getFlags().containsAll(EnumSet.of(FINAL, STATIC));
            if ((parent == null || parent.getKind() != Tree.Kind.PARENTHESIZED) &&
                    (et.getKind() == Tree.Kind.PRIMITIVE_TYPE || et.getKind() == Tree.Kind.ARRAY_TYPE || et.getKind() == Tree.Kind.PARAMETERIZED_TYPE)) {
                TypeMirror tm = controller.getTrees().getTypeMirror(exPath);
                final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
                Scope scope = env.getScope();
                final ExecutableElement method = scope.getEnclosingMethod();
                ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                    public boolean accept(Element e, TypeMirror t) {
                        return (method == null || method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                !illegalForwardRefs.contains(e);
                    }
                };
                for (String name : Utilities.varNamesSuggestions(tm, null, prefix, controller.getTypes(), controller.getElements(), controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), isConst))
                    results.add(JavaCompletionItem.createVariableItem(env.getController(), name, anchorOffset, true, false));
                return;
            }
            if (et.getKind() == Tree.Kind.UNION_TYPE) {
                for(Tree t : ((UnionTypeTree)et).getTypeAlternatives()) {
                    et = t;
                    exPath = new TreePath(exPath, t);
                }
            }
            if (et.getKind() == Tree.Kind.IDENTIFIER) {
                Element e = controller.getTrees().getElement(exPath);
                if (e == null)
                    return;
                TypeMirror tm = controller.getTrees().getTypeMirror(exPath);
                switch (e.getKind()) {
                    case ANNOTATION_TYPE:
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                    case PACKAGE:
                        if (parent == null || parent.getKind() != Tree.Kind.PARENTHESIZED
                                || env.getController().getSourceVersion().compareTo(SourceVersion.RELEASE_8) >= 0) {
                            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
                            Scope scope = env.getScope();
                            final ExecutableElement method = scope.getEnclosingMethod();
                            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                public boolean accept(Element e, TypeMirror t) {
                                    return (method == null || method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                            !illegalForwardRefs.contains(e);
                                }
                            };
                            for (String name : Utilities.varNamesSuggestions(tm, null, prefix, controller.getTypes(), controller.getElements(), controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), isConst))
                                results.add(JavaCompletionItem.createVariableItem(env.getController(), name, anchorOffset, true, false));
                        }
                        VariableElement ve = getFieldOrVar(env, e.getSimpleName().toString());
                        if (ve != null) {
                            addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                        }
                        break;
                    case ENUM_CONSTANT:
                    case EXCEPTION_PARAMETER:
                    case FIELD:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case PARAMETER:
                        if (tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY || tm.getKind() == TypeKind.ERROR)) {
                            addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                        }
                        TypeElement te = getTypeElement(env, e.getSimpleName().toString());
                        if (te != null) {
                            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
                            Scope scope = env.getScope();
                            final ExecutableElement method = scope.getEnclosingMethod();
                            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                public boolean accept(Element e, TypeMirror t) {
                                    return (method == null || method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                            !illegalForwardRefs.contains(e);
                                }
                            };
                            for (String name : Utilities.varNamesSuggestions(controller.getTypes().getDeclaredType(te), null, prefix, controller.getTypes(), controller.getElements(), controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), isConst))
                                results.add(JavaCompletionItem.createVariableItem(env.getController(), name, anchorOffset, true, false));
                        }
                        break;
                }
                return;
            }
            Tree exp = null;
            if (et.getKind() == Tree.Kind.PARENTHESIZED) {
                exp = ((ParenthesizedTree)et).getExpression();
            } else if (et.getKind() == Tree.Kind.TYPE_CAST) {
                if (env.getSourcePositions().getEndPosition(env.getRoot(), ((TypeCastTree)et).getType()) <= offset)
                    exp = ((TypeCastTree)et).getType();
            } else if (et.getKind() == Tree.Kind.ASSIGNMENT) {
                Tree t = ((AssignmentTree)et).getExpression();
                if (t.getKind() == Tree.Kind.PARENTHESIZED && env.getSourcePositions().getEndPosition(env.getRoot(), t) < offset)
                    exp = ((ParenthesizedTree)t).getExpression();
            }
            if (exp != null) {
                exPath = new TreePath(exPath, exp);
                if (exp.getKind() == Tree.Kind.PRIMITIVE_TYPE || exp.getKind() == Tree.Kind.ARRAY_TYPE || exp.getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
                    localResult(env);
                    addValueKeywords(env);
                    return;
                }
                Element e = controller.getTrees().getElement(exPath);
                if (e == null) {
                    if (exp.getKind() == Tree.Kind.TYPE_CAST) {
                        addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                    }
                    return;
                }
                TypeMirror tm = controller.getTrees().getTypeMirror(exPath);
                switch (e.getKind()) {
                    case ANNOTATION_TYPE:
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                    case PACKAGE:
                        if (exp.getKind() == Tree.Kind.IDENTIFIER) {
                            VariableElement ve = getFieldOrVar(env, e.getSimpleName().toString());
                            if (ve != null) {
                                addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                            }
                            if (ve == null || tm == null || tm.getKind() != TypeKind.ERROR) {
                                localResult(env);
                                addValueKeywords(env);
                            }
                        } else if (exp.getKind() == Tree.Kind.MEMBER_SELECT) {
                            if (tm != null && (tm.getKind() == TypeKind.ERROR || tm.getKind() == TypeKind.PACKAGE)) {
                                addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                            }
                            localResult(env);
                            addValueKeywords(env);
                        } else if (exp.getKind() == Tree.Kind.PARENTHESIZED && tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY)) {
                            addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                        }
                        break;
                    case ENUM_CONSTANT:
                    case EXCEPTION_PARAMETER:
                    case FIELD:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case PARAMETER:
                        if (tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY || tm.getKind() == TypeKind.ERROR)) {
                            addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                        }
                        TypeElement te = getTypeElement(env, e.getSimpleName().toString());
                        if (te != null || exp.getKind() == Tree.Kind.MEMBER_SELECT) {
                            localResult(env);
                            addValueKeywords(env);
                        }
                        break;
                    case CONSTRUCTOR:
                    case METHOD:
                        if (tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY || tm.getKind() == TypeKind.ERROR)) {
                            addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                        }
                }
                return;
            }
            Element e = controller.getTrees().getElement(exPath);
            TypeMirror tm = controller.getTrees().getTypeMirror(exPath);
            if (e == null) {
                if (tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY || tm.getKind() == TypeKind.ERROR)) {
                    addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                }
                return;
            }
            switch (e.getKind()) {
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                case PACKAGE:
                    final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
                    Scope scope = env.getScope();
                    final ExecutableElement method = scope.getEnclosingMethod();
                    ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                       public boolean accept(Element e, TypeMirror t) {
                            return (method == null || method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                    !illegalForwardRefs.contains(e);
                        }
                    };
                    for (String name : Utilities.varNamesSuggestions(tm, null, prefix, controller.getTypes(), controller.getElements(), controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), isConst))
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), name, anchorOffset, true, false));
                    break;
                case ENUM_CONSTANT:
                case EXCEPTION_PARAMETER:
                case FIELD:
                case LOCAL_VARIABLE:
                case RESOURCE_VARIABLE:
                case PARAMETER:
                case CONSTRUCTOR:
                case METHOD:
                    if (tm != null && (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY || tm.getKind() == TypeKind.ERROR)) {
                        addKeyword(env, INSTANCEOF_KEYWORD, SPACE, false);
                    }
            }
        }

        private void insideBreak(Env env) throws IOException {
            TreePath path = env.getPath();
            TokenSequence<JavaTokenId> ts = findLastNonWhitespaceToken(env, path.getLeaf(), env.getOffset());
            if (ts != null && ts.token().id() == JavaTokenId.BREAK) {
                while (path != null) {
                    if (path.getLeaf().getKind() == Tree.Kind.LABELED_STATEMENT)
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), ((LabeledStatementTree)path.getLeaf()).getLabel().toString(), anchorOffset, false, false));
                    path = path.getParentPath();
                }
            }
        }
        
        private void localResult(Env env) throws IOException {
            addLocalMembersAndVars(env);
            addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
            addPrimitiveTypeKeywords(env);
        }
        
        private void addLocalConstantsAndTypes(final Env env) throws IOException {
            final String prefix = env.getPrefix();
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final Trees trees = controller.getTrees();
            final Scope scope = env.getScope();
            Set<? extends TypeMirror> smartTypes = null;
            boolean smartType = false;
            if (queryType == COMPLETION_QUERY_TYPE) {
                smartTypes = env.getSmartTypes();
                if (smartTypes != null) {
                    for (TypeMirror st : smartTypes) {
                        if (st.getKind() == TypeKind.BOOLEAN) {
                            smartType = true;
                        }
                        if (st.getKind().isPrimitive())
                            st = types.boxedClass((PrimitiveType)st).asType();
                        if (st.getKind() == TypeKind.DECLARED) {
                            final DeclaredType type = (DeclaredType)st;
                            final TypeElement element = (TypeElement)type.asElement();
                            if (element.getKind() == ANNOTATION_TYPE && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(element)))
                                results.add(JavaCompletionItem.createAnnotationItem(env.getController(), element, (DeclaredType)type, anchorOffset, env.getReferencesCount(), elements.isDeprecated(element), env.getWhiteList()));
                            final boolean isStatic = element.getKind().isClass() || element.getKind().isInterface();
                            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                public boolean accept(Element e, TypeMirror t) {
                                    return (e.getKind() == ENUM_CONSTANT || e.getKind() == FIELD && ((VariableElement)e).getConstantValue() != null) &&
                                            (!isStatic || e.getModifiers().contains(STATIC)) &&
                                            Utilities.startsWith(e.getEnclosingElement().getSimpleName() + "." + e.getSimpleName(), prefix) &&
                                            trees.isAccessible(scope, e, (DeclaredType)t) &&
                                            types.isAssignable(((VariableElement)e).asType(), type);
                                }
                            };
                            for (Element ee : controller.getElementUtilities().getMembers(type, acceptor)) {
                                if (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(ee))
                                    results.add(JavaCompletionItem.createStaticMemberItem(env.getController(), type, ee, types.asMemberOf(type, ee), anchorOffset, elements.isDeprecated(ee), false, env.getWhiteList()));
                            }
                        }
                    }
                }
            }
            if (env.getPath().getLeaf().getKind() != Tree.Kind.CASE) {
                if (Utilities.startsWith(FALSE_KEYWORD, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(FALSE_KEYWORD, null, anchorOffset, smartType));
                if (Utilities.startsWith(TRUE_KEYWORD, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(TRUE_KEYWORD, null, anchorOffset, smartType));
            }
            final TypeElement enclClass = scope.getEnclosingClass();
            final TreeUtilities tu = controller.getTreeUtilities();
            final boolean isStatic = enclClass == null ? false :
                (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
            final ExecutableElement method = scope.getEnclosingMethod();
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    switch (e.getKind()) {
                        case LOCAL_VARIABLE:
                        case RESOURCE_VARIABLE:
                        case EXCEPTION_PARAMETER:
                        case PARAMETER:
                            return (method == e.getEnclosingElement() || e.getModifiers().contains(FINAL) ||
                                    (method == null && (e.getEnclosingElement().getKind() == INSTANCE_INIT ||
                                    e.getEnclosingElement().getKind() == STATIC_INIT))) &&
                                    !illegalForwardRefs.contains(e) &&
                                    ((VariableElement)e).getConstantValue() != null;
                        case FIELD:
                            if (illegalForwardRefs.contains(e) || ((VariableElement)e).getConstantValue() == null)
                                return false;
                        case ENUM_CONSTANT:
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    (!isStatic || e.getModifiers().contains(STATIC)) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    trees.isAccessible(scope, e, (DeclaredType)t);
                    }
                    return false;
                }
            };
            for (Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor))
                if (e.getKind() == FIELD) {
                    TypeMirror tm = asMemberOf(e, enclClass != null ? enclClass.asType() : null, types);
                    results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, tm, anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), false, isOfSmartType(env, tm, smartTypes), env.assignToVarPos(), env.getWhiteList()));
                } else {
                    results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, e.asType(), anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), false, isOfSmartType(env, e.asType(), smartTypes), env.assignToVarPos(), env.getWhiteList()));
                }
            addTypes(env, EnumSet.of(CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, TYPE_PARAMETER), null);
        }
        
        private void addLocalMembersAndVars(final Env env) throws IOException {
            final String prefix = env.getPrefix();
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final Trees trees = controller.getTrees();
            final TreeUtilities tu = controller.getTreeUtilities();
            final Scope scope = env.getScope();
            Set<? extends TypeMirror> smartTypes = null;
            if (queryType == COMPLETION_QUERY_TYPE) {
                smartTypes = env.getSmartTypes();
                if (smartTypes != null) {
                    for (TypeMirror st : smartTypes) {
                        if (st.getKind().isPrimitive())
                            st = types.boxedClass((PrimitiveType)st).asType();
                        if (st.getKind() == TypeKind.DECLARED) {
                            final DeclaredType type = (DeclaredType)st;
                            final TypeElement element = (TypeElement)type.asElement();
                            final boolean withinScope = withinScope(env, element);
                            if (withinScope && scope.getEnclosingClass() == element)
                                continue;
                            final boolean isStatic = element.getKind().isClass() || element.getKind().isInterface();
                            final Set<? extends TypeMirror> finalSmartTypes = smartTypes;
                            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                public boolean accept(Element e, TypeMirror t) {
                                    return ((!withinScope && (!isStatic || e.getModifiers().contains(STATIC))) || withinScope && e.getSimpleName().contentEquals(THIS_KEYWORD)) &&
                                            startsWith(env, e.getSimpleName().toString()) &&
                                            trees.isAccessible(scope, e, (DeclaredType)t) &&
                                            (e.getKind().isField() && isOfSmartType(env, ((VariableElement)e).asType(), finalSmartTypes) || e.getKind() == METHOD && isOfSmartType(env, ((ExecutableElement)e).getReturnType(), finalSmartTypes));
                                }
                            };
                            for (Element ee : controller.getElementUtilities().getMembers(type, acceptor)) {
                                if (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(ee))
                                    results.add(JavaCompletionItem.createStaticMemberItem(env.getController(), type, ee, types.asMemberOf(type, ee), anchorOffset, elements.isDeprecated(ee), env.addSemicolon(), env.getWhiteList()));
                            }
                        }
                    }
                }
            }
            final TypeElement enclClass = scope.getEnclosingClass();
            final boolean enclStatic = enclClass != null && enclClass.getModifiers().contains(Modifier.STATIC);
            final boolean ctxStatic = enclClass != null && (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
            final ExecutableElement method = scope.getEnclosingMethod() != null && scope.getEnclosingMethod().getEnclosingElement() == enclClass ? scope.getEnclosingMethod() : null;
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    boolean isStatic = ctxStatic || (t != null && t.getKind() == TypeKind.DECLARED && ((DeclaredType)t).asElement() != enclClass && enclStatic);
                    switch (e.getKind()) {
                        case CONSTRUCTOR:
                            return false;
                        case LOCAL_VARIABLE:
                        case RESOURCE_VARIABLE:
                        case EXCEPTION_PARAMETER:
                        case PARAMETER:
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    (method == e.getEnclosingElement() || e.getModifiers().contains(FINAL) ||
                                    (method == null && (e.getEnclosingElement().getKind() == INSTANCE_INIT ||
                                    e.getEnclosingElement().getKind() == STATIC_INIT))) &&
                                    !illegalForwardRefs.contains(e);
                        case FIELD:
                            if (e.getSimpleName().contentEquals(THIS_KEYWORD) || e.getSimpleName().contentEquals(SUPER_KEYWORD))
                                return Utilities.startsWith(e.getSimpleName().toString(), prefix) && !isStatic;
                        case ENUM_CONSTANT:
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    !illegalForwardRefs.contains(e) &&
                                    (!isStatic || e.getModifiers().contains(STATIC)) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    trees.isAccessible(scope, e, (DeclaredType)t);
                        case METHOD:
                            String sn = e.getSimpleName().toString();
                            return startsWith(env, sn) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    (!isStatic || e.getModifiers().contains(STATIC)) &&
                                    trees.isAccessible(scope, e, (DeclaredType)t) &&
                                    (!Utilities.isExcludeMethods() || !Utilities.isExcluded(Utilities.getElementName(e.getEnclosingElement(), true) + "." + sn)); //NOI18N
                    }
                    return false;
                }
            };
            for (Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor)) {
                switch (e.getKind()) {
                    case ENUM_CONSTANT:
                    case EXCEPTION_PARAMETER:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case PARAMETER:
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, e.asType(), anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), env.addSemicolon(), isOfSmartType(env, e.asType(), smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        break;
                    case FIELD:
                        String name = e.getSimpleName().toString();
                        if (THIS_KEYWORD.equals(name) || SUPER_KEYWORD.equals(name)) {
                            results.add(JavaCompletionItem.createKeywordItem(name, null, anchorOffset, isOfSmartType(env, e.asType(), smartTypes)));
                        } else {
                            TypeMirror tm = asMemberOf(e, enclClass != null ? enclClass.asType() : null, types);
                            results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, tm, anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), env.addSemicolon(), isOfSmartType(env, tm, smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        }
                        break;
                    case METHOD:
                        ExecutableType et = (ExecutableType)asMemberOf(e, enclClass != null ? enclClass.asType() : null, types);
                        results.add(JavaCompletionItem.createExecutableItem(env.getController(), (ExecutableElement)e, et, anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), false, env.addSemicolon(), isOfSmartType(env, et.getReturnType(), smartTypes), env.assignToVarPos(), false, env.getWhiteList()));
                        break;
                }
            }
        }

        private void addLocalFieldsAndVars(final Env env) throws IOException {
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final TreeUtilities tu = controller.getTreeUtilities();
            final Scope scope = env.getScope();
            Set<? extends TypeMirror> smartTypes = queryType == COMPLETION_QUERY_TYPE ? env.getSmartTypes() : null;
            final TypeElement enclClass = scope.getEnclosingClass();
            final boolean isStatic = enclClass == null ? false :
                (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
            final ExecutableElement method = scope.getEnclosingMethod();
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    switch (e.getKind()) {
                        case LOCAL_VARIABLE:
                        case RESOURCE_VARIABLE:
                        case EXCEPTION_PARAMETER:
                        case PARAMETER:
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    (method == e.getEnclosingElement() || e.getModifiers().contains(FINAL) ||
                                    (method == null && (e.getEnclosingElement().getKind() == INSTANCE_INIT ||
                                    e.getEnclosingElement().getKind() == STATIC_INIT))) &&
                                    !illegalForwardRefs.contains(e);
                        case FIELD:
                            return !e.getSimpleName().contentEquals(THIS_KEYWORD) && !e.getSimpleName().contentEquals(SUPER_KEYWORD) &&
                                    !isStatic && startsWith(env, e.getSimpleName().toString()) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e));
                    }
                    return false;
                }
            };            
            for (Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor)) {
                switch (e.getKind()) {
                    case ENUM_CONSTANT:
                    case EXCEPTION_PARAMETER:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case PARAMETER:
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, e.asType(), anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), false, isOfSmartType(env, e.asType(), smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        break;
                    case FIELD:
                        TypeMirror tm = asMemberOf(e, enclClass != null ? enclClass.asType() : null, types);
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, tm, anchorOffset, null, env.getScope().getEnclosingClass() != e.getEnclosingElement(), elements.isDeprecated(e), false, isOfSmartType(env, tm, smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        break;
                }
            }
        }
        
        private void addMemberConstantsAndTypes(final Env env, final TypeMirror type, final Element elem) throws IOException {
            Set<? extends TypeMirror> smartTypes = queryType == COMPLETION_QUERY_TYPE ? env.getSmartTypes() : null;
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final Trees trees = controller.getTrees();
            TypeElement typeElem = type.getKind() == TypeKind.DECLARED ? (TypeElement)((DeclaredType)type).asElement() : null;
            final boolean isStatic = elem != null && (elem.getKind().isClass() || elem.getKind().isInterface() || elem.getKind() == TYPE_PARAMETER);
            final boolean isSuperCall = elem != null && elem.getKind().isField() && elem.getSimpleName().contentEquals(SUPER_KEYWORD);
            final Scope scope = env.getScope();
            TypeElement enclClass = scope.getEnclosingClass();
            final TypeMirror enclType = enclClass != null ? enclClass.asType() : null;
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    if (!startsWith(env, e.getSimpleName().toString()) ||
                            (isStatic && !e.getModifiers().contains(STATIC)))
                        return false;
                    switch (e.getKind()) {
                        case FIELD:
                            if (((VariableElement)e).getConstantValue() == null && !CLASS_KEYWORD.contentEquals(e.getSimpleName()))
                                return false;
                        case ENUM_CONSTANT:
                            return  (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) && trees.isAccessible(scope, e, (DeclaredType)(isSuperCall && enclType != null ? enclType : t));
                        case CLASS:
                        case ENUM:
                        case INTERFACE:
                            return  (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) && trees.isAccessible(scope, e, (DeclaredType)t);
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getMembers(type, acceptor)) {
                switch (e.getKind()) {
                    case FIELD:
                    case ENUM_CONSTANT:
                        String name = e.getSimpleName().toString();
                        if (CLASS_KEYWORD.equals(name)) {
                            results.add(JavaCompletionItem.createKeywordItem(name, null, anchorOffset, false));
                        } else {
                            TypeMirror tm = type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType();
                            results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, tm, anchorOffset, null, typeElem != e.getEnclosingElement(), elements.isDeprecated(e), false, isOfSmartType(env, tm, smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        }
                        break;
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                        DeclaredType dt = (DeclaredType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                        results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, dt, anchorOffset, null, elements.isDeprecated(e), false, env.isInsideClass(), true, false, false, env.getWhiteList()));
                        break;
                }
            }
        }
        
        private void addMethodReferences(final Env env, final TypeMirror type, final Element elem) throws IOException {
            Set<? extends TypeMirror> smartTypes = env.getSmartTypes();
            final String prefix = env.getPrefix();
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final TreeUtilities tu = controller.getTreeUtilities();
            TypeElement typeElem = type.getKind() == TypeKind.DECLARED ? (TypeElement)((DeclaredType)type).asElement() : null;
            final boolean isStatic = elem != null && (elem.getKind().isClass() || elem.getKind().isInterface()) && elem.asType().getKind() != TypeKind.ERROR;
            final boolean isThisCall = elem != null && elem.getKind().isField() && elem.getSimpleName().contentEquals(THIS_KEYWORD);
            final boolean isSuperCall = elem != null && elem.getKind().isField() && elem.getSimpleName().contentEquals(SUPER_KEYWORD);
            final Scope scope = env.getScope();
            if ((isThisCall || isSuperCall) && tu.isStaticContext(scope))
                return;
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    switch (e.getKind()) {
                        case METHOD:
                            String sn = e.getSimpleName().toString();
                            return startsWith(env, sn, prefix) &&
                                    (!isStatic || e.getModifiers().contains(STATIC)) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    env.isAccessible(scope, e, t, isSuperCall) &&
                                    (!Utilities.isExcludeMethods() || !Utilities.isExcluded(Utilities.getElementName(e.getEnclosingElement(), true) + "." + sn)); //NOI18N
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getMembers(type, acceptor)) {
                switch (e.getKind()) {
                    case METHOD:
                        ExecutableType et = (ExecutableType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                        results.add(JavaCompletionItem.createExecutableItem(env.getController(), (ExecutableElement)e, et, anchorOffset, null, typeElem != e.getEnclosingElement(), elements.isDeprecated(e), false, false, isOfSmartType(env, et.getReturnType(), smartTypes), env.assignToVarPos(), true, env.getWhiteList()));
                        break;
                }
            }
        }

        private void addMembers(final Env env, final TypeMirror type, final Element elem, final EnumSet<ElementKind> kinds, final DeclaredType baseType, final boolean inImport, final boolean insideNew, final boolean autoImport) throws IOException {
            Set<? extends TypeMirror> smartTypes = env.getSmartTypes();
            final CompilationController controller = env.getController();
            final Trees trees = controller.getTrees();
            final Elements elements = controller.getElements();
            final ElementUtilities eu = controller.getElementUtilities();
            final Types types = controller.getTypes();
            final TreeUtilities tu = controller.getTreeUtilities();
            TypeElement typeElem = type.getKind() == TypeKind.DECLARED ? (TypeElement)((DeclaredType)type).asElement() : null;
            final boolean isStatic = elem != null && (elem.getKind().isClass() || elem.getKind().isInterface() || elem.getKind() == TYPE_PARAMETER) && elem.asType().getKind() != TypeKind.ERROR;
            if (isStatic && elem.getKind() == ElementKind.TYPE_PARAMETER)
                return;
            final boolean isThisCall = elem != null && elem.getKind().isField() && elem.getSimpleName().contentEquals(THIS_KEYWORD);
            final boolean isSuperCall = elem != null && elem.getKind().isField() && elem.getSimpleName().contentEquals(SUPER_KEYWORD);
            final Scope scope = env.getScope();
            if ((isThisCall || isSuperCall) && tu.isStaticContext(scope))
                return;
            final boolean[] ctorSeen = {false};
            final boolean[] nestedClassSeen = {false};
            final TypeElement enclClass = scope.getEnclosingClass();
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    switch (e.getKind()) {
                        case FIELD:
                            if (!startsWith(env, e.getSimpleName().toString()))
                                return false;
                            if (e.getSimpleName().contentEquals(THIS_KEYWORD) || e.getSimpleName().contentEquals(SUPER_KEYWORD)) {
                                boolean b = false;
                                TypeElement cls = enclClass;                                
                                while(cls != null) {
                                    if (cls == elem)
                                        return isOfKindAndType(asMemberOf(e, t, types), e, kinds, baseType, scope, trees, types);
                                    TypeElement outer = eu.enclosingTypeElement(cls);
                                    cls = !cls.getModifiers().contains(STATIC) ? outer : null;
                                }
                                return false;
                            }
                            if (isStatic) {
                                if (!e.getModifiers().contains(STATIC))
                                    return false;
                            } else {
                                if (queryType == COMPLETION_QUERY_TYPE && e.getModifiers().contains(STATIC)) {
                                    if ((Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e))
                                            && isOfKindAndType(asMemberOf(e, t, types), e, kinds, baseType, scope, trees, types)
                                            && env.isAccessible(scope, e, t, isSuperCall)
                                            && ((isStatic && !inImport) || !e.getSimpleName().contentEquals(CLASS_KEYWORD))) {
                                        hasAdditionalItems = 2; //instance members only
                                    }
                                    return false;
                                }
                            }
                            return (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    isOfKindAndType(asMemberOf(e, t, types), e, kinds, baseType, scope, trees, types) &&
                                    env.isAccessible(scope, e, t, isSuperCall) &&
                                    ((isStatic && !inImport) || !e.getSimpleName().contentEquals(CLASS_KEYWORD));
                        case ENUM_CONSTANT:
                        case EXCEPTION_PARAMETER:
                        case LOCAL_VARIABLE:
                        case RESOURCE_VARIABLE:
                        case PARAMETER:
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    isOfKindAndType(asMemberOf(e, t, types), e, kinds, baseType, scope, trees, types) &&
                                    env.isAccessible(scope, e, t, isSuperCall);
                        case METHOD:
                            String sn = e.getSimpleName().toString();
                            if (isStatic) {
                                if (!e.getModifiers().contains(STATIC))
                                    return false;
                            } else {
                                if (queryType == COMPLETION_QUERY_TYPE && e.getModifiers().contains(STATIC)) {
                                    if (startsWith(env, sn)
                                            && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e))
                                            && isOfKindAndType(((ExecutableType) asMemberOf(e, t, types)).getReturnType(), e, kinds, baseType, scope, trees, types)
                                            && env.isAccessible(scope, e, t, isSuperCall)
                                            && (!Utilities.isExcludeMethods() || !Utilities.isExcluded(Utilities.getElementName(e.getEnclosingElement(), true) + "." + sn))) { //NOI18N
                                        hasAdditionalItems = 2; //instance members only
                                    }
                                    return false;
                                }
                            }
                            return startsWith(env, sn) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    isOfKindAndType(((ExecutableType)asMemberOf(e, t, types)).getReturnType(), e, kinds, baseType, scope, trees, types) &&
                                    env.isAccessible(scope, e, t, isSuperCall) &&
                                    (!Utilities.isExcludeMethods() || !Utilities.isExcluded(Utilities.getElementName(e.getEnclosingElement(), true) + "." + sn)); //NOI18N
                        case CLASS:
                        case ENUM:
                        case INTERFACE:
                        case ANNOTATION_TYPE:
                            if (!e.getModifiers().contains(STATIC))
                                nestedClassSeen[0] = true;
                            return startsWith(env, e.getSimpleName().toString()) &&
                                    (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    isOfKindAndType(e.asType(), e, kinds, baseType, scope, trees, types) &&
                                    (!env.isAfterExtends() || containsAccessibleNonFinalType(e, scope, trees)) &&
                                    env.isAccessible(scope, e, t, isSuperCall) && isStatic;
                        case CONSTRUCTOR:
                            ctorSeen[0] = true;
                            return (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    isOfKindAndType(e.getEnclosingElement().asType(), e, kinds, baseType, scope, trees, types) &&
                                    (env.isAccessible(scope, e, t, isSuperCall) || (elem.getModifiers().contains(ABSTRACT) && !e.getModifiers().contains(PRIVATE))) &&
                                    isStatic;
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getMembers(type, acceptor)) {
                switch (e.getKind()) {
                    case ENUM_CONSTANT:
                    case EXCEPTION_PARAMETER:
                    case FIELD:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case PARAMETER:
                        String name = e.getSimpleName().toString();
                        if (THIS_KEYWORD.equals(name) || CLASS_KEYWORD.equals(name) || SUPER_KEYWORD.equals(name)) {
                            results.add(JavaCompletionItem.createKeywordItem(name, null, anchorOffset, isOfSmartType(env, e.asType(), smartTypes)));
                        } else {
                            TypeMirror tm = type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType();
                            results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, tm, anchorOffset, autoImport ? env.getReferencesCount() : null, typeElem != e.getEnclosingElement(), elements.isDeprecated(e), env.addSemicolon(), isOfSmartType(env, tm, smartTypes), env.assignToVarPos(), env.getWhiteList()));
                        }
                        break;
                    case CONSTRUCTOR:
                        ExecutableType et = (ExecutableType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                        results.add(JavaCompletionItem.createExecutableItem(env.getController(), (ExecutableElement)e, et, anchorOffset, autoImport ? env.getReferencesCount() : null, typeElem != e.getEnclosingElement(), elements.isDeprecated(e), inImport, false, isOfSmartType(env, type, smartTypes), env.assignToVarPos(), false, env.getWhiteList()));
                        break;
                    case METHOD:
                        et = (ExecutableType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                        results.add(JavaCompletionItem.createExecutableItem(env.getController(), (ExecutableElement)e, et, anchorOffset, autoImport ? env.getReferencesCount() : null, typeElem != e.getEnclosingElement(), elements.isDeprecated(e), inImport, env.addSemicolon(), isOfSmartType(env, et.getReturnType(), smartTypes), env.assignToVarPos(), false, env.getWhiteList()));
                        break;
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        DeclaredType dt = (DeclaredType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                        results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, dt, anchorOffset, null, elements.isDeprecated(e), insideNew, insideNew || env.isInsideClass(), true, isOfSmartType(env, dt, smartTypes), autoImport, env.getWhiteList()));
                        break;
                }
            }
            if (!ctorSeen[0] && kinds.contains(CONSTRUCTOR) && elem.getKind().isInterface()) {
                results.add(JavaCompletionItem.createDefaultConstructorItem((TypeElement)elem, anchorOffset, isOfSmartType(env, type, smartTypes)));
            }
            if (!isStatic && nestedClassSeen[0]) {
                addKeyword(env, NEW_KEYWORD, SPACE, false);
            }
        }
        
        private void addThisOrSuperConstructor(final Env env, final TypeMirror type, final Element elem, final String name, final ExecutableElement toExclude) throws IOException {
            final CompilationController controller = env.getController();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final Trees trees = controller.getTrees();
            final Scope scope = env.getScope();
            final boolean[] ctorSeen = {false};
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    switch (e.getKind()) {
                        case CONSTRUCTOR:
                            ctorSeen[0] = true;
                            return toExclude != e && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) &&
                                    (trees.isAccessible(scope, e, (DeclaredType)t) || (elem.getModifiers().contains(ABSTRACT) && !e.getModifiers().contains(PRIVATE)));
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getMembers(type, acceptor)) {
                if (e.getKind() == CONSTRUCTOR) {
                    ExecutableType et = (ExecutableType)(type.getKind() == TypeKind.DECLARED ? types.asMemberOf((DeclaredType)type, e) : e.asType());
                    results.add(JavaCompletionItem.createThisOrSuperConstructorItem(env.getController(), (ExecutableElement)e, et, anchorOffset, elements.isDeprecated(e), name, env.getWhiteList()));
                }
            }
        }

        private void addEnumConstants(Env env, TypeElement elem) {
            Elements elements = env.getController().getElements();
            for(Element e : elem.getEnclosedElements()) {
                if (e.getKind() == ENUM_CONSTANT) {
                    String name = e.getSimpleName().toString();
                    if (startsWith(env, name) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)))
                        results.add(JavaCompletionItem.createVariableItem(env.getController(), (VariableElement)e, e.asType(), anchorOffset, null, false, elements.isDeprecated(e), false, false, env.assignToVarPos(), env.getWhiteList()));
                }
            }
        }
        
        private void addPackageContent(final Env env, PackageElement pe, EnumSet<ElementKind> kinds, DeclaredType baseType, boolean insideNew, boolean insidePkgStmt) throws IOException {
            Set<? extends TypeMirror> smartTypes = queryType == COMPLETION_QUERY_TYPE ? env.getSmartTypes() : null;
            CompilationController controller = env.getController();
            Elements elements = controller.getElements();
            Types types = controller.getTypes();
            Trees trees = controller.getTrees();
            Scope scope = env.getScope();
            for(Element e : pe.getEnclosedElements()) {
                if (e.getKind().isClass() || e.getKind().isInterface()) {
                    String name = e.getSimpleName().toString();
                        if (startsWith(env, name) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e))
                        && trees.isAccessible(scope, (TypeElement)e)
                        && isOfKindAndType(e.asType(), e, kinds, baseType, scope, trees, types)
                        && !Utilities.isExcluded(Utilities.getElementName(e, true))) {
                            results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, (DeclaredType)e.asType(), anchorOffset, null, elements.isDeprecated(e), insideNew, insideNew || env.isInsideClass(), true, isOfSmartType(env, e.asType(), smartTypes), false, env.getWhiteList()));
                    }
                }
            }
            String pkgName = pe.getQualifiedName() + "."; //NOI18N
            addPackages(env, pkgName, insidePkgStmt);
        }
        
        private void addPackages(Env env, String fqnPrefix, boolean inPkgStmt) {
            if (fqnPrefix == null)
                fqnPrefix = EMPTY;
            String prefix = env.getPrefix() != null ? fqnPrefix + env.getPrefix() : null;
            for (String pkgName : env.getController().getClasspathInfo().getClassIndex().getPackageNames(fqnPrefix, true, EnumSet.allOf(ClassIndex.SearchScope.class)))
                if (startsWith(env, pkgName, prefix) && !Utilities.isExcluded(pkgName + ".")) //NOI18N
                    results.add(JavaCompletionItem.createPackageItem(pkgName, anchorOffset, inPkgStmt));
        }
        
        private void addTypes(Env env, EnumSet<ElementKind> kinds, DeclaredType baseType) throws IOException {
            if (queryType == COMPLETION_ALL_QUERY_TYPE) {
                if (baseType == null) {
                    addAllTypes(env, kinds);
                } else {
                    Elements elements = env.getController().getElements();
                    Set<? extends Element> excludes = env.getExcludes();
                    for(DeclaredType subtype : getSubtypesOf(env, baseType)) {
                        TypeElement elem = (TypeElement)subtype.asElement();
                        if ((excludes == null || !excludes.contains(elem)) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(elem)) && (!env.isAfterExtends() || !elem.getModifiers().contains(Modifier.FINAL)))
                            results.add(JavaCompletionItem.createTypeItem(env.getController(), elem, subtype, anchorOffset, env.getReferencesCount(), elements.isDeprecated(elem), env.isInsideNew(), env.isInsideNew() || env.isInsideClass(), false, true, false, env.getWhiteList()));
                    }
                }
            } else {
                addLocalAndImportedTypes(env, kinds, baseType);
                hasAdditionalItems = 1; //imported items only
            }
            addPackages(env, null, false);
        }
        
        private void addLocalAndImportedTypes(final Env env, final EnumSet<ElementKind> kinds, final DeclaredType baseType) throws IOException {
            final CompilationController controller = env.getController();
            final Trees trees = controller.getTrees();
            final Elements elements = controller.getElements();
            final Types types = controller.getTypes();
            final TreeUtilities tu = controller.getTreeUtilities();
            final Scope scope = env.getScope();
            final ExecutableElement enclMethod = scope.getEnclosingMethod();
            final TypeElement enclClass = scope.getEnclosingClass();
            final boolean isStatic = enclClass == null ? false :
                (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    if ((env.getExcludes() == null || !env.getExcludes().contains(e)) && (e.getKind().isClass() || e.getKind().isInterface() || e.getKind() == TYPE_PARAMETER) && (!env.isAfterExtends() || containsAccessibleNonFinalType(e, scope, trees))) {
                        String name = e.getSimpleName().toString();
                        return name.length() > 0 && !Character.isDigit(name.charAt(0)) && startsWith(env, name) &&
                                (!isStatic || e.getModifiers().contains(STATIC) || e.getEnclosingElement() == enclMethod) && (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) && isOfKindAndType(e.asType(), e, kinds, baseType, scope, trees, types);
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor)) {
                switch (e.getKind()) {
                    case CLASS:
                    case ENUM:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, (DeclaredType)e.asType(), anchorOffset, null, elements.isDeprecated(e), env.isInsideNew(), env.isInsideNew() || env.isInsideClass(), false, false, false, env.getWhiteList()));
                        env.addToExcludes(e);
                        break;
                    case TYPE_PARAMETER:
                        results.add(JavaCompletionItem.createTypeParameterItem((TypeParameterElement)e, anchorOffset));
                        break;                        
                }                
            }
            acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    if ((e.getKind().isClass() || e.getKind().isInterface())) {
                        return (env.getExcludes() == null || !env.getExcludes().contains(e)) && startsWith(env, e.getSimpleName().toString()) &&
                                (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(e)) && trees.isAccessible(scope, (TypeElement)e) &&
                                isOfKindAndType(e.asType(), e, kinds, baseType, scope, trees, types) && (!env.isAfterExtends() || containsAccessibleNonFinalType(e, scope, trees));
                    }
                    return false;
                }
            };
            for (TypeElement e : controller.getElementUtilities().getGlobalTypes(acceptor)) {
                results.add(JavaCompletionItem.createTypeItem(env.getController(), (TypeElement)e, (DeclaredType)e.asType(), anchorOffset, null, elements.isDeprecated(e), env.isInsideNew(), env.isInsideNew() || env.isInsideClass(), false, false, false, env.getWhiteList()));
            }
        }
        
        private void addAllTypes(Env env, EnumSet<ElementKind> kinds) {
            String prefix = env.getPrefix();
            CompilationController controller = env.getController();
            Set<? extends Element> excludes = env.getExcludes();
            Set<ElementHandle<Element>> excludeHandles = null;
            if (excludes != null) {
                excludeHandles = new HashSet<ElementHandle<Element>>(excludes.size());
                for (Element el : excludes) {
                    excludeHandles.add(ElementHandle.create(el));
                }
            }
            ClassIndex.NameKind kind = env.isCamelCasePrefix() ?
                Utilities.isCaseSensitive() ? ClassIndex.NameKind.CAMEL_CASE : ClassIndex.NameKind.CAMEL_CASE_INSENSITIVE :
                Utilities.isCaseSensitive() ? ClassIndex.NameKind.PREFIX : ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX;
            Set<ElementHandle<TypeElement>> declaredTypes = controller.getClasspathInfo().getClassIndex().getDeclaredTypes(prefix != null ? prefix : EMPTY, kind, EnumSet.allOf(ClassIndex.SearchScope.class));
            results.ensureCapacity(results.size() + declaredTypes.size());
            for(ElementHandle<TypeElement> name : declaredTypes) {
                if (excludeHandles != null && excludeHandles.contains(name) || isAnnonInner(name))
                    continue;
                results.add(LazyTypeCompletionItem.create(name, kinds, anchorOffset, env.getReferencesCount(), controller.getSnapshot().getSource(), env.isInsideNew(), env.isInsideNew() || env.isInsideClass(), env.afterExtends, env.getWhiteList()));
            }
        }
        
        private List<DeclaredType> getSubtypesOf(Env env, DeclaredType baseType) throws IOException {
            if (((TypeElement)baseType.asElement()).getQualifiedName().contentEquals(JAVA_LANG_OBJECT))
                return Collections.emptyList();
            LinkedList<DeclaredType> subtypes = new LinkedList<DeclaredType>();
            String prefix = env.getPrefix();
            CompilationController controller = env.getController();
            Types types = controller.getTypes();
            Trees trees = controller.getTrees();
            Scope scope = env.getScope();
            if (prefix != null && prefix.length() > 2 && baseType.getTypeArguments().isEmpty()) {
                ClassIndex.NameKind kind = env.isCamelCasePrefix() ?
                    Utilities.isCaseSensitive() ? ClassIndex.NameKind.CAMEL_CASE : ClassIndex.NameKind.CAMEL_CASE_INSENSITIVE :
                    Utilities.isCaseSensitive() ? ClassIndex.NameKind.PREFIX : ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX;
                for(ElementHandle<TypeElement> handle : controller.getClasspathInfo().getClassIndex().getDeclaredTypes(prefix, kind, EnumSet.allOf(ClassIndex.SearchScope.class))) {
                    TypeElement te = handle.resolve(controller);
                    if (te != null && trees.isAccessible(scope, te) && types.isSubtype(types.getDeclaredType(te), baseType))
                        subtypes.add(types.getDeclaredType(te));
                }
            } else {
                HashSet<TypeElement> elems = new HashSet<TypeElement>();
                LinkedList<DeclaredType> bases = new LinkedList<DeclaredType>();
                bases.add(baseType);
                ClassIndex index = controller.getClasspathInfo().getClassIndex();
                while(!bases.isEmpty()) {
                    DeclaredType head = bases.remove();
                    TypeElement elem = (TypeElement)head.asElement();
                    if (!elems.add(elem))
                        continue;
                    if (startsWith(env, elem.getSimpleName().toString()))
                        subtypes.add(head);
                    List<? extends TypeMirror> tas = head.getTypeArguments();
                    boolean isRaw = !tas.iterator().hasNext();
                    subtypes:
                    for (ElementHandle<TypeElement> eh : index.getElements(ElementHandle.create(elem), EnumSet.of(ClassIndex.SearchKind.IMPLEMENTORS),EnumSet.allOf(ClassIndex.SearchScope.class))) {
                        TypeElement e = eh.resolve(controller);
                        if (e != null) {
                            if (trees.isAccessible(scope, e)) {
                                if (isRaw) {
                                    DeclaredType dt = types.getDeclaredType(e);
                                    bases.add(dt);
                                } else {
                                    HashMap<Element, TypeMirror> map = new HashMap<Element, TypeMirror>();
                                    TypeMirror sup = e.getSuperclass();
                                    if (sup.getKind() == TypeKind.DECLARED && ((DeclaredType)sup).asElement() == elem) {
                                        DeclaredType dt = (DeclaredType)sup;
                                        Iterator<? extends TypeMirror> ittas = tas.iterator();
                                        Iterator<? extends TypeMirror> it = dt.getTypeArguments().iterator();
                                        while(it.hasNext() && ittas.hasNext()) {
                                            TypeMirror basetm = ittas.next();
                                            TypeMirror stm = it.next();
                                            if (basetm != stm) {
                                                if (stm.getKind() == TypeKind.TYPEVAR) {
                                                    map.put(((TypeVariable)stm).asElement(), basetm);
                                                } else {
                                                    continue subtypes;
                                                }
                                            }
                                        }
                                        if (it.hasNext() != ittas.hasNext()) {
                                            continue subtypes;
                                        }
                                    } else {
                                        for (TypeMirror tm : e.getInterfaces()) {
                                            if (((DeclaredType)tm).asElement() == elem) {
                                                DeclaredType dt = (DeclaredType)tm;
                                                Iterator<? extends TypeMirror> ittas = tas.iterator();
                                                Iterator<? extends TypeMirror> it = dt.getTypeArguments().iterator();
                                                while(it.hasNext() && ittas.hasNext()) {
                                                    TypeMirror basetm = ittas.next();
                                                    TypeMirror stm = it.next();
                                                    if (basetm != stm) {
                                                        if (stm.getKind() == TypeKind.TYPEVAR) {
                                                            map.put(((TypeVariable)stm).asElement(), basetm);
                                                        } else {
                                                            continue subtypes;
                                                        }
                                                    }
                                                }
                                                if (it.hasNext() != ittas.hasNext()) {
                                                    continue subtypes;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    bases.add(getDeclaredType(e, map, types));
                                }
                            }
                        } else {
                            Logger.getLogger("global").log(Level.FINE, String.format("Cannot resolve: %s on bootpath: %s classpath: %s sourcepath: %s\n", eh.toString(),
                                    controller.getClasspathInfo().getClassPath(ClasspathInfo.PathKind.BOOT),
                                    controller.getClasspathInfo().getClassPath(ClasspathInfo.PathKind.COMPILE),
                                    controller.getClasspathInfo().getClassPath(ClasspathInfo.PathKind.SOURCE)));
                        }
                    }
                }
            }
            return subtypes;
        }
        
        private void addMethodArguments(Env env, MethodInvocationTree mit) throws IOException {
            CompilationController controller = env.getController();
            TreePath path = env.getPath();
            CompilationUnitTree root = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            List<Tree> argTypes = getArgumentsUpToPos(env, mit.getArguments(), (int)sourcePositions.getEndPosition(root, mit.getMethodSelect()), env.getOffset(), true);
            if (argTypes != null) {
                controller.toPhase(Phase.RESOLVED);
                TypeMirror[] types = new TypeMirror[argTypes.size()];
                int j = 0;
                for (Tree t : argTypes)
                    types[j++] = controller.getTrees().getTypeMirror(new TreePath(path, t));
                List<Pair<ExecutableElement, ExecutableType>> methods = null;
                String name = null;
                Tree mid = mit.getMethodSelect();
                path = new TreePath(path, mid);
                switch (mid.getKind()) {
                    case MEMBER_SELECT: {
                        ExpressionTree exp = ((MemberSelectTree)mid).getExpression();
                        path = new TreePath(path, exp);
                        final Trees trees = controller.getTrees();
                        final TypeMirror type = trees.getTypeMirror(path);
                        final Element element = trees.getElement(path);
                        final boolean isStatic = element != null && (element.getKind().isClass() || element.getKind().isInterface() || element.getKind() == TYPE_PARAMETER);
                        final boolean isSuperCall = element != null && element.getKind().isField() && element.getSimpleName().contentEquals(SUPER_KEYWORD);
                        final Scope scope = env.getScope();
                        TypeElement enclClass = scope.getEnclosingClass();
                        final TypeMirror enclType = enclClass != null ? enclClass.asType() : null;
                        ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                            public boolean accept(Element e, TypeMirror t) {
                                return (!isStatic || e.getModifiers().contains(STATIC) || e.getKind() == CONSTRUCTOR) && (t.getKind() != TypeKind.DECLARED || trees.isAccessible(scope, e, (DeclaredType)(isSuperCall && enclType != null ? enclType : t)));
                            }
                        };
                        methods = getMatchingExecutables(type, controller.getElementUtilities().getMembers(type, acceptor), ((MemberSelectTree)mid).getIdentifier().toString(), types, controller.getTypes());
                        break;
                    }
                    case IDENTIFIER: {
                        final Scope scope = env.getScope();
                        final TreeUtilities tu = controller.getTreeUtilities();
                        final Trees trees = controller.getTrees();
                        final TypeElement enclClass = scope.getEnclosingClass();
                        final boolean isStatic = enclClass != null ? (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic())) : false;
                        final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
                        final ExecutableElement method = scope.getEnclosingMethod();
                        ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                            public boolean accept(Element e, TypeMirror t) {
                                switch (e.getKind()) {
                                    case LOCAL_VARIABLE:
                                    case RESOURCE_VARIABLE:
                                    case EXCEPTION_PARAMETER:
                                    case PARAMETER:
                                        return (method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                                !illegalForwardRefs.contains(e);
                                    case FIELD:
                                        if (illegalForwardRefs.contains(e))
                                            return false;
                                        if (e.getSimpleName().contentEquals(THIS_KEYWORD) || e.getSimpleName().contentEquals(SUPER_KEYWORD))
                                            return !isStatic;
                                    default:
                                        return (!isStatic || e.getModifiers().contains(STATIC)) && trees.isAccessible(scope, e, (DeclaredType)t);
                                }
                            }
                        };
                        name = ((IdentifierTree)mid).getName().toString();
                        if (SUPER_KEYWORD.equals(name) && enclClass != null) {
                            TypeMirror superclass = enclClass.getSuperclass();
                            methods = getMatchingExecutables(superclass, controller.getElementUtilities().getMembers(superclass, acceptor), INIT, types, controller.getTypes());
                        } else if (THIS_KEYWORD.equals(name) && enclClass != null) {
                            TypeMirror thisclass = enclClass.asType();
                            methods = getMatchingExecutables(thisclass, controller.getElementUtilities().getMembers(thisclass, acceptor), INIT, types, controller.getTypes());
                        } else {
                            methods = getMatchingExecutables(enclClass != null ? enclClass.asType() : null, controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), name, types, controller.getTypes());
                            name = null;
                        }
                        break;
                    }
                }
                if (methods != null) {
                    Elements elements = controller.getElements();
                    for (Pair<ExecutableElement, ExecutableType> method : methods)
                        if (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(method.a))
                            results.add(JavaCompletionItem.createParametersItem(env.getController(), method.a, method.b, anchorOffset, elements.isDeprecated(method.a), types.length, name));
                }
            }
        }
        
        private void addConstructorArguments(Env env, NewClassTree nct) throws IOException {
            CompilationController controller = env.getController();
            TreePath path = env.getPath();
            CompilationUnitTree root = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            List<Tree> argTypes = getArgumentsUpToPos(env, nct.getArguments(), (int)sourcePositions.getEndPosition(root, nct.getIdentifier()), env.getOffset(), true);
            if (argTypes != null) {
                controller.toPhase(Phase.RESOLVED);
                TypeMirror[] types = new TypeMirror[argTypes.size()];
                int j = 0;
                for (Tree t : argTypes)
                    types[j++] = controller.getTrees().getTypeMirror(new TreePath(path, t));
                path = new TreePath(path, nct.getIdentifier());
                final Trees trees = controller.getTrees();
                final TypeMirror type = trees.getTypeMirror(path);
                final Element el = trees.getElement(path);
                final Scope scope = env.getScope();
                final boolean isAnonymous = nct.getClassBody() != null || (el != null && (el.getKind().isInterface() || el.getModifiers().contains(ABSTRACT)));
                ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                    public boolean accept(Element e, TypeMirror t) {
                        return e.getKind() == CONSTRUCTOR && (trees.isAccessible(scope, e, (DeclaredType)t) || isAnonymous && e.getModifiers().contains(PROTECTED));
                    }
                };
                List<Pair<ExecutableElement, ExecutableType>> ctors = getMatchingExecutables(type, controller.getElementUtilities().getMembers(type, acceptor), INIT, types, controller.getTypes());
                Elements elements = controller.getElements();
                for (Pair<ExecutableElement, ExecutableType> ctor : ctors)
                    if (Utilities.isShowDeprecatedMembers() || !elements.isDeprecated(ctor.a))
                        results.add(JavaCompletionItem.createParametersItem(env.getController(), ctor.a, ctor.b, anchorOffset, elements.isDeprecated(ctor.a), types.length, null));
            }
        }

        private void addAttributeValues(Env env, Element element, AnnotationMirror annotation, ExecutableElement member) throws IOException {
            CompilationController controller = env.getController();
            TreeUtilities tu = controller.getTreeUtilities();
            ElementUtilities eu = controller.getElementUtilities();
            for (javax.annotation.processing.Completion completion : SourceUtils.getAttributeValueCompletions(controller, element, annotation, member, env.getPrefix())) {
                String value = completion.getValue().trim();
                if (value.length() > 0 && startsWith(env, value)) {
                    TypeMirror type = member.getReturnType();
                    TypeElement typeElement = null;
                    while (type.getKind() == TypeKind.ARRAY) {
                        type = ((ArrayType)type).getComponentType();
                    }
                    if (type.getKind() == TypeKind.DECLARED) {
                        CharSequence fqn = ((TypeElement)((DeclaredType)type).asElement()).getQualifiedName();
                        if ("java.lang.Class".contentEquals(fqn)) {
                            String name = value.endsWith(".class") ?  value.substring(0, value.length() - 6) : value; //NOI18N
                            TypeMirror tm = tu.parseType(name, eu.outermostTypeElement(element));
                            typeElement = tm != null && tm.getKind() == TypeKind.DECLARED ? (TypeElement)((DeclaredType)tm).asElement() : null;
                            if (typeElement != null && startsWith(env, typeElement.getSimpleName().toString()))
                                env.addToExcludes(typeElement);
                        }
                    }
                    results.add(JavaCompletionItem.createAttributeValueItem(env.getController(), value, completion.getMessage(), typeElement, anchorOffset, env.getReferencesCount(), env.getWhiteList()));
                }
            }
        }

        private void addKeyword(Env env, String kw, String postfix, boolean smartType) {
            if (Utilities.startsWith(kw, env.getPrefix()))
                results.add(JavaCompletionItem.createKeywordItem(kw, postfix, anchorOffset, smartType));
        }
        
        private void addKeywordsForCU(Env env) {
            List<String> kws = new ArrayList<String>();
            int offset = env.getOffset();
            String prefix = env.getPrefix();
            CompilationUnitTree cu = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            kws.add(ABSTRACT_KEYWORD);
            kws.add(CLASS_KEYWORD);
            kws.add(ENUM_KEYWORD);
            kws.add(FINAL_KEYWORD);
            kws.add(INTERFACE_KEYWORD);
            boolean beforeAnyClass = true;
            boolean beforePublicClass = true;
            for(Tree t : cu.getTypeDecls()) {
                if (TreeUtilities.CLASS_TREE_KINDS.contains(t.getKind())) {
                    int pos = (int)sourcePositions.getEndPosition(cu, t);
                    if (pos != Diagnostic.NOPOS && offset >= pos) {
                        beforeAnyClass = false;
                        if (((ClassTree)t).getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                            beforePublicClass = false;
                            break;
                        }
                    }
                }
            }
            if (beforePublicClass)
                kws.add(PUBLIC_KEYWORD);
            if (beforeAnyClass) {
                kws.add(IMPORT_KEYWORD);
                Tree firstImport = null;
                for(Tree t : cu.getImports()) {
                    firstImport = t;
                    break;
                }
                Tree pd = cu.getPackageName();
                if ((pd != null && offset <= sourcePositions.getStartPosition(cu, cu)) ||
                        (pd == null && (firstImport == null || sourcePositions.getStartPosition(cu, firstImport) >= offset)))
                    kws.add(PACKAGE_KEYWORD);
            }
            for (String kw : kws) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
        }
        
        private void addKeywordsForClassBody(Env env) {
            String prefix = env.getPrefix();
            for (String kw : CLASS_BODY_KEYWORDS)
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            if (env.getController().getSourceVersion().compareTo(SourceVersion.RELEASE_8) >= 0
                    && Utilities.startsWith(DEFAULT_KEYWORD, prefix)
                    && Utilities.getPathElementOfKind(Tree.Kind.INTERFACE, env.getPath()) != null) {
                results.add(JavaCompletionItem.createKeywordItem(DEFAULT_KEYWORD, SPACE, anchorOffset, false));
            }
            addPrimitiveTypeKeywords(env);
        }
        
        private void addKeywordsForBlock(Env env) {
            String prefix = env.getPrefix();
            for (String kw : STATEMENT_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, null, anchorOffset, false));
            }
            for (String kw : BLOCK_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
            if (Utilities.startsWith(RETURN_KEYWORD, prefix)) {
                TreePath tp = Utilities.getPathElementOfKind(EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION), env.getPath());
                String postfix = SPACE;
                if (tp != null) {
                    if (tp.getLeaf().getKind() == Tree.Kind.METHOD) {
                        Tree rt = ((MethodTree)tp.getLeaf()).getReturnType();
                        if (rt == null || (rt.getKind() == Tree.Kind.PRIMITIVE_TYPE && ((PrimitiveTypeTree)rt).getPrimitiveTypeKind() == TypeKind.VOID))
                            postfix = SEMI;
                    } else {
                        TypeMirror tm = env.getController().getTrees().getTypeMirror(tp);
                        if (tm != null && tm.getKind() == TypeKind.DECLARED) {
                            ExecutableType dt = env.getController().getTypeUtilities().getDescriptorType((DeclaredType)tm);
                            if (dt != null && dt.getReturnType().getKind() == TypeKind.VOID) {
                                postfix = SEMI;
                            }
                        }
                    }
                }
                results.add(JavaCompletionItem.createKeywordItem(RETURN_KEYWORD, postfix, anchorOffset, false));
            }
            boolean caseAdded = false;
            boolean breakAdded = false;
            boolean continueAdded = false;
            TreePath tp = env.getPath();
            while (tp != null) {
                switch (tp.getLeaf().getKind()) {
                    case SWITCH:
                        CaseTree lastCase = null;
                        CompilationUnitTree root = env.getRoot();
                        SourcePositions sourcePositions = env.getSourcePositions();
                        for (CaseTree t : ((SwitchTree)tp.getLeaf()).getCases()) {
                            if (sourcePositions.getStartPosition(root, t) >= env.getOffset())
                                break;
                            lastCase = t;
                        }
                        if (! caseAdded && (lastCase == null || lastCase.getExpression() != null)) {
                            caseAdded = true;
                            if (Utilities.startsWith(CASE_KEYWORD, prefix))
                                results.add(JavaCompletionItem.createKeywordItem(CASE_KEYWORD, SPACE, anchorOffset, false));
                            if (Utilities.startsWith(DEFAULT_KEYWORD, prefix))
                                results.add(JavaCompletionItem.createKeywordItem(DEFAULT_KEYWORD, COLON, anchorOffset, false));
                        }
                        if (!breakAdded && Utilities.startsWith(BREAK_KEYWORD, prefix)) {
                            breakAdded = true;
                            results.add(JavaCompletionItem.createKeywordItem(BREAK_KEYWORD, withinLabeledStatement(env) ? null : SEMI, anchorOffset, false));
                        }
                        break;
                    case DO_WHILE_LOOP:
                    case ENHANCED_FOR_LOOP:
                    case FOR_LOOP:
                    case WHILE_LOOP:
                        if (! breakAdded && Utilities.startsWith(BREAK_KEYWORD, prefix)) {
                            breakAdded = true;
                            results.add(JavaCompletionItem.createKeywordItem(BREAK_KEYWORD, withinLabeledStatement(env) ? null : SEMI, anchorOffset, false));
                        }
                        if (!continueAdded && Utilities.startsWith(CONTINUE_KEYWORD, prefix)) {
                            continueAdded = true;
                            results.add(JavaCompletionItem.createKeywordItem(CONTINUE_KEYWORD, withinLabeledStatement(env) ? null : SEMI, anchorOffset, false));
                        }
                        break;
                }
                tp = tp.getParentPath();
            }
        }
        
        private void addKeywordsForStatement(Env env) {
            String prefix = env.getPrefix();
            for (String kw : STATEMENT_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, null, anchorOffset, false));
            }
            for (String kw : STATEMENT_SPACE_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
            if (Utilities.startsWith(RETURN_KEYWORD, prefix)) {
                TreePath tp = Utilities.getPathElementOfKind(EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION), env.getPath());
                String postfix = SPACE;
                if (tp != null) {
                    if (tp.getLeaf().getKind() == Tree.Kind.METHOD) {
                        Tree rt = ((MethodTree)tp.getLeaf()).getReturnType();
                        if (rt == null || (rt.getKind() == Tree.Kind.PRIMITIVE_TYPE && ((PrimitiveTypeTree)rt).getPrimitiveTypeKind() == TypeKind.VOID))
                            postfix = SEMI;
                    } else {
                        TypeMirror tm = env.getController().getTrees().getTypeMirror(tp);
                        if (tm != null && tm.getKind() == TypeKind.DECLARED) {
                            ExecutableType dt = env.getController().getTypeUtilities().getDescriptorType((DeclaredType)tm);
                            if (dt != null && dt.getReturnType().getKind() == TypeKind.VOID) {
                                postfix = SEMI;
                            }
                        }
                    }
                }
                results.add(JavaCompletionItem.createKeywordItem(RETURN_KEYWORD, postfix, anchorOffset, false));
            }
            TreePath tp = env.getPath();
            while (tp != null) {
                switch (tp.getLeaf().getKind()) {
                    case DO_WHILE_LOOP:
                    case ENHANCED_FOR_LOOP:
                    case FOR_LOOP:
                    case WHILE_LOOP:
                        if (Utilities.startsWith(CONTINUE_KEYWORD, prefix))
                            results.add(JavaCompletionItem.createKeywordItem(CONTINUE_KEYWORD, SEMI, anchorOffset, false));
                    case SWITCH:
                        if (Utilities.startsWith(BREAK_KEYWORD, prefix))
                            results.add(JavaCompletionItem.createKeywordItem(BREAK_KEYWORD, SEMI, anchorOffset, false));
                        break;
                }
                tp = tp.getParentPath();
            }
        }
        
        private void addValueKeywords(Env env) throws IOException {
            String prefix = env.getPrefix();
            boolean smartType = false;
            if (queryType == COMPLETION_QUERY_TYPE) {
                Set<? extends TypeMirror> smartTypes = env.getSmartTypes();
                if (smartTypes != null && !smartTypes.isEmpty()) {
                    for (TypeMirror st : smartTypes) {
                        if (st.getKind() == TypeKind.BOOLEAN) {
                            smartType = true;
                            break;
                        }
                    }
                }
            }
            if (Utilities.startsWith(FALSE_KEYWORD, prefix))
                results.add(JavaCompletionItem.createKeywordItem(FALSE_KEYWORD, null, anchorOffset, smartType));
            if (Utilities.startsWith(TRUE_KEYWORD, prefix))
                results.add(JavaCompletionItem.createKeywordItem(TRUE_KEYWORD, null, anchorOffset, smartType));
            if (Utilities.startsWith(NULL_KEYWORD, prefix))
                results.add(JavaCompletionItem.createKeywordItem(NULL_KEYWORD, null, anchorOffset, false));
            if (Utilities.startsWith(NEW_KEYWORD, prefix))
                results.add(JavaCompletionItem.createKeywordItem(NEW_KEYWORD, SPACE, anchorOffset, false));
        }

        private void addPrimitiveTypeKeywords(Env env) {
            String prefix = env.getPrefix();
            for (String kw : PRIM_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, null, anchorOffset, false));
            }
        }
        
        private void addClassModifiers(Env env, Set<Modifier> modifiers) {
            String prefix = env.getPrefix();
            List<String> kws = new ArrayList<String>();
            if (!modifiers.contains(PUBLIC) && !modifiers.contains(PRIVATE)) {
                kws.add(PUBLIC_KEYWORD);
            }
            if (!modifiers.contains(FINAL) && !modifiers.contains(ABSTRACT)) {
                kws.add(ABSTRACT_KEYWORD);
                kws.add(FINAL_KEYWORD);
            }
            kws.add(CLASS_KEYWORD);
            kws.add(INTERFACE_KEYWORD);
            kws.add(ENUM_KEYWORD);
            for (String kw : kws) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
        }
        
        private void addMemberModifiers(Env env, Set<Modifier> modifiers, boolean isLocal) {
            String prefix = env.getPrefix();
            List<String> kws = new ArrayList<String>();
            if (isLocal) {
                if (!modifiers.contains(FINAL)) {
                    kws.add(FINAL_KEYWORD);
                }
            } else {
                if (!modifiers.contains(PUBLIC) && !modifiers.contains(PROTECTED) && !modifiers.contains(PRIVATE)) {
                    kws.add(PUBLIC_KEYWORD);
                    kws.add(PROTECTED_KEYWORD);
                    kws.add(PRIVATE_KEYWORD);
                }
                if (env.getController().getSourceVersion().compareTo(SourceVersion.RELEASE_8) >= 0
                        && Utilities.getPathElementOfKind(Tree.Kind.INTERFACE, env.getPath()) != null
                        && !modifiers.contains(STATIC) && !modifiers.contains(ABSTRACT) /*TODO: && !modifiers.contains(DEFAULT)*/) {
                        kws.add(DEFAULT_KEYWORD);
                }
                if (!modifiers.contains(FINAL) && !modifiers.contains(ABSTRACT) && !modifiers.contains(VOLATILE)) {
                    kws.add(FINAL_KEYWORD);
                }
                if (!modifiers.contains(FINAL) && !modifiers.contains(ABSTRACT) /*TODO: && !modifiers.contains(DEFAULT)*/
                        && !modifiers.contains(NATIVE) && !modifiers.contains(SYNCHRONIZED)) {
                    kws.add(ABSTRACT_KEYWORD);
                }
                if (!modifiers.contains(STATIC) /*TODO: && !modifiers.contains(DEFAULT)*/) {
                    kws.add(STATIC_KEYWORD);
                }
                if (!modifiers.contains(ABSTRACT) && !modifiers.contains(NATIVE)) {
                    kws.add(NATIVE_KEYWORD);
                }
                if (!modifiers.contains(STRICTFP)) {
                    kws.add(STRICT_KEYWORD);
                }
                if (!modifiers.contains(SYNCHRONIZED) && !modifiers.contains(ABSTRACT)) {
                    kws.add(SYNCHRONIZED_KEYWORD);
                }
                if (!modifiers.contains(TRANSIENT)) {
                    kws.add(TRANSIENT_KEYWORD);
                }
                if (!modifiers.contains(FINAL) && !modifiers.contains(VOLATILE)) {
                    kws.add(VOLATILE_KEYWORD);
                }
                kws.add(VOID_KEYWORD);
                kws.add(CLASS_KEYWORD);
                kws.add(INTERFACE_KEYWORD);
                kws.add(ENUM_KEYWORD);
            }
            for (String kw : kws) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
            for (String kw : PRIM_KEYWORDS) {
                if (Utilities.startsWith(kw, prefix))
                    results.add(JavaCompletionItem.createKeywordItem(kw, SPACE, anchorOffset, false));
            }
        }
        
        private void addElementCreators(Env env) throws IOException {
            CompilationController controller = env.getController();
            controller.toPhase(Phase.ELEMENTS_RESOLVED);
            TreePath clsPath = Utilities.getPathElementOfKind(TreeUtilities.CLASS_TREE_KINDS, env.getPath());
            if (clsPath == null)
                return;
            ClassTree cls = (ClassTree)clsPath.getLeaf();
            CompilationUnitTree root = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            Tree currentMember = null;
            int nextMemberPos = (int)Diagnostic.NOPOS;            
            for (Tree member : cls.getMembers()) {
                int pos = (int)sourcePositions.getStartPosition(root, member);
                if (pos > caretOffset) {
                    nextMemberPos = pos;
                    break;
                }
                pos = (int)sourcePositions.getEndPosition(root, member);
                if (caretOffset < pos) {
                    currentMember = member;
                    nextMemberPos = pos;
                    break;
                }
            }
            if (nextMemberPos > caretOffset) {
                String text = controller.getText().substring(caretOffset, nextMemberPos);
                int idx = text.indexOf('\n'); // NOI18N
                if (idx >= 0)
                    text = text.substring(0, idx);
                if (text.trim().length() > 0)
                    return;
            }
            final Trees trees = controller.getTrees();
            TypeElement te = (TypeElement)trees.getElement(clsPath);            
            if (te == null || !te.getKind().isClass())
                return;
            String prefix = env.getPrefix();
            Types types = controller.getTypes();
            DeclaredType clsType = (DeclaredType)te.asType();            
            for (ExecutableElement ee : GeneratorUtils.findUndefs(controller, te)) {
                if (startsWith(env, ee.getSimpleName().toString())) {
                    TypeMirror tm = types.asMemberOf(clsType, ee);
                    if (tm.getKind() == TypeKind.EXECUTABLE)
                        results.add(JavaCompletionItem.createOverrideMethodItem(env.getController(), ee, (ExecutableType)tm, anchorOffset, true, env.getWhiteList()));
                }
            }            
            for (ExecutableElement ee : GeneratorUtils.findOverridable(controller, te)) {
                if (startsWith(env, ee.getSimpleName().toString())) {
                    TypeMirror tm = types.asMemberOf(clsType, ee);
                    if (tm.getKind() == TypeKind.EXECUTABLE)
                        results.add(JavaCompletionItem.createOverrideMethodItem(env.getController(), ee, (ExecutableType)tm, anchorOffset, false, env.getWhiteList()));
                }
            }
            if (prefix == null || startsWith(env, "get") || startsWith(env, "set") || startsWith(env, "is")
                    || startsWith(env, prefix, "get") || startsWith(env, prefix, "set") || startsWith(env, prefix, "is")) {
                List<? extends Element> members = controller.getElements().getAllMembers(te);
                Map<String, List<ExecutableElement>> methods = new HashMap<String, List<ExecutableElement>>();
                for (ExecutableElement method : ElementFilter.methodsIn(members)) {
                    List<ExecutableElement> l = methods.get(method.getSimpleName().toString());
                    if (l == null) {
                        l = new ArrayList<ExecutableElement>();
                        methods.put(method.getSimpleName().toString(), l);
                    }
                    l.add(method);
                }
                for (VariableElement variableElement : ElementFilter.fieldsIn(members)) {
                    Name name = variableElement.getSimpleName();
                    if (!name.contentEquals(ERROR)) {
                        String nameBase = GeneratorUtils.getCapitalizedName(name).toString();
                        String setterName = "set" + nameBase; //NOI18N
                        String getterName = (variableElement.asType().getKind() == TypeKind.BOOLEAN ? "is" : "get") + nameBase; //NOI18N
                        if ((prefix == null || startsWith(env, getterName)) && !GeneratorUtils.hasGetter(controller, te, variableElement, methods)) {
                            results.add(JavaCompletionItem.createGetterSetterMethodItem(env.getController(), variableElement, asMemberOf(variableElement, clsType, types), anchorOffset, false));
                        }
                        if ((prefix == null || startsWith(env, setterName)) && !(variableElement.getModifiers().contains(Modifier.FINAL) || GeneratorUtils.hasSetter(controller, te, variableElement, methods))) {
                            results.add(JavaCompletionItem.createGetterSetterMethodItem(env.getController(), variableElement, asMemberOf(variableElement, clsType, types), anchorOffset, true));
                        }
                    }
                }
            }
            if (startsWith(env, te.getSimpleName().toString())) {
                final Set<VariableElement> initializedFields = new LinkedHashSet<VariableElement>();
                final Set<VariableElement> uninitializedFields = new LinkedHashSet<VariableElement>();
                final List<ExecutableElement> constructors = new ArrayList<ExecutableElement>();
                if (currentMember != null && currentMember.getKind() == Tree.Kind.VARIABLE) {
                    Element e = trees.getElement(new TreePath(clsPath, currentMember));
                    if (e.getKind().isField())
                        initializedFields.add((VariableElement)e);
                }
                Map<ExecutableElement, boolean[]> ctors2generate = new LinkedHashMap<ExecutableElement, boolean[]>();
                GeneratorUtils.scanForFieldsAndConstructors(controller, clsPath, initializedFields, uninitializedFields, constructors);
                final Set<VariableElement> uninitializedFinalFields = new LinkedHashSet<VariableElement>();
                for (VariableElement ve : uninitializedFields) {
                    if (ve.getModifiers().contains(Modifier.FINAL))
                        uninitializedFinalFields.add(ve);
                }
                int ufSize = uninitializedFields.size();
                int uffSize = uninitializedFinalFields.size();
                if (cls.getKind() != Tree.Kind.ENUM) {
                    DeclaredType superType = (DeclaredType)te.getSuperclass();
                    Scope scope = env.getScope();
                    for (ExecutableElement ctor : ElementFilter.constructorsIn(superType.asElement().getEnclosedElements())) {
                        if (trees.isAccessible(scope, ctor, superType)) {
                            ctors2generate.put(ctor, new boolean[] {true, uffSize > 0 && uffSize < ufSize, ufSize > 0});
                        }
                    }
                } else {
                    ctors2generate.put(null, new boolean[] {true, uffSize > 0 && uffSize < ufSize, ufSize > 0});
                }
                for (ExecutableElement ee : constructors) {
                    if (!controller.getElementUtilities().isSynthetic(ee)) {
                        List<? extends VariableElement> parameters = ee.getParameters();
                        for (Map.Entry<ExecutableElement, boolean[]> entry : ctors2generate.entrySet()) {
                            List<? extends VariableElement> params = entry.getKey() != null ? entry.getKey().getParameters() : Collections.<VariableElement>emptyList();
                            int paramSize = params.size();
                            if (parameters.size() == paramSize) {
                                Iterator<? extends VariableElement> proposed = params.iterator();
                                Iterator<? extends VariableElement> original = parameters.iterator();                        
                                boolean same = true;
                                while (same && proposed.hasNext() && original.hasNext())
                                    same &= controller.getTypes().isSameType(proposed.next().asType(), original.next().asType());
                                if (same)
                                    entry.getValue()[0] = false;
                            } else if (ufSize > 0) {
                                if (uffSize > 0 && uffSize < ufSize && parameters.size() == paramSize + uffSize) {
                                    Iterator<? extends VariableElement> proposed = uninitializedFinalFields.iterator();
                                    Iterator<? extends VariableElement> original = parameters.iterator();
                                    boolean same = true;
                                    while (same && proposed.hasNext() && original.hasNext())
                                        same &= controller.getTypes().isSameType(proposed.next().asType(), original.next().asType());
                                    if (same) {
                                        proposed = params.iterator();
                                        while (same && proposed.hasNext() && original.hasNext())
                                            same &= controller.getTypes().isSameType(proposed.next().asType(), original.next().asType());
                                        if (same)
                                            entry.getValue()[1] = false;
                                    }
                                }
                                if (parameters.size() == paramSize + ufSize) {
                                    Iterator<? extends VariableElement> proposed = uninitializedFields.iterator();
                                    Iterator<? extends VariableElement> original = parameters.iterator();
                                    boolean same = true;
                                    while (same && proposed.hasNext() && original.hasNext())
                                        same &= controller.getTypes().isSameType(proposed.next().asType(), original.next().asType());
                                    if (same) {
                                        proposed = params.iterator();
                                        while (same && proposed.hasNext() && original.hasNext())
                                            same &= controller.getTypes().isSameType(proposed.next().asType(), original.next().asType());
                                        if (same)
                                            entry.getValue()[2] = false;
                                    }
                                }
                            }
                        }
                    }
                }
                for (Map.Entry<ExecutableElement, boolean[]> entry : ctors2generate.entrySet()) {
                    if (entry.getValue()[0])
                        results.add(JavaCompletionItem.createInitializeAllConstructorItem(env.getController(), Collections.<VariableElement>emptySet(), entry.getKey(), te, anchorOffset));
                    if (entry.getValue()[1])
                        results.add(JavaCompletionItem.createInitializeAllConstructorItem(env.getController(), uninitializedFinalFields, entry.getKey(), te, anchorOffset));
                    if (entry.getValue()[2])
                        results.add(JavaCompletionItem.createInitializeAllConstructorItem(env.getController(), uninitializedFields, entry.getKey(), te, anchorOffset));
                }
            }
        }
        
        private TypeElement getTypeElement(Env env, final String simpleName) throws IOException {
            if (simpleName == null || simpleName.length() == 0)
                return null;
            final CompilationController controller = env.getController();
            final TreeUtilities tu = controller.getTreeUtilities();
            final Trees trees = controller.getTrees();
            final Scope scope = env.getScope();
            final TypeElement enclClass = scope.getEnclosingClass();
            final boolean isStatic = enclClass == null ? false :
                (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    return (e.getKind().isClass() || e.getKind().isInterface()) &&
                            e.getSimpleName().contentEquals(simpleName) &&
                            (!isStatic || e.getModifiers().contains(STATIC)) &&
                            trees.isAccessible(scope, e, (DeclaredType)t);
                }
            };
            for(Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor))
                return (TypeElement)e;
            acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    return e.getSimpleName().contentEquals(simpleName) &&
                            trees.isAccessible(scope, (TypeElement)e);
                }
            };
            for (TypeElement e : controller.getElementUtilities().getGlobalTypes(acceptor))
                if (simpleName.contentEquals(e.getSimpleName()))
                    return e;
            return null;
        }
        
        private VariableElement getFieldOrVar(Env env, final String simpleName) throws IOException {
            if (simpleName == null || simpleName.length() == 0)
                return null;
            final CompilationController controller = env.getController();
            final Scope scope = env.getScope();
            final TypeElement enclClass = scope.getEnclosingClass();
            final boolean isStatic = enclClass == null ? false :
                (controller.getTreeUtilities().isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic()));
            final Collection<? extends Element> illegalForwardRefs = env.getForwardReferences();
            final ExecutableElement method = scope.getEnclosingMethod();
            ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                public boolean accept(Element e, TypeMirror t) {
                    if (!e.getSimpleName().contentEquals(simpleName))
                        return false;
                    switch (e.getKind()) {
                        case LOCAL_VARIABLE:
                        case RESOURCE_VARIABLE:
                            if (isStatic && (e.getSimpleName().contentEquals(THIS_KEYWORD) || e.getSimpleName().contentEquals(SUPER_KEYWORD)))
                                return false;
                        case EXCEPTION_PARAMETER:
                        case PARAMETER:
                            return (method == e.getEnclosingElement() || e.getModifiers().contains(FINAL)) &&
                                    !illegalForwardRefs.contains(e);
                        case FIELD:
                            if (e.getSimpleName().contentEquals(THIS_KEYWORD) || e.getSimpleName().contentEquals(SUPER_KEYWORD))
                                return !isStatic;
                        case ENUM_CONSTANT:
                            return !illegalForwardRefs.contains(e);
                    }
                    return false;
                }
            };
            for(Element e : controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor))
                return (VariableElement)e;
            return null;
        }
        
        private boolean isOfSmartType(Env env, TypeMirror type, Set<? extends TypeMirror> smartTypes) {
            if (smartTypes == null || smartTypes.isEmpty())
                return false;
            if (env.isInsideForEachExpression()) {
                if (type.getKind() == TypeKind.ARRAY) {
                    type = ((ArrayType)type).getComponentType();
                } else if (type.getKind() == TypeKind.DECLARED) {
                    Elements elements = env.getController().getElements();
                    Types types = env.getController().getTypes();
                    TypeElement iterableTE = elements.getTypeElement(JAVA_LANG_ITERABLE); //NOI18N
                    DeclaredType iterable = iterableTE != null ? types.getDeclaredType(iterableTE) : null;
                    if (iterable != null && types.isSubtype(type, iterable)) {
                        Iterator<? extends TypeMirror> it = ((DeclaredType)type).getTypeArguments().iterator();
                        type = it.hasNext() ? it.next() : elements.getTypeElement(JAVA_LANG_OBJECT).asType(); //NOI18N
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            for (TypeMirror smartType : smartTypes) {
                if (SourceUtils.checkTypesAssignable(env.getController(), type, smartType))
                    return true;
            }
            return false;
        }
        
        private boolean isTopLevelClass(Tree tree, CompilationUnitTree root) {
            if (TreeUtilities.CLASS_TREE_KINDS.contains(tree.getKind()) || (tree.getKind() == Tree.Kind.EXPRESSION_STATEMENT && ((ExpressionStatementTree)tree).getExpression().getKind() == Tree.Kind.ERRONEOUS)) {
                for (Tree t : root.getTypeDecls())
                    if (tree == t)
                        return true;
            }
            return false;
        }

        private static boolean isAnnonInner(ElementHandle<TypeElement> elem) {
            String name = elem.getQualifiedName();
            int idx = name.lastIndexOf('.'); //NOI18N
            String simpleName = idx > -1 ? name.substring(idx + 1) : name;
            return simpleName.length() == 0 || Character.isDigit(simpleName.charAt(0));
        }

        private static boolean isJavaIdentifierPart(String text) {
            for (int i = 0; i < text.length(); i++) {
                if (!(Character.isJavaIdentifierPart(text.charAt(i))))
                    return false;
            }
            return true;
        }

        private Collection getFilteredData(Collection<JavaCompletionItem> data, String prefix) {
            if (prefix.length() == 0)
                return data;
            List ret = new ArrayList();
            boolean camelCase = isCamelCasePrefix(prefix);
            for (Iterator<JavaCompletionItem> it = data.iterator(); it.hasNext();) {
                CompletionItem itm = it.next();
                if (Utilities.startsWith(itm.getInsertPrefix().toString(), prefix)
                        || (camelCase && Utilities.startsWithCamelCase(itm.getInsertPrefix().toString(), prefix)))
                    ret.add(itm);
            }
            return ret;
        }
        
        private boolean isOfKindAndType(TypeMirror type, Element e, EnumSet<ElementKind> kinds, TypeMirror base, Scope scope, Trees trees, Types types) {
            if (type.getKind() != TypeKind.ERROR && kinds.contains(e.getKind())) {
                if (base == null)
                    return true;
                if (types.isSubtype(type, base))
                    return true;
            }
            if ((e.getKind().isClass() || e.getKind().isInterface()) && 
                (kinds.contains(ANNOTATION_TYPE) || kinds.contains(CLASS) || kinds.contains(ENUM) || kinds.contains(INTERFACE))) {
                DeclaredType dt = (DeclaredType)e.asType();
                for (Element ee : e.getEnclosedElements())
                    if (trees.isAccessible(scope, ee, dt) && isOfKindAndType(ee.asType(), ee, kinds, base, scope, trees, types))
                        return true;
            }
            return false;
        }
        
        private boolean containsAccessibleNonFinalType(Element e, Scope scope, Trees trees) {
            if (e.getKind().isClass() || e.getKind().isInterface()) {
                if (!e.getModifiers().contains(Modifier.FINAL)) {
                    return true;
                }
                DeclaredType dt = (DeclaredType)e.asType();
                for (Element ee : e.getEnclosedElements())
                    if (trees.isAccessible(scope, ee, dt) && containsAccessibleNonFinalType(ee, scope, trees))
                        return true;
            }
            return false;
        }
        
        private Set<? extends TypeMirror> getSmartTypes(Env env) throws IOException {
            int offset = env.getOffset();
            final CompilationController controller = env.getController();
            TreePath path = controller.getTreeUtilities().pathFor(offset);
            Tree lastTree = null;
            int dim = 0;
            while(path != null) {
                Tree tree = path.getLeaf();
                switch(tree.getKind()) {
                    case VARIABLE:
                        TypeMirror type = controller.getTrees().getTypeMirror(new TreePath(path, ((VariableTree)tree).getType()));
                        if (type == null)
                            return null;
                        while(dim-- > 0) {
                            if (type.getKind() == TypeKind.ARRAY)
                                type = ((ArrayType)type).getComponentType();
                            else
                                return null;
                        }
                        return type != null ? Collections.singleton(type) : null;
                    case ASSIGNMENT:
                        type = controller.getTrees().getTypeMirror(new TreePath(path, ((AssignmentTree)tree).getVariable()));
                        if (type == null)
                            return null;
                        TreePath parentPath = path.getParentPath();
                        if (parentPath != null && parentPath.getLeaf().getKind() == Tree.Kind.ANNOTATION && type.getKind() == TypeKind.EXECUTABLE) {
                            type = ((ExecutableType)type).getReturnType();
                            while(dim-- > 0) {
                                if (type.getKind() == TypeKind.ARRAY)
                                    type = ((ArrayType)type).getComponentType();
                                else
                                    return null;
                            }
                            if (type.getKind() == TypeKind.ARRAY)
                                type = ((ArrayType)type).getComponentType();
                        }
                        return type != null ? Collections.singleton(type) : null;
                    case RETURN:
                        TreePath methodOrLambdaPath = Utilities.getPathElementOfKind(EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION), path);
                        if (methodOrLambdaPath == null)
                            return null;
                        if (methodOrLambdaPath.getLeaf().getKind() == Tree.Kind.METHOD) {
                            Tree retTree = ((MethodTree)methodOrLambdaPath.getLeaf()).getReturnType();
                            if (retTree == null)
                                return null;
                            type = controller.getTrees().getTypeMirror(new TreePath(methodOrLambdaPath, retTree));
                            if (type == null && JavaSource.Phase.RESOLVED.compareTo(controller.getPhase()) > 0) {
                                controller.toPhase(Phase.RESOLVED);
                                type = controller.getTrees().getTypeMirror(new TreePath(methodOrLambdaPath, retTree));
                            }
                            return type != null ? Collections.singleton(type) : null;
                        } else {
                            type = controller.getTrees().getTypeMirror(methodOrLambdaPath);
                            if (type != null && type.getKind() == TypeKind.DECLARED) {
                                ExecutableType descType = controller.getTypeUtilities().getDescriptorType((DeclaredType)type);
                                if (descType != null) {
                                    return Collections.singleton(descType.getReturnType());
                                }
                            }
                        }
                        break;
                    case THROW:
                        TreePath methodPath = Utilities.getPathElementOfKind(Tree.Kind.METHOD, path);
                        if (methodPath == null)
                            return null;
                        HashSet<TypeMirror> ret = new HashSet<TypeMirror>();
                        Trees trees = controller.getTrees();
                        for (ExpressionTree thr : ((MethodTree)methodPath.getLeaf()).getThrows()) {
                            type = trees.getTypeMirror(new TreePath(methodPath, thr));
                            if (type == null && JavaSource.Phase.RESOLVED.compareTo(controller.getPhase()) > 0) {
                                controller.toPhase(Phase.RESOLVED);
                                type = trees.getTypeMirror(new TreePath(methodPath, thr));
                            }
                            if (type != null)
                                ret.add(type);
                        }
                        return ret;
                    case TRY:
                        TypeElement te = controller.getElements().getTypeElement("java.lang.AutoCloseable"); //NOI18N
                        return te != null ? Collections.singleton(controller.getTypes().getDeclaredType(te)) : null;
                    case IF:
                        IfTree iff = (IfTree)tree;
                        return iff.getCondition() == lastTree ? Collections.<TypeMirror>singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN)) : null;
                    case WHILE_LOOP:
                        WhileLoopTree wl = (WhileLoopTree)tree;
                        return wl.getCondition() == lastTree ? Collections.<TypeMirror>singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN)) : null;
                    case DO_WHILE_LOOP:
                        DoWhileLoopTree dwl = (DoWhileLoopTree)tree;
                        return dwl.getCondition() == lastTree ? Collections.<TypeMirror>singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN)) : null;
                    case FOR_LOOP:
                        ForLoopTree fl = (ForLoopTree)tree;
                        Tree cond = fl.getCondition();
                        if (lastTree != null) {
                            if (cond instanceof ErroneousTree) {
                                Iterator<? extends Tree> itt =((ErroneousTree)cond).getErrorTrees().iterator();
                                if (itt.hasNext())
                                    cond = itt.next();
                            }
                            return cond == lastTree ? Collections.<TypeMirror>singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN)) : null;
                        }
                        SourcePositions sourcePositions = env.getSourcePositions();
                        CompilationUnitTree root = env.getRoot();
                        if (cond != null && sourcePositions.getEndPosition(root, cond) < offset)
                            return null;
                        Tree lastInit = null;
                        for (Tree init : fl.getInitializer()) {
                            if (sourcePositions.getEndPosition(root, init) >= offset)
                                return null;
                            lastInit = init;
                        }
                        String text = null;
                        if (lastInit == null) {
                            text = controller.getText().substring((int)sourcePositions.getStartPosition(root, fl), offset).trim();
                            int idx = text.indexOf('('); //NOI18N
                            if (idx >= 0)
                                text = text.substring(idx + 1);
                        } else {
                            text = controller.getText().substring((int)sourcePositions.getEndPosition(root, lastInit), offset).trim();
                        }
                        return ";".equals(text) ? Collections.<TypeMirror>singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN)) : null; //NOI18N
                    case ENHANCED_FOR_LOOP:
                        EnhancedForLoopTree efl = (EnhancedForLoopTree)tree;
                        Tree expr = efl.getExpression();
                        if (lastTree != null) {
                            if (expr instanceof ErroneousTree) {
                                Iterator<? extends Tree> itt =((ErroneousTree)expr).getErrorTrees().iterator();
                                if (itt.hasNext())
                                    expr = itt.next();
                            }
                            if(expr != lastTree)
                                return null;
                        } else {
                            sourcePositions = env.getSourcePositions();
                            root = env.getRoot();
                            text = null;
                            if (efl.getVariable() == null || sourcePositions.getEndPosition(root, efl.getVariable()) > offset) {
                                text = controller.getText().substring((int)sourcePositions.getStartPosition(root, efl), offset).trim();
                                int idx = text.indexOf('('); //NOI18N
                                if (idx >= 0)
                                    text = text.substring(idx + 1);
                            } else {
                                text = controller.getText().substring((int)sourcePositions.getEndPosition(root, efl.getVariable()), offset).trim();
                            }
                            if (!":".equals(text))
                                return null;
                        }
                        TypeMirror var = efl.getVariable() != null ? controller.getTrees().getTypeMirror(new TreePath(path, efl.getVariable())) : null;
                        return var != null ? Collections.singleton(var) : null;
                    case SWITCH:
                        SwitchTree sw = (SwitchTree)tree;
                        if (sw.getExpression() != lastTree)
                            return null;
                        ret = new HashSet<TypeMirror>();
                        Types types = controller.getTypes();
                        ret.add(controller.getTypes().getPrimitiveType(TypeKind.INT));
                        te = controller.getElements().getTypeElement("java.lang.Enum"); //NOI18N
                        if (te != null)
                            ret.add(types.getDeclaredType(te));
                        if (controller.getSourceVersion().compareTo(SourceVersion.RELEASE_7) >= 0) {
                            te = controller.getElements().getTypeElement("java.lang.String"); //NOI18N
                            if (te != null)
                                ret.add(types.getDeclaredType(te));
                        }
                        return ret;
                    case METHOD_INVOCATION:
                        MethodInvocationTree mi = (MethodInvocationTree)tree;
                        sourcePositions = env.getSourcePositions();
                        root = env.getRoot();
                        List<Tree> argTypes = getArgumentsUpToPos(env, mi.getArguments(), (int)sourcePositions.getEndPosition(root, mi.getMethodSelect()), lastTree != null ? (int)sourcePositions.getStartPosition(root, lastTree) : offset, true);
                        if (argTypes != null) {
                            TypeMirror[] args = new TypeMirror[argTypes.size()];
                            int j = 0;
                            for (Tree t : argTypes)
                                args[j++] = controller.getTrees().getTypeMirror(new TreePath(path, t));
                            TypeMirror[] targs = null;
                            if (!mi.getTypeArguments().isEmpty()) {
                                targs = new TypeMirror[mi.getTypeArguments().size()];
                                j = 0;
                                for (Tree t : mi.getTypeArguments()) {
                                    TypeMirror ta = controller.getTrees().getTypeMirror(new TreePath(path, t));
                                    if (ta == null)
                                        return null;
                                    targs[j++] = ta;
                                }
                            }
                            Tree mid = mi.getMethodSelect();
                            path = new TreePath(path, mid);
                            TypeMirror typeMirror = controller.getTrees().getTypeMirror(path);
                            final ExecutableType midTM = typeMirror != null && typeMirror.getKind() == TypeKind.EXECUTABLE ? (ExecutableType)typeMirror : null;
                            final ExecutableElement midEl = midTM == null ? null : (ExecutableElement)controller.getTrees().getElement(path);
                            switch (mid.getKind()) {
                                case MEMBER_SELECT: {
                                    String name = ((MemberSelectTree)mid).getIdentifier().toString();
                                    ExpressionTree exp = ((MemberSelectTree)mid).getExpression();
                                    path = new TreePath(path, exp);
                                    final TypeMirror tm = controller.getTrees().getTypeMirror(path);
                                    final Element el = controller.getTrees().getElement(path);
                                    final Trees trs = controller.getTrees();
                                    if (el != null && tm.getKind() == TypeKind.DECLARED) {
                                        final boolean isStatic = el.getKind().isClass() || el.getKind().isInterface() || el.getKind() == TYPE_PARAMETER;
                                        final boolean isSuperCall = el != null && el.getKind().isField() && el.getSimpleName().contentEquals(SUPER_KEYWORD);
                                        final Scope scope = env.getScope();
                                        TypeElement enclClass = scope.getEnclosingClass();
                                        final TypeMirror enclType = enclClass != null ? enclClass.asType() : null;
                                        ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                            public boolean accept(Element e, TypeMirror t) {
                                                return e.getKind() == METHOD && (!isStatic || e.getModifiers().contains(STATIC)) && trs.isAccessible(scope, e, (DeclaredType)(isSuperCall && enclType != null ? enclType : t));
                                            }
                                        };
                                        return getMatchingArgumentTypes(tm, controller.getElementUtilities().getMembers(tm, acceptor), name, args, targs, midEl, midTM, controller.getTypes(), controller.getTypeUtilities());
                                    }
                                    return null;
                                }
                                case IDENTIFIER: {
                                    String name = ((IdentifierTree)mid).getName().toString();
                                    final Scope scope = env.getScope();
                                    final TreeUtilities tu = controller.getTreeUtilities();
                                    final Trees trs = controller.getTrees();
                                    final TypeElement enclClass = scope.getEnclosingClass();
                                    final boolean isStatic = enclClass != null ? (tu.isStaticContext(scope) || (env.getPath().getLeaf().getKind() == Tree.Kind.BLOCK && ((BlockTree)env.getPath().getLeaf()).isStatic())) : false;
                                    if (SUPER_KEYWORD.equals(name) && enclClass != null) {
                                        ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                            public boolean accept(Element e, TypeMirror t) {
                                                return e.getKind() == CONSTRUCTOR && trs.isAccessible(scope, e, (DeclaredType)t);
                                            }
                                        };
                                        TypeMirror superclass = enclClass.getSuperclass();
                                        return getMatchingArgumentTypes(superclass, controller.getElementUtilities().getMembers(superclass, acceptor), INIT, args, targs, midEl, midTM, controller.getTypes(), controller.getTypeUtilities());
                                    }
                                    ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                        public boolean accept(Element e, TypeMirror t) {
                                            return e.getKind() == METHOD && (!isStatic || e.getModifiers().contains(STATIC)) && trs.isAccessible(scope, e, (DeclaredType)t);
                                        }
                                    };
                                    return getMatchingArgumentTypes(enclClass != null ? enclClass.asType() : null, controller.getElementUtilities().getLocalMembersAndVars(scope, acceptor), THIS_KEYWORD.equals(name) ? INIT : name, args, targs, midEl, midTM, controller.getTypes(), controller.getTypeUtilities());
                                    }
                                }
                            }
                        break;
                    case NEW_CLASS:
                        NewClassTree nc = (NewClassTree)tree;
                        sourcePositions = env.getSourcePositions();
                        root = env.getRoot();
                        int idEndPos = (int)sourcePositions.getEndPosition(root, nc.getIdentifier());
                        if (idEndPos < 0)
                            idEndPos = (int)sourcePositions.getStartPosition(root, nc);
                        if (idEndPos < 0 || idEndPos >= offset || controller.getText().substring(idEndPos, offset).indexOf('(') < 0)
                            break;
                        argTypes = getArgumentsUpToPos(env, nc.getArguments(), idEndPos, lastTree != null ? (int)sourcePositions.getStartPosition(root, lastTree) : offset, true);
                        if (argTypes != null) {
                            trees = controller.getTrees();
                            TypeMirror[] args = new TypeMirror[argTypes.size()];
                            int j = 0;
                            for (Tree t : argTypes)
                                args[j++] = trees.getTypeMirror(new TreePath(path, t));
                            TypeMirror[] targs = null;
                            if (!nc.getTypeArguments().isEmpty()) {
                                targs = new TypeMirror[nc.getTypeArguments().size()];
                                j = 0;
                                for (Tree t : nc.getTypeArguments()) {
                                    TypeMirror ta = trees.getTypeMirror(new TreePath(path, t));
                                    if (ta == null)
                                        return null;
                                    targs[j++] = ta;
                                }
                            }
                            Element elem = controller.getTrees().getElement(path);
                            ExecutableElement ncElem = elem != null && elem.getKind() == CONSTRUCTOR ? (ExecutableElement)elem : null;
                            TypeMirror ncTM = ncElem != null ? ncElem.asType() : null;
                            ExecutableType ncType = ncTM != null && ncTM.getKind() == TypeKind.EXECUTABLE ? (ExecutableType)ncTM : null;
                            Tree mid = nc.getIdentifier();
                            path = new TreePath(path, mid);
                            TypeMirror tm = trees.getTypeMirror(path);
                            if (tm != null && tm.getKind() == TypeKind.ERROR && path.getLeaf().getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
                                path = new TreePath(path, ((ParameterizedTypeTree)path.getLeaf()).getType());
                                tm = trees.getTypeMirror(path);
                            }
                            final Element el = controller.getTrees().getElement(path);
                            final Trees trs = controller.getTrees();
                            if (el != null && tm.getKind() == TypeKind.DECLARED) {
                                final Scope scope = env.getScope();
                                final boolean isAnonymous = nc.getClassBody() != null || el.getKind().isInterface() || el.getModifiers().contains(ABSTRACT);
                                ElementUtilities.ElementAcceptor acceptor = new ElementUtilities.ElementAcceptor() {
                                    public boolean accept(Element e, TypeMirror t) {
                                        return e.getKind() == CONSTRUCTOR && (trs.isAccessible(scope, e, (DeclaredType)t) || isAnonymous && e.getModifiers().contains(PROTECTED));
                                    }
                                };
                                return getMatchingArgumentTypes(tm, controller.getElementUtilities().getMembers(tm, acceptor), INIT, args, targs, ncElem, ncType, controller.getTypes(), controller.getTypeUtilities());
                            }
                            return null;
                        }
                        break;
                    case NEW_ARRAY:
                        NewArrayTree nat = (NewArrayTree)tree;
                        Tree arrayType = nat.getType();
                        if (arrayType == null) {
                            dim++;
                            break;
                        }
                        sourcePositions = env.getSourcePositions();
                        root = env.getRoot();
                        int typeEndPos = (int)sourcePositions.getEndPosition(root, arrayType);
                        if (typeEndPos > offset) {
                            break;
                        }
                        text = controller.getText().substring(typeEndPos, offset);
                        if (text.indexOf('{') >= 0) {
                            type = controller.getTrees().getTypeMirror(new TreePath(path, arrayType));
                            while(dim-- > 0) {
                                if (type.getKind() == TypeKind.ARRAY)
                                    type = ((ArrayType)type).getComponentType();
                                else
                                    return null;
                            }
                            return type != null ? Collections.singleton(type) : null;
                        }
                        if (text.trim().endsWith("[")) //NOI18N
                            return Collections.singleton(controller.getTypes().getPrimitiveType(TypeKind.INT));
                        return null;
                    case LAMBDA_EXPRESSION:
                        LambdaExpressionTree let = (LambdaExpressionTree)tree;
                        int pos = (int)env.getSourcePositions().getStartPosition(env.getRoot(), let.getBody());
                        if (offset <= pos && findLastNonWhitespaceToken(env, tree, offset).token().id() != JavaTokenId.ARROW
                                || lastTree != null && lastTree.getKind() == Tree.Kind.BLOCK)
                            break;
                        type = controller.getTrees().getTypeMirror(path);
                        if (type != null && type.getKind() == TypeKind.DECLARED) {
                            ExecutableType descType = controller.getTypeUtilities().getDescriptorType((DeclaredType)type);
                            if (descType != null) {
                                return Collections.singleton(descType.getReturnType());
                            }
                        }
                        break;
                    case CASE:
                        CaseTree ct = (CaseTree)tree;
                        ExpressionTree exp = ct.getExpression();
                        if (exp != null && env.getSourcePositions().getEndPosition(env.getRoot(), exp) >= offset) {
                            parentPath = path.getParentPath();
                            if (parentPath.getLeaf().getKind() == Tree.Kind.SWITCH) {
                                exp = ((SwitchTree)parentPath.getLeaf()).getExpression();
                                type = controller.getTrees().getTypeMirror(new TreePath(parentPath, exp));
                                return type != null ? Collections.singleton(type) : null;
                            }
                        }
                        return null;
                    case ANNOTATION:
                        AnnotationTree ann = (AnnotationTree)tree;
                        pos = (int)env.getSourcePositions().getStartPosition(env.getRoot(), ann.getAnnotationType());
                        if (offset <= pos)
                            break;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), ann.getAnnotationType());
                        if (offset < pos)
                            break;
                        text = controller.getText().substring(pos, offset).trim();
                        if ("(".equals(text) || text.endsWith("{") || text.endsWith(",")) { //NOI18N
                            TypeElement el = (TypeElement)controller.getTrees().getElement(new TreePath(path, ann.getAnnotationType()));
                            if (el != null) {
                                for (Element ee : el.getEnclosedElements()) {
                                    if (ee.getKind() == METHOD && "value".contentEquals(ee.getSimpleName())) {
                                        type = ((ExecutableElement)ee).getReturnType();
                                        while(dim-- > 0) {
                                            if (type.getKind() == TypeKind.ARRAY)
                                                type = ((ArrayType)type).getComponentType();
                                            else
                                                return null;
                                        }
                                        if (type.getKind() == TypeKind.ARRAY)
                                            type = ((ArrayType)type).getComponentType();
                                        return type != null ? Collections.singleton(type) : null;
                                    }
                                }
                            }
                        }
                        return null;
                    case REMAINDER_ASSIGNMENT:
                    case AND_ASSIGNMENT:
                    case XOR_ASSIGNMENT:
                    case OR_ASSIGNMENT:
                    case LEFT_SHIFT_ASSIGNMENT:
                    case RIGHT_SHIFT_ASSIGNMENT:
                    case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                        CompoundAssignmentTree cat = (CompoundAssignmentTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), cat.getVariable());
                        if (offset <= pos)
                            break;
                        ret = new HashSet<TypeMirror>();
                        types = controller.getTypes();
                        ret.add(types.getPrimitiveType(TypeKind.BYTE));
                        ret.add(types.getPrimitiveType(TypeKind.CHAR));
                        ret.add(types.getPrimitiveType(TypeKind.INT));
                        ret.add(types.getPrimitiveType(TypeKind.LONG));
                        ret.add(types.getPrimitiveType(TypeKind.SHORT));
                        return ret;
                    case LEFT_SHIFT:
                    case RIGHT_SHIFT:
                    case UNSIGNED_RIGHT_SHIFT:
                    case AND:                        
                    case OR:
                    case XOR:
                    case REMAINDER:
                        BinaryTree bt = (BinaryTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), bt.getLeftOperand());
                        if (offset <= pos)
                            break;
                    case BITWISE_COMPLEMENT:
                        ret = new HashSet<TypeMirror>();
                        types = controller.getTypes();
                        ret.add(types.getPrimitiveType(TypeKind.BYTE));
                        ret.add(types.getPrimitiveType(TypeKind.CHAR));
                        ret.add(types.getPrimitiveType(TypeKind.INT));
                        ret.add(types.getPrimitiveType(TypeKind.LONG));
                        ret.add(types.getPrimitiveType(TypeKind.SHORT));
                        return ret;
                    case CONDITIONAL_AND:
                    case CONDITIONAL_OR:
                        bt = (BinaryTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), bt.getLeftOperand());
                        if (offset <= pos)
                            break;
                    case LOGICAL_COMPLEMENT:
                        return Collections.singleton(controller.getTypes().getPrimitiveType(TypeKind.BOOLEAN));
                    case PLUS:
                    case EQUAL_TO:
                    case NOT_EQUAL_TO:
                        bt = (BinaryTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), bt.getLeftOperand());
                        if (offset <= pos)
                            break;
                        TypeMirror tm = controller.getTrees().getTypeMirror(new TreePath(path, bt.getLeftOperand()));
                        if (tm == null)
                            return null;
                        if (tm.getKind().isPrimitive()) {
                            ret = new HashSet<TypeMirror>();
                            types = controller.getTypes();
                            ret.add(types.getPrimitiveType(TypeKind.BYTE));
                            ret.add(types.getPrimitiveType(TypeKind.CHAR));
                            ret.add(types.getPrimitiveType(TypeKind.DOUBLE));
                            ret.add(types.getPrimitiveType(TypeKind.FLOAT));
                            ret.add(types.getPrimitiveType(TypeKind.INT));
                            ret.add(types.getPrimitiveType(TypeKind.LONG));
                            ret.add(types.getPrimitiveType(TypeKind.SHORT));
                            return ret;
                        }
                        return Collections.singleton(tm);
                    case PLUS_ASSIGNMENT:
                        cat = (CompoundAssignmentTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), cat.getVariable());
                        if (offset <= pos)
                            break;
                        tm = controller.getTrees().getTypeMirror(new TreePath(path, cat.getVariable()));
                        if (tm == null)
                            return null;
                        if (tm.getKind().isPrimitive()) {
                            ret = new HashSet<TypeMirror>();
                            types = controller.getTypes();
                            ret.add(types.getPrimitiveType(TypeKind.BYTE));
                            ret.add(types.getPrimitiveType(TypeKind.CHAR));
                            ret.add(types.getPrimitiveType(TypeKind.DOUBLE));
                            ret.add(types.getPrimitiveType(TypeKind.FLOAT));
                            ret.add(types.getPrimitiveType(TypeKind.INT));
                            ret.add(types.getPrimitiveType(TypeKind.LONG));
                            ret.add(types.getPrimitiveType(TypeKind.SHORT));
                            return ret;
                        }
                        return Collections.singleton(tm);                        
                    case MULTIPLY_ASSIGNMENT:
                    case DIVIDE_ASSIGNMENT:
                    case MINUS_ASSIGNMENT:
                        cat = (CompoundAssignmentTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), cat.getVariable());
                        if (offset <= pos)
                            break;
                        ret = new HashSet<TypeMirror>();
                        types = controller.getTypes();
                        ret.add(types.getPrimitiveType(TypeKind.BYTE));
                        ret.add(types.getPrimitiveType(TypeKind.CHAR));
                        ret.add(types.getPrimitiveType(TypeKind.DOUBLE));
                        ret.add(types.getPrimitiveType(TypeKind.FLOAT));
                        ret.add(types.getPrimitiveType(TypeKind.INT));
                        ret.add(types.getPrimitiveType(TypeKind.LONG));
                        ret.add(types.getPrimitiveType(TypeKind.SHORT));
                        return ret;
                    case DIVIDE:
                    case GREATER_THAN:
                    case GREATER_THAN_EQUAL:
                    case LESS_THAN:
                    case LESS_THAN_EQUAL:
                    case MINUS:
                    case MULTIPLY:
                        bt = (BinaryTree)tree;
                        pos = (int)env.getSourcePositions().getEndPosition(env.getRoot(), bt.getLeftOperand());
                        if (offset <= pos)
                            break;
                    case PREFIX_INCREMENT:
                    case PREFIX_DECREMENT:
                    case UNARY_PLUS:
                    case UNARY_MINUS:
                        ret = new HashSet<TypeMirror>();
                        types = controller.getTypes();
                        ret.add(types.getPrimitiveType(TypeKind.BYTE));
                        ret.add(types.getPrimitiveType(TypeKind.CHAR));
                        ret.add(types.getPrimitiveType(TypeKind.DOUBLE));
                        ret.add(types.getPrimitiveType(TypeKind.FLOAT));
                        ret.add(types.getPrimitiveType(TypeKind.INT));
                        ret.add(types.getPrimitiveType(TypeKind.LONG));
                        ret.add(types.getPrimitiveType(TypeKind.SHORT));
                        return ret;
                    case EXPRESSION_STATEMENT:
                        exp = ((ExpressionStatementTree)tree).getExpression();
                        if (exp.getKind() == Tree.Kind.PARENTHESIZED) {
                            text = controller.getText().substring((int)env.getSourcePositions().getStartPosition(env.getRoot(), exp), offset).trim();
                            if (text.endsWith(")")) //NOI18N
                                return null;
                        }
                        break;
                }
                lastTree = tree;
                path = path.getParentPath();
            }
            return null;
        }
        
        private TokenSequence<JavaTokenId> findFirstNonWhitespaceToken(Env env, int startPos, int endPos) {
            TokenSequence<JavaTokenId> ts = env.getController().getTokenHierarchy().tokenSequence(JavaTokenId.language());
            ts.move(startPos);
            ts = nextNonWhitespaceToken(ts);
            if (ts == null || ts.offset() >= endPos)
                return null;
            return ts;
        }
        
        private TokenSequence<JavaTokenId> nextNonWhitespaceToken(TokenSequence<JavaTokenId> ts) {
            while(ts.moveNext()) {
                switch (ts.token().id()) {
                    case WHITESPACE:
                    case LINE_COMMENT:
                    case BLOCK_COMMENT:
                    case JAVADOC_COMMENT:
                        break;
                    default:
                        return ts;
                }
            }
            return null;
        }
        
        private TokenSequence<JavaTokenId> findLastNonWhitespaceToken(Env env, Tree tree, int position) {
            int startPos = (int)env.getSourcePositions().getStartPosition(env.getRoot(), tree);
            return findLastNonWhitespaceToken(env, startPos, position);
        }
        
        private TokenSequence<JavaTokenId> findLastNonWhitespaceToken(Env env, int startPos, int endPos) {
            TokenSequence<JavaTokenId> ts = env.getController().getTokenHierarchy().tokenSequence(JavaTokenId.language());
            ts.move(endPos);
            ts = previousNonWhitespaceToken(ts);
            if (ts == null || ts.offset() < startPos)
                return null;
            return ts;
        }
        
        private TokenSequence<JavaTokenId> previousNonWhitespaceToken(TokenSequence<JavaTokenId> ts) {
            while(ts.movePrevious()) {
                switch (ts.token().id()) {
                    case WHITESPACE:
                    case LINE_COMMENT:
                    case BLOCK_COMMENT:
                    case JAVADOC_COMMENT:
                        break;
                    default:
                        return ts;
                }
            }
            return null;
        }
        
        private List<Tree> getArgumentsUpToPos(Env env, Iterable<? extends ExpressionTree> args, int startPos, int position, boolean strict) {
            List<Tree> ret = new ArrayList<Tree>();
            CompilationUnitTree root = env.getRoot();
            SourcePositions sourcePositions = env.getSourcePositions();
            if (args == null)
                return null; //TODO: member reference???
            for (ExpressionTree e : args) {
                int pos = (int)sourcePositions.getEndPosition(root, e);
                if (pos != Diagnostic.NOPOS && (position > pos || !strict && position == pos)) {
                    startPos = pos;
                    ret.add(e);
                } else {
                    break;
                }
            }
            if (startPos < 0)
                return ret;
            if (position >= startPos) {
                TokenSequence<JavaTokenId> last = findLastNonWhitespaceToken(env, startPos, position);
                if (last == null) {
                    if (!strict && !ret.isEmpty()) {
                        ret.remove(ret.size() - 1);
                        return ret;
                    }
                } else if (last.token().id() == JavaTokenId.LPAREN || last.token().id() == JavaTokenId.COMMA) {
                    return ret;
                }
            }
            return null;
        }
        
        private List<Pair<ExecutableElement, ExecutableType>> getMatchingExecutables(TypeMirror type, Iterable<? extends Element> elements, String name, TypeMirror[] argTypes, Types types) {
            List<Pair<ExecutableElement, ExecutableType>> ret = new ArrayList<Pair<ExecutableElement, ExecutableType>>();
            for (Element e : elements) {
                if ((e.getKind() == CONSTRUCTOR || e.getKind() == METHOD) && name.contentEquals(e.getSimpleName())) {
                    List<? extends VariableElement> params = ((ExecutableElement)e).getParameters();
                    int parSize = params.size();
                    boolean varArgs = ((ExecutableElement)e).isVarArgs();
                    if (!varArgs && (parSize < argTypes.length)) {
                        continue;
                    }
                    ExecutableType eType = (ExecutableType)asMemberOf(e, type, types);
                    if (parSize == 0) {
                        ret.add(new Pair(e, eType));
                    } else {
                        Iterator<? extends TypeMirror> parIt = eType.getParameterTypes().iterator();
                        TypeMirror param = null;
                        for (int i = 0; i <= argTypes.length; i++) {
                            if (parIt.hasNext()) {
                                param = parIt.next();
                                if (!parIt.hasNext() && param.getKind() == TypeKind.ARRAY)
                                    param = ((ArrayType)param).getComponentType();
                            } else if (!varArgs) {
                                break;
                            }
                            if (i == argTypes.length) {
                                ret.add(new Pair(e, eType));
                                break;
                            }
                            if (argTypes[i] == null || !types.isAssignable(argTypes[i], param))
                                break;
                        }
                    }
                }
            }
            return ret;
        }
        
        private List<List<String>> getMatchingParams(CompilationInfo info, TypeMirror type, Iterable<? extends Element> elements, String name, TypeMirror[] argTypes, Types types) {
            List<List<String>> ret = new ArrayList<List<String>>();
            for (Element e : elements) {
                if ((e.getKind() == CONSTRUCTOR || e.getKind() == METHOD) && name.contentEquals(e.getSimpleName())) {
                    List<? extends VariableElement> params = ((ExecutableElement)e).getParameters();
                    int parSize = params.size();
                    boolean varArgs = ((ExecutableElement)e).isVarArgs();
                    if (!varArgs && (parSize < argTypes.length)) {
                        continue;
                    }
                    if (parSize == 0) {
                        ret.add(Collections.<String>singletonList(NbBundle.getMessage(JavaCompletionProvider.class, "JCP-no-parameters")));
                    } else {
                        ExecutableType eType = (ExecutableType)asMemberOf(e, type, types);
                        Iterator<? extends TypeMirror> parIt = eType.getParameterTypes().iterator();
                        TypeMirror param = null;
                        for (int i = 0; i <= argTypes.length; i++) {
                            if (parIt.hasNext()) {
                                param = parIt.next();
                                if (!parIt.hasNext() && param.getKind() == TypeKind.ARRAY)
                                    param = ((ArrayType)param).getComponentType();
                            } else if (!varArgs) {
                                break;
                            }
                            if (i == argTypes.length) {
                                List<String> paramStrings = new ArrayList<String>(parSize);
                                Iterator<? extends TypeMirror> tIt = eType.getParameterTypes().iterator();
                                for (Iterator<? extends VariableElement> it = params.iterator(); it.hasNext();) {
                                    VariableElement ve = it.next();
                                    StringBuffer sb = new StringBuffer();
                                    sb.append(Utilities.getTypeName(info, tIt.next(), false));
                                    if (varArgs && !tIt.hasNext())
                                        sb.delete(sb.length() - 2, sb.length()).append("..."); //NOI18N
                                    CharSequence veName = ve.getSimpleName();
                                    if (veName != null && veName.length() > 0) {
                                        sb.append(" "); // NOI18N
                                        sb.append(veName);
                                    }
                                    if (it.hasNext()) {
                                        sb.append(", "); // NOI18N
                                    }
                                    paramStrings.add(sb.toString());
                                }
                                ret.add(paramStrings);
                                break;
                            }
                            if (argTypes[i].getKind() != TypeKind.ERROR && !types.isAssignable(argTypes[i], param))
                                break;
                        }
                    }
                }
            }
            return ret.isEmpty() ? null : ret;
        }
        
        private Set<TypeMirror> getMatchingArgumentTypes(TypeMirror type, Iterable<? extends Element> elements, String name, TypeMirror[] argTypes, TypeMirror[] typeArgTypes, ExecutableElement prototypeSym, ExecutableType prototype, Types types, TypeUtilities tu) {
            Set<TypeMirror> ret = new HashSet<TypeMirror>();
            for (Element e : elements) {
                if ((e.getKind() == CONSTRUCTOR || e.getKind() == METHOD) && name.contentEquals(e.getSimpleName())) {
                    List<? extends VariableElement> params = ((ExecutableElement)e).getParameters();
                    int parSize = params.size();
                    boolean varArgs = ((ExecutableElement)e).isVarArgs();
                    if (!varArgs && (parSize <= argTypes.length))
                        continue;
                    ExecutableType meth = e == prototypeSym && prototype != null ? prototype : (ExecutableType)asMemberOf(e, type, types);
                    Iterator<? extends TypeMirror> parIt = meth.getParameterTypes().iterator();
                    TypeMirror param = null;
                    for (int i = 0; i <= argTypes.length; i++) {
                        if (parIt.hasNext())
                            param = parIt.next();
                        else if (!varArgs)
                            break;
                        if (i == argTypes.length) {
                            if (typeArgTypes != null && param.getKind() == TypeKind.DECLARED && typeArgTypes.length == meth.getTypeVariables().size())
                                param = tu.substitute(param, meth.getTypeVariables(), Arrays.asList(typeArgTypes));
                            TypeMirror toAdd = null;
                            if (i < parSize)
                                toAdd = param;
                            if (varArgs && !parIt.hasNext() && param.getKind() == TypeKind.ARRAY)
                                toAdd = ((ArrayType)param).getComponentType();
                            if (toAdd != null && ret.add(toAdd) && toAdd.getKind() != TypeKind.TYPEVAR) {
                                TypeMirror toRemove = null;
                                for (TypeMirror tm : ret) {
                                    if (tm != toAdd) {
                                        TypeMirror tmErasure = types.erasure(tm);
                                        TypeMirror toAddErasure = types.erasure(toAdd);
                                        if (types.isSubtype(toAddErasure, tmErasure)) {
                                            toRemove = toAdd;
                                            break;
                                        } else if (types.isSubtype(tmErasure, toAddErasure)) {
                                            toRemove = tm;
                                            break;
                                        }
                                    }
                                }
                                if (toRemove != null && !toRemove.getKind().isPrimitive() &&
                                        !"java.lang.String".equals(toRemove.toString()) && !"char[]".equals(toRemove.toString())) //NOI18N
                                    ret.remove(toRemove);
                            }
                            break;
                        }
                        if (argTypes[i] == null)
                            break;
                        if (varArgs && !parIt.hasNext() && param.getKind() == TypeKind.ARRAY) {
                            if (types.isAssignable(argTypes[i], param))
                                varArgs = false;
                            else if (argTypes[i].getKind() != TypeKind.ERROR && !types.isAssignable(argTypes[i], ((ArrayType)param).getComponentType()))
                                break;
                        } else if (argTypes[i].getKind() != TypeKind.ERROR && !types.isAssignable(argTypes[i], param))
                            break;
                    }
                }
            }
            return ret.isEmpty() ? null : ret;
        }
        
        private TypeMirror asMemberOf(Element element, TypeMirror type, Types types) {
            TypeMirror ret = element.asType();
            TypeMirror enclType = element.getEnclosingElement().asType();
            if (enclType.getKind() == TypeKind.DECLARED)
                enclType = types.erasure(enclType);
            while(type != null && type.getKind() == TypeKind.DECLARED) {
                if ((enclType.getKind() != TypeKind.DECLARED || ((DeclaredType)enclType).asElement().getSimpleName().length() > 0) && types.isSubtype(type, enclType)) {
                    ret = types.asMemberOf((DeclaredType)type, element);
                    break;
                }
                type = ((DeclaredType)type).getEnclosingType();
            }
            return ret;
        }       
        
        private Tree unwrapErrTree(Tree tree) {
            if (tree != null && tree.getKind() == Tree.Kind.ERRONEOUS) {
                Iterator<? extends Tree> it = ((ErroneousTree)tree).getErrorTrees().iterator();
                tree = it.hasNext() ? it.next() : null;
            }
            return tree;
        }
        
        private boolean withinScope(Env env, TypeElement e) throws IOException {
            for (Element encl = env.getScope().getEnclosingClass(); encl != null; encl = encl.getEnclosingElement()) {
                if (e == encl)
                    return true;
            }
            return false;
        }

        private boolean withinLabeledStatement(Env env) {
            TreePath path = env.getPath();
            while (path != null) {
                if (path.getLeaf().getKind() == Tree.Kind.LABELED_STATEMENT)
                    return true;
                path = path.getParentPath();
            }
            return false;
        }
        
        private String fullName(Tree tree) {
            switch (tree.getKind()) {
            case IDENTIFIER:
                return ((IdentifierTree)tree).getName().toString();
            case MEMBER_SELECT:
                String sname = fullName(((MemberSelectTree)tree).getExpression());
                return sname == null ? null : sname + '.' + ((MemberSelectTree)tree).getIdentifier();
            default:
                return null;
            }
        }

        private DeclaredType getDeclaredType(TypeElement e, HashMap<? extends Element, ? extends TypeMirror> map, Types types) {
            List<? extends TypeParameterElement> tpes = e.getTypeParameters();
            TypeMirror[] targs = new TypeMirror[tpes.size()];
            int i = 0;
            for (Iterator<? extends TypeParameterElement> it = tpes.iterator(); it.hasNext();) {
                TypeParameterElement tpe = it.next();
                TypeMirror t = map.get(tpe);
                targs[i++] = t != null ? t : tpe.asType();
            }
            Element encl = e.getEnclosingElement();
            if ((encl.getKind().isClass() || encl.getKind().isInterface()) && !((TypeElement)encl).getTypeParameters().isEmpty())
                    return types.getDeclaredType(getDeclaredType((TypeElement)encl, map, types), e, targs);
            return types.getDeclaredType(e, targs);
        }
        
        private Env getCompletionEnvironment(CompilationController controller, int queryType) throws IOException {
            controller.toPhase(Phase.PARSED);
            int offset = controller.getSnapshot().getEmbeddedOffset(caretOffset);
            if (offset < 0)
                return null;
            boolean complQuery = (queryType & COMPLETION_QUERY_TYPE) != 0;
            String prefix = null;
            if (offset > 0) {
                if (complQuery) {
                    TokenSequence<JavaTokenId> ts = controller.getTokenHierarchy().tokenSequence(JavaTokenId.language());
                     // When right at the token end move to previous token; otherwise move to the token that "contains" the offset
                    if (ts.move(offset) == 0 || !ts.moveNext())
                        ts.movePrevious();
                    int len = offset - ts.offset();
                    if (len > 0 && ts.token().length() >= len) {
                        if (ts.token().id() == JavaTokenId.IDENTIFIER ||
                            ts.token().id().primaryCategory().startsWith("keyword") || //NOI18N
                            ts.token().id().primaryCategory().startsWith("string") || //NOI18N
                            ts.token().id().primaryCategory().equals("literal")) //NOI18N
                        { //TODO: Use isKeyword(...) when available
                            prefix = ts.token().text().toString().substring(0, len);
                            offset = ts.offset();
                        } else if ((ts.token().id() == JavaTokenId.DOUBLE_LITERAL
                                || ts.token().id() == JavaTokenId.FLOAT_LITERAL
                                || ts.token().id() == JavaTokenId.FLOAT_LITERAL_INVALID
                                || ts.token().id() == JavaTokenId.LONG_LITERAL)
                                && ts.token().text().charAt(0) == '.') {
                            prefix = ts.token().text().toString().substring(1, len);
                            offset = ts.offset() + 1;
                        }
                    }
                } else if (queryType == DOCUMENTATION_QUERY_TYPE) {
                    TokenSequence<JavaTokenId> ts = controller.getTokenHierarchy().tokenSequence(JavaTokenId.language());
                     // When right at the token start move offset to the position "inside" the token
                    ts.move(offset);
                    if (!ts.moveNext())
                        ts.movePrevious();
                    if (ts.offset() == offset && ts.token().length() > 0 &&
                            (ts.token().id() == JavaTokenId.IDENTIFIER ||
                            ts.token().id().primaryCategory().startsWith("keyword") || //NOI18N
                            ts.token().id().primaryCategory().startsWith("string") || //NOI18N
                            ts.token().id().primaryCategory().equals("literal"))) { //NOI18N
                        offset++;
                    }
                }
            }
            TreePath path = controller.getTreeUtilities().pathFor(offset);
            if (queryType != DOCUMENTATION_QUERY_TYPE) {
                TreePath treePath = path;
                while (treePath != null) {
                    TreePath pPath = treePath.getParentPath();
                    TreePath gpPath = pPath != null ? pPath.getParentPath() : null;
                    Env env = getEnvImpl(controller, path, treePath, pPath, gpPath, offset, prefix, true);
                    if (env != null)
                        return env;
                    treePath = treePath.getParentPath();
                }
            } else {
                if (Phase.RESOLVED.compareTo(controller.getPhase()) > 0) {
                    LinkedList<TreePath> reversePath = new LinkedList<TreePath>();
                    TreePath treePath = path;
                    while (treePath != null) {
                        reversePath.addFirst(treePath);
                        treePath = treePath.getParentPath();
                    }
                    for (TreePath tp : reversePath) {
                        TreePath pPath = tp.getParentPath();
                        TreePath gpPath = pPath != null ? pPath.getParentPath() : null;
                        Env env = getEnvImpl(controller, path, tp, pPath, gpPath, offset, prefix, false);
                        if (env != null)
                            return env;
                    }
                }
            }
            return new Env(offset, prefix, controller, path, controller.getTrees().getSourcePositions(), null);
        }
        
        private Env getEnvImpl(CompilationController controller, TreePath orig, TreePath path, TreePath pPath, TreePath gpPath, int offset, String prefix, boolean upToOffset) throws IOException {
            Tree tree = path != null ? path.getLeaf() : null;
            Tree parent = pPath != null ? pPath.getLeaf() : null;
            Tree grandParent = gpPath != null ? gpPath.getLeaf() : null;
            SourcePositions sourcePositions = controller.getTrees().getSourcePositions();
            CompilationUnitTree root = controller.getCompilationUnit();
            TreeUtilities tu = controller.getTreeUtilities();
            if (upToOffset && TreeUtilities.CLASS_TREE_KINDS.contains(tree.getKind())) {
                controller.toPhase(Utilities.inAnonymousOrLocalClass(path)? Phase.RESOLVED : Phase.ELEMENTS_RESOLVED);
                return new Env(offset, prefix, controller, orig, sourcePositions, null);
            } else if (parent != null && tree.getKind() == Tree.Kind.BLOCK && (parent.getKind() == Tree.Kind.METHOD || TreeUtilities.CLASS_TREE_KINDS.contains(parent.getKind()))) {
                controller.toPhase(Utilities.inAnonymousOrLocalClass(path)? Phase.RESOLVED : Phase.ELEMENTS_RESOLVED);
                int blockPos = (int)sourcePositions.getStartPosition(root, tree);
                String blockText = controller.getText().substring(blockPos, upToOffset ? offset : (int)sourcePositions.getEndPosition(root, tree));
                final SourcePositions[] sp = new SourcePositions[1];
                final StatementTree block = (((BlockTree)tree).isStatic() ? tu.parseStaticBlock(blockText, sp) : tu.parseStatement(blockText, sp));
                if (block == null)
                    return null;
                sourcePositions = new SourcePositionsImpl(block, sourcePositions, sp[0], blockPos, upToOffset ? offset : -1);
                Scope scope = controller.getTrees().getScope(path);
                path = tu.pathFor(new TreePath(pPath, block), offset, sourcePositions);
                if (upToOffset) {
                    Tree last = path.getLeaf();
                    List<? extends StatementTree> stmts = null;
                    switch (path.getLeaf().getKind()) {
                        case BLOCK:
                            stmts = ((BlockTree)path.getLeaf()).getStatements();
                            break;
                        case FOR_LOOP:
                            stmts = ((ForLoopTree)path.getLeaf()).getInitializer();
                            break;
                        case ENHANCED_FOR_LOOP:
                            stmts = Collections.singletonList(((EnhancedForLoopTree)path.getLeaf()).getStatement());
                            break;
                        case METHOD:
                            stmts = ((MethodTree)path.getLeaf()).getParameters();
                            break;
                        case SWITCH:
                            CaseTree lastCase = null;
                            for (CaseTree caseTree : ((SwitchTree)path.getLeaf()).getCases())
                                lastCase = caseTree;
                            if (lastCase != null)
                                stmts = lastCase.getStatements();
                            break;
                        case CASE:
                            stmts = ((CaseTree)path.getLeaf()).getStatements();
                            break;
                    }
                    if (stmts != null) {
                        for (StatementTree st : stmts) {
                            if (sourcePositions.getEndPosition(root, st) <= offset)
                                last = st;
                        }
                    }
                    scope = tu.reattributeTreeTo(block, scope, last);
                } else {
                    tu.reattributeTreeTo(block, scope, block);
                }
                return new Env(offset, prefix, controller, path, sourcePositions, scope);
            } else if (grandParent != null && TreeUtilities.CLASS_TREE_KINDS.contains(grandParent.getKind()) &&
                    parent != null && parent.getKind() == Tree.Kind.VARIABLE && unwrapErrTree(((VariableTree)parent).getInitializer()) == tree) {
                if (tu.isEnum((ClassTree)grandParent)) {
                    controller.toPhase(Phase.RESOLVED);
                    return null;
                }
                controller.toPhase(Utilities.inAnonymousOrLocalClass(path)? Phase.RESOLVED : Phase.ELEMENTS_RESOLVED);
                final int initPos = (int)sourcePositions.getStartPosition(root, tree);
                String initText = controller.getText().substring(initPos, upToOffset ? offset : (int)sourcePositions.getEndPosition(root, tree));
                final SourcePositions[] sp = new SourcePositions[1];
                final ExpressionTree init = tu.parseVariableInitializer(initText, sp);
                final ExpressionStatementTree fake = new ExpressionStatementTree() {
                    public Object accept(TreeVisitor v, Object p) {
                        return v.visitExpressionStatement(this, p);
                    }
                    public ExpressionTree getExpression() {
                        return init;
                    }
                    public Kind getKind() {
                        return Tree.Kind.EXPRESSION_STATEMENT;
                    }
                };
                sourcePositions = new SourcePositionsImpl(fake, sourcePositions, sp[0], initPos, upToOffset ? offset : -1);
                Scope scope = controller.getTrees().getScope(path);
                path = tu.pathFor(new TreePath(pPath, fake), offset, sourcePositions);
                if (upToOffset && sp[0].getEndPosition(root, init) + initPos > offset) {
                    scope = tu.reattributeTreeTo(init, scope, path.getLeaf());
                } else {
                    tu.reattributeTree(init, scope);
                }
                return new Env(offset, prefix, controller, path, sourcePositions, scope);
            } else if (parent != null && TreeUtilities.CLASS_TREE_KINDS.contains(parent.getKind()) && tree.getKind() == Tree.Kind.VARIABLE &&
                    ((VariableTree)tree).getInitializer() != null && orig == path &&
                    sourcePositions.getStartPosition(root, ((VariableTree)tree).getInitializer()) >= 0 &&
                    sourcePositions.getStartPosition(root, ((VariableTree)tree).getInitializer()) <= offset) {
                controller.toPhase(Utilities.inAnonymousOrLocalClass(path)? Phase.RESOLVED : Phase.ELEMENTS_RESOLVED);
                tree = ((VariableTree)tree).getInitializer();
                final int initPos = (int)sourcePositions.getStartPosition(root, tree);
                String initText = controller.getText().substring(initPos, offset);
                final SourcePositions[] sp = new SourcePositions[1];
                final ExpressionTree init = tu.parseVariableInitializer(initText, sp);
                Scope scope = controller.getTrees().getScope(new TreePath(path, tree));
                final ExpressionStatementTree fake = new ExpressionStatementTree() {
                    public Object accept(TreeVisitor v, Object p) {
                        return v.visitExpressionStatement(this, p);
                    }
                    public ExpressionTree getExpression() {
                        return init;
                    }
                    public Kind getKind() {
                        return Tree.Kind.EXPRESSION_STATEMENT;
                    }
                };
                sourcePositions = new SourcePositionsImpl(fake, sourcePositions, sp[0], initPos, offset);
                path = tu.pathFor(new TreePath(path, fake), offset, sourcePositions);
                tu.reattributeTree(init, scope);
                return new Env(offset, prefix, controller, path, sourcePositions, scope);
            } else if (tree.getKind() == Tree.Kind.LAMBDA_EXPRESSION &&
                    ((LambdaExpressionTree)tree).getBody() != null) {
                controller.toPhase(Utilities.inAnonymousOrLocalClass(path)? Phase.RESOLVED : Phase.ELEMENTS_RESOLVED);
                tree = ((LambdaExpressionTree)tree).getBody();
                Scope scope = controller.getTrees().getScope(new TreePath(path, tree));
                final int bodyPos = (int)sourcePositions.getStartPosition(root, tree);
                if (bodyPos >= offset) {
                    TokenSequence<JavaTokenId> ts = controller.getTokenHierarchy().tokenSequence(JavaTokenId.language());
                    ts.move(offset);
                    while(ts.movePrevious()) {
                        switch (ts.token().id()) {
                            case WHITESPACE:
                            case LINE_COMMENT:
                            case BLOCK_COMMENT:
                            case JAVADOC_COMMENT:
                                break;
                            case ARROW:
                                return new Env(offset, prefix, controller, path, sourcePositions, scope);
                            default:
                                return null;
                        }
                    }
                }
                String bodyText = controller.getText().substring(bodyPos, offset);
                final SourcePositions[] sp = new SourcePositions[1];
                final Tree body = bodyText.charAt(0) == '{' ? tu.parseStatement(bodyText, sp) : tu.parseExpression(bodyText, sp);
                final Tree fake = body instanceof ExpressionTree ? new ExpressionStatementTree() {
                    public Object accept(TreeVisitor v, Object p) {
                        return v.visitExpressionStatement(this, p);
                    }
                    public ExpressionTree getExpression() {
                        return (ExpressionTree) body;
                    }
                    public Kind getKind() {
                        return Tree.Kind.EXPRESSION_STATEMENT;
                    }
                } : body;
                sourcePositions = new SourcePositionsImpl(fake, sourcePositions, sp[0], bodyPos, offset);
                path = tu.pathFor(new TreePath(path, fake), offset, sourcePositions);
                tu.reattributeTree(body, scope);
                return new Env(offset, prefix, controller, path, sourcePositions, scope);
            }
            return null;
        }
        
        private boolean startsWith(Env env, String theString) {
            String prefix = env.getPrefix();
            return startsWith(env, theString, prefix);
        }
        
        private boolean startsWith(Env env, String theString, String prefix) {
            return env.isCamelCasePrefix() ? Utilities.isCaseSensitive() ? 
                Utilities.startsWithCamelCase(theString, prefix) : 
                Utilities.startsWithCamelCase(theString, prefix) || Utilities.startsWith(theString, prefix) :
                Utilities.startsWith(theString, prefix);
        }
        
        private boolean withinBounds(Env env, TypeMirror type, List<? extends TypeMirror> bounds) {
            if (bounds != null) {
                Types types = env.getController().getTypes();
                for (TypeMirror bound : bounds) {
                    if (!types.isSubtype(type, bound))
                        return false;
                }
            }
            return true;
        }
        
        private class SourcePositionsImpl extends TreeScanner<Void, Tree> implements SourcePositions {
            
            private Tree root;
            private SourcePositions original;
            private SourcePositions modified;
            private int startOffset;
            private int endOffset;
            
            private boolean found;
            
            private SourcePositionsImpl(Tree root, SourcePositions original, SourcePositions modified, int startOffset, int endOffset) {
                this.root = root;
                this.original = original;
                this.modified = modified;
                this.startOffset = startOffset;
                this.endOffset = endOffset;
            }
            
            public long getStartPosition(CompilationUnitTree compilationUnitTree, Tree tree) {
                if (tree == root)
                    return startOffset;
                found = false;
                scan(root, tree);
                return found ? modified.getStartPosition(compilationUnitTree, tree) + startOffset : original.getStartPosition(compilationUnitTree, tree);
            }

            public long getEndPosition(CompilationUnitTree compilationUnitTree, Tree tree) {
                if (tree == root)
                    return endOffset;
                found = false;
                scan(root, tree);
                return found ? modified.getEndPosition(compilationUnitTree, tree) + startOffset : original.getEndPosition(compilationUnitTree, tree);
            }

            @Override
            public Void scan(Tree node, Tree p) {
                if (node == p)
                    found = true;
                else
                    super.scan(node, p);
                return null;
            }
        }
                
        private static boolean isCamelCasePrefix(String prefix) {
            if (prefix == null || prefix.length() < 2 || prefix.charAt(0) == '"')
                return false;
            for (int i = 1; i < prefix.length(); i++) {
                if (Character.isUpperCase(prefix.charAt(i)))
                        return true;                
            }
            return false;
        }

        private class Task extends UserTask {

            @Override
            public void run(ResultIterator resultIterator) throws Exception {
                Result result = resultIterator.getParserResult(caretOffset);
                CompilationController controller = result != null ? CompilationController.get(result) : null;
                if (controller != null)
                    JavaCompletionQuery.this.run(controller);
            }
        }
        
        private class Env {
            private int offset;
            private String prefix;
            private boolean isCamelCasePrefix;
            private CompilationController controller;
            private TreePath path;
            private SourcePositions sourcePositions;
            private Scope scope;
            private ReferencesCount referencesCount;
            private Collection<? extends Element> refs = null;
            private boolean afterExtends = false;
            private boolean insideNew = false;
            private boolean insideForEachExpression = false;
            private boolean insideClass = false;
            private Set<? extends TypeMirror> smartTypes = null;
            private Set<Element> excludes = null;
            private boolean checkAccessibility;
            private boolean addSemicolon = false;
            private boolean checkAddSemicolon = true;
            private int assignToVarPos = -2;
            private WhiteListQuery.WhiteList[] whiteList;
            
            private Env(int offset, String prefix, CompilationController controller, TreePath path, SourcePositions sourcePositions, Scope scope) {
                this.offset = offset;
                this.prefix = prefix;
                this.isCamelCasePrefix = JavaCompletionQuery.isCamelCasePrefix(prefix);
                this.controller = controller;
                this.path = path;
                this.sourcePositions = sourcePositions;
                this.scope = scope;
                Object prop = component != null ? component.getDocument().getProperty(SKIP_ACCESSIBILITY_CHECK) : null;
                this.checkAccessibility = !(prop instanceof String && Boolean.parseBoolean((String)prop));
            }

            public WhiteListQuery.WhiteList getWhiteList() {
                if (whiteList == null) {
                    final FileObject file = controller.getFileObject();
                    whiteList = new WhiteListQuery.WhiteList[]{file == null ? null : WhiteListQuery.getWhiteList(file)};
                }
                return whiteList[0];
            }

            public int getOffset() {
                return offset;
            }
            
            public String getPrefix() {
                return prefix;
            }
            
            public boolean isCamelCasePrefix() {
                return isCamelCasePrefix;
            }
            
            public CompilationController getController() {
                return controller;
            }
            
            public CompilationUnitTree getRoot() {
                return path.getCompilationUnit();
            }
            
            public TreePath getPath() {
                return path;
            }
            
            public SourcePositions getSourcePositions() {
                return sourcePositions;
            }
            
            public Scope getScope() throws IOException {
                if (scope == null) {
                    controller.toPhase(Phase.ELEMENTS_RESOLVED);
                    scope = controller.getTreeUtilities().scopeFor(offset);
                }
                return scope;
            }

            public ReferencesCount getReferencesCount() {
                if (referencesCount == null) {
                    referencesCount = ReferencesCount.get(controller.getClasspathInfo());
                }
                return referencesCount;
            }

            public Collection<? extends Element> getForwardReferences() {
                if (refs == null)
                    refs = Utilities.getForwardReferences(path, offset, sourcePositions, controller.getTrees());
                return refs;
            }

            public boolean isAfterExtends() {
                return afterExtends;
            }

            public void afterExtends() {
                this.afterExtends = true;
            }
        
            public void insideForEachExpression() {
                this.insideForEachExpression = true;
            }

            public boolean isInsideForEachExpression() {
                return insideForEachExpression;
            }

            public void insideNew() {
                this.insideNew = true;
            }

            public boolean isInsideNew() {
                return insideNew;
            }

            public void insideClass(boolean insideClass) {
                this.insideClass = insideClass;
            }

            public boolean isInsideClass() {
                return insideClass;
            }

            public Set<? extends TypeMirror> getSmartTypes() throws IOException {
                if (smartTypes == null) {
                    controller.toPhase(Phase.RESOLVED);
                    smartTypes = JavaCompletionQuery.this.getSmartTypes(this);
                    if(smartTypes != null) {
                        Iterator<? extends TypeMirror> it = smartTypes.iterator();
                        TypeMirror err = null;
                        if (it.hasNext()) {
                            err = it.next();
                            if (it.hasNext() || err.getKind() != TypeKind.ERROR) {
                                err = null;
                            }
                        }
                        if (err != null) {
                            HashSet<TypeMirror> st = new HashSet<TypeMirror>();
                            Types types = controller.getTypes();
                            TypeElement te = (TypeElement) ((ErrorType)err).asElement();
                            if (te.getQualifiedName() == te.getSimpleName()) {
                                ClassIndex ci = controller.getClasspathInfo().getClassIndex();
                                for (ElementHandle<TypeElement> eh : ci.getDeclaredTypes(te.getSimpleName().toString(), ClassIndex.NameKind.SIMPLE_NAME, EnumSet.allOf(ClassIndex.SearchScope.class))) {
                                    te = eh.resolve(controller);
                                    if (te != null) {
                                        st.add(types.erasure(te.asType()));
                                    }
                                }
                            }
                            smartTypes = st;
                        }
                    }
                }
                return smartTypes;
            }

            public void addToExcludes(Element toExclude) {
                if (toExclude != null) {
                    if (excludes == null)
                        excludes = new HashSet<Element>();
                    excludes.add(toExclude);
                }
            }

            public Set<? extends Element> getExcludes() {
                return excludes;
            }

            public boolean isAccessible(Scope scope, Element member, TypeMirror type, boolean selectSuper) {
                if (!checkAccessibility)
                    return true;
                if (type.getKind() != TypeKind.DECLARED)
                    return member.getModifiers().contains(PUBLIC);
                if (getController().getTrees().isAccessible(scope, member, (DeclaredType)type))
                    return true;
                return selectSuper
                        && member.getModifiers().contains(PROTECTED) && !member.getModifiers().contains(STATIC)
                        && !member.getKind().isClass() && !member.getKind().isInterface()
                        && getController().getTrees().isAccessible(scope, (TypeElement)((DeclaredType)type).asElement())
                        && (member.getKind() != METHOD
                        || getController().getElementUtilities().getImplementationOf((ExecutableElement)member, (TypeElement)((DeclaredType)type).asElement()) == member);
            }
            
            public boolean addSemicolon() {
                if (checkAddSemicolon) {
                    TreePath tp = getPath();
                    Tree tree = tp.getLeaf();
                    if (tree.getKind() == Tree.Kind.IDENTIFIER || tree.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
                        tp = tp.getParentPath();
                        if (tp.getLeaf().getKind() == Tree.Kind.VARIABLE && ((VariableTree)tp.getLeaf()).getType() == tree)
                            addSemicolon = true;
                    }
                    if (tp.getLeaf().getKind() == Tree.Kind.MEMBER_SELECT ||
                        (tp.getLeaf().getKind() == Tree.Kind.METHOD_INVOCATION && ((MethodInvocationTree)tp.getLeaf()).getMethodSelect() == tree) ||
                        tp.getLeaf().getKind() == Tree.Kind.VARIABLE)
                        tp = tp.getParentPath();
                    if (tp.getLeaf().getKind() == Tree.Kind.EXPRESSION_STATEMENT
                            && tp.getParentPath().getLeaf().getKind() != Tree.Kind.LAMBDA_EXPRESSION
                            || tp.getLeaf().getKind() == Tree.Kind.BLOCK
                            || tp.getLeaf().getKind() == Tree.Kind.RETURN)
                        addSemicolon = true;
                    checkAddSemicolon = false;
                }
                return addSemicolon;
            }
            
            public int assignToVarPos() {
                if (assignToVarPos < -1) {
                    TreePath tp = getPath();
                    Tree tree = tp.getLeaf();
                    if (tp.getLeaf().getKind() == Tree.Kind.MEMBER_SELECT ||
                        (tp.getLeaf().getKind() == Tree.Kind.METHOD_INVOCATION && ((MethodInvocationTree)tp.getLeaf()).getMethodSelect() == tree))
                        tp = tp.getParentPath();
                    if (tp.getLeaf().getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
                        assignToVarPos = getController().getSnapshot().getOriginalOffset((int) getSourcePositions().getStartPosition(getRoot(), tree));
                    } else if (tp.getLeaf().getKind() == Tree.Kind.BLOCK) {
                        assignToVarPos = getController().getSnapshot().getOriginalOffset(offset);
                    } else {
                        assignToVarPos = -1;
                    }
                }
                return assignToVarPos;
            }
        }
        
        private static class Pair<A, B> {

            private A a;
            private B b;

            private Pair(A a, B b) {
                this.a = a;
                this.b = b;
            }
        }
    }
}
