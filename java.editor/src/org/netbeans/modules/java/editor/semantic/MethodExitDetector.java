/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.java.editor.semantic;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.swing.text.Document;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.modules.editor.highlights.spi.Highlight;

/**
 *
 * @author Jan Lahoda
 */
public class MethodExitDetector extends CancellableTreePathScanner<Boolean, Stack<Tree>> {
    
    /** Creates a new instance of MethodExitDetector */
    public MethodExitDetector() {
    }
    
    private CompilationInfo info;
    private Document doc;
    private Set<Highlight> highlights;
    private Collection<TypeMirror> exceptions;
    private Stack<Map<TypeMirror, List<Highlight>>> exceptions2HighlightsStack;
    
    public Set<Highlight> process(CompilationInfo info, Document document, MethodTree methoddecl, Collection<Tree> excs) {
        this.info = info;
        this.doc  = document;
        this.highlights = new HashSet<Highlight>();
        this.exceptions2HighlightsStack = new Stack<Map<TypeMirror, List<Highlight>>>();
        this.exceptions2HighlightsStack.push(null);
        
        try {
            Set<Highlight> result = new HashSet<Highlight>();
            
            CompilationUnitTree cu = info.getCompilationUnit();
            
            Boolean wasReturn = scan(TreePath.getPath(cu, methoddecl), null);
            
            if (isCanceled())
                return Collections.emptySet();
            
            if (excs == null) {
                //"return" exit point only if not searching for exceptions:
                result.addAll(highlights);
                
                if (wasReturn != Boolean.TRUE) {
                    int lastBracket = Utilities.findLastBracket(methoddecl, cu, info.getTrees().getSourcePositions(), document);
                    
                    if (lastBracket != (-1)) {
                        //highlight the "fall over" exitpoint:
                        result.add(Utilities.createHighlight(cu, info.getTrees().getSourcePositions(), document, lastBracket, lastBracket + 1, EnumSet.of(ColoringAttributes.MARK_OCCURRENCES),MarkOccurrencesHighlighter.ES_COLOR));
                    }
                }
            }
            
            List<TypeMirror> exceptions = null;
            
            if (excs != null) {
                exceptions = new ArrayList<TypeMirror>();
                
                for (Tree t : excs) {
                    if (isCanceled())
                        return Collections.emptySet();
                    
                    TypeMirror m = info.getTrees().getTypeMirror(TreePath.getPath(cu, t));
                    
                    if (m != null) {
                        exceptions.add(m);
                    }
                }
            }
            
            Types t = info.getTypes();
            
            assert exceptions2HighlightsStack.size() == 1 : exceptions2HighlightsStack.size();
            
            Map<TypeMirror, List<Highlight>> exceptions2Highlights = exceptions2HighlightsStack.peek();
            
            //exceptions2Highlights may be null if the method is empty (or not finished, like "public void")
            //see ExitPointsEmptyMethod and ExitPointsStartedMethod tests:
            if (exceptions2Highlights != null) {
                for (TypeMirror type1 : exceptions2Highlights.keySet()) {
                    if (isCanceled())
                        return Collections.emptySet();
                    
                    boolean add = true;
                    
                    if (exceptions != null) {
                        add = false;
                        
                        for (TypeMirror type2 : exceptions) {
                            add |= t.isAssignable(type1, type2);
                        }
                    }
                    
                    if (add) {
                        result.addAll(exceptions2Highlights.get(type1));
                    }
                }
            }
            
            return result;
        } finally {
            //clean-up:
            this.info = null;
            this.doc  = null;
            this.highlights = null;
            this.exceptions2HighlightsStack = null;
        }
    }
    
    private void addToExceptionsMap(TypeMirror key, Highlight value) {
        if (value == null)
            return ;
        
        Map<TypeMirror, List<Highlight>> map = exceptions2HighlightsStack.peek();
        
        if (map == null) {
            map = new HashMap<TypeMirror, List<Highlight>>();
            exceptions2HighlightsStack.pop();
            exceptions2HighlightsStack.push(map);
        }
        
        List<Highlight> l = map.get(key);
        
        if (l == null) {
            map.put(key, l = new ArrayList<Highlight>());
        }
        
        l.add(value);
    }
    
    private void doPopup() {
        Map<TypeMirror, List<Highlight>> top = exceptions2HighlightsStack.pop();
        
        if (top == null)
            return ;
        
        Map<TypeMirror, List<Highlight>> result = exceptions2HighlightsStack.pop();
        
        if (result == null) {
            exceptions2HighlightsStack.push(top);
            return ;
        }
        
        for (TypeMirror key : top.keySet()) {
            List<Highlight> topKey    = top.get(key);
            List<Highlight> resultKey = result.get(key);
            
            if (topKey == null)
                continue;
            
            if (resultKey == null) {
                result.put(key, topKey);
                continue;
            }
            
            resultKey.addAll(topKey);
        }
        
        exceptions2HighlightsStack.push(result);
    }
    
    private Highlight createHighlight(TreePath tree) {
        return Utilities.createHighlight(info.getCompilationUnit(), info.getTrees().getSourcePositions(), doc, tree, EnumSet.of(ColoringAttributes.MARK_OCCURRENCES),MarkOccurrencesHighlighter.ES_COLOR);
    }
    
    @Override
    public Boolean visitTry(TryTree tree, Stack<Tree> d) {
        exceptions2HighlightsStack.push(null);
        
        Boolean returnInTryBlock = scan(tree.getBlock(), d);
        
        boolean returnInCatchBlock = true;
        
        for (Tree t : tree.getCatches()) {
            Boolean b = scan(t, d);
            
            returnInCatchBlock &= b == Boolean.TRUE;
        }
        
        Boolean returnInFinallyBlock = scan(tree.getFinallyBlock(), d);
        
        doPopup();
        
        if (returnInTryBlock == Boolean.TRUE && returnInCatchBlock)
            return Boolean.TRUE;
        
        return returnInFinallyBlock;
    }
    
    @Override
    public Boolean visitReturn(ReturnTree tree, Stack<Tree> d) {
        if (exceptions == null) {
            Highlight h = createHighlight(getCurrentPath());
            
            if (h != null) {
                highlights.add(h);
            }
        }
        
        super.visitReturn(tree, d);
        return Boolean.TRUE;
    }
    
    @Override
    public Boolean visitCatch(CatchTree tree, Stack<Tree> d) {
        TypeMirror type1 = info.getTrees().getTypeMirror(new TreePath(new TreePath(getCurrentPath(), tree.getParameter()), tree.getParameter().getType()));
        Types t = info.getTypes();
        
        if (type1 != null) {
            Set<TypeMirror> toRemove = new HashSet<TypeMirror>();
            Map<TypeMirror, List<Highlight>> exceptions2Highlights = exceptions2HighlightsStack.peek();
            
            if (exceptions2Highlights != null) {
                for (TypeMirror type2 : exceptions2Highlights.keySet()) {
                    if (t.isAssignable(type2, type1)) {
                        toRemove.add(type2);
                    }
                }
                
                for (TypeMirror type : toRemove) {
                    exceptions2Highlights.remove(type);
                }
            }
            
        }
        
        scan(tree.getParameter(), d);
        return scan(tree.getBlock(), d);
    }
    
    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree tree, Stack<Tree> d) {
        Element el = info.getTrees().getElement(new TreePath(getCurrentPath(), tree.getMethodSelect()));
        
        if (el == null) {
            System.err.println("Warning: decl == null");
            System.err.println("tree=" + tree);
        }
        
        if (el != null && el.getKind() == ElementKind.METHOD) {
            for (TypeMirror m : ((ExecutableElement) el).getThrownTypes()) {
                addToExceptionsMap(m, createHighlight(getCurrentPath()));
            }
        }
        
        super.visitMethodInvocation(tree, d);
        return null;
    }
    
    @Override
    public Boolean visitThrow(ThrowTree tree, Stack<Tree> d) {
        addToExceptionsMap(info.getTrees().getTypeMirror(new TreePath(getCurrentPath(), tree.getExpression())), createHighlight(getCurrentPath()));
        
        super.visitThrow(tree, d);
        
        return Boolean.TRUE;
    }
    
    @Override
    public Boolean visitNewClass(NewClassTree tree, Stack<Tree> d) {
        Element el = info.getTrees().getElement(getCurrentPath());
        
        if (el != null && el.getKind() == ElementKind.CONSTRUCTOR) {
            for (TypeMirror m : ((ExecutableElement) el).getThrownTypes()) {
                addToExceptionsMap(m, createHighlight(getCurrentPath()));
            }
        }
        
        super.visitNewClass(tree, d);
        return null;
    }
    
    @Override
    public Boolean visitMethod(MethodTree node, Stack<Tree> p) {
        scan(node.getModifiers(), p);
        scan(node.getReturnType(), p);
        scan(node.getTypeParameters(), p);
        scan(node.getParameters(), p);
        scan(node.getThrows(), p);
        return scan(node.getBody(), p);
    }
    
    @Override
    public Boolean visitIf(IfTree node, Stack<Tree> p) {
        scan(node.getCondition(), p);
        Boolean thenResult = scan(node.getThenStatement(), p);
        Boolean elseResult = scan(node.getElseStatement(), p);
        
        if (thenResult == Boolean.TRUE && elseResult == Boolean.TRUE)
            return Boolean.TRUE;
        
        return null;
    }
    
}
