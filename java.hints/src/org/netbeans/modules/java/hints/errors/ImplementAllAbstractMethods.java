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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.java.hints.errors;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.swing.text.BadLocationException;
import javax.tools.Diagnostic;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.editor.GuardedException;
import org.netbeans.modules.java.editor.codegen.ImplementOverrideMethodGenerator;
import org.netbeans.modules.java.hints.spi.ErrorRule;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public final class ImplementAllAbstractMethods implements ErrorRule<Object>, OverrideErrorMessage<Object> {

    private static final String PREMATURE_EOF_CODE = "compiler.err.premature.eof"; // NOI18N
    
    /** Creates a new instance of ImplementAllAbstractMethodsCreator */
    public ImplementAllAbstractMethods() {
    }

    public Set<String> getCodes() {
        return new HashSet<String>(Arrays.asList(
                "compiler.err.abstract.cant.be.instantiated", // NOI18N
                "compiler.err.does.not.override.abstract", // NOI18N
                "compiler.err.abstract.cant.be.instantiated", // NOI18N
                "compiler.err.enum.constant.does.not.override.abstract")); // NOI18N
    }

    @NbBundle.Messages({
        "ERR_CannotOverrideAbstractMethods=Inherited abstract methods are not accessible and could not be implemented"
    })
    @Override
    public String createMessage(CompilationInfo info, String diagnosticKey, int offset, TreePath treePath, Data<Object> data) {
        TreePath path = deepTreePath(info, offset);
        Element e = info.getTrees().getElement(path);
        if (e == null || !e.getKind().isClass()) {
            TypeMirror tm = info.getTrees().getTypeMirror(path);
            if (tm == null || tm.getKind() != TypeKind.DECLARED) {
                if (path.getLeaf().getKind() == Tree.Kind.NEW_CLASS) {
                    tm = info.getTrees().getTypeMirror(new TreePath(path, ((NewClassTree)path.getLeaf()).getIdentifier()));
                }
            }
            if (tm != null && tm.getKind() == TypeKind.DECLARED) {
                e = ((DeclaredType)tm).asElement();
            } else {
                return null;
            }
        }
        if (e == null) {
            return null;
        }
        Map<Tree, Object> d = (Map)data.getData();
        if (d == null) {
            data.setData(d = new HashMap<>());
        }
        List<? extends ExecutableElement> lee = info.getElementUtilities().findUnimplementedMethods((TypeElement)e, true);
        Scope s = info.getTrees().getScope(path);
        boolean hasDefault = false;
        for (ExecutableElement ee : lee) {
            if (!info.getTrees().isAccessible(s, ee, (DeclaredType)e.asType())) {
                // mark the diagnostic as processed; run() will not bother with analysis of the issue.
                d.put(path.getLeaf(), true);
                return Bundle.ERR_CannotOverrideAbstractMethods();
                
            }
            if (ee.getModifiers().contains(Modifier.DEFAULT)) {
                hasDefault = true;
            }
        }
        if (hasDefault) {
            d.put(path.getLeaf(), e);
        }
        return null;
    }
    
    public List<Fix> run(final CompilationInfo info, String diagnosticKey, final int offset, TreePath treePath, Data<Object> data) {
        TreePath path = deepTreePath(info, offset);
        if (path == null) {
            return null;
        }

        Map<Tree, Object> holder = data == null ? null : (Map)data.getData();
        Object saved = null;
        if (holder != null) {
            saved = holder.get(path.getLeaf());
        }
        if (Boolean.TRUE == saved) {
            return null;
        }
        Element e = info.getTrees().getElement(path);
        boolean isUsableElement = e != null && (e.getKind().isClass() || e.getKind().isInterface());
        final Tree leaf = path.getLeaf();
        
        List<Fix> fixes = new ArrayList<>();
        if (TreeUtilities.CLASS_TREE_KINDS.contains(leaf.getKind())) {
            CompilationUnitTree cut = info.getCompilationUnit();
            // do not offer for class declarations without body
            long start = info.getTrees().getSourcePositions().getStartPosition(cut, leaf);
            long end = info.getTrees().getSourcePositions().getEndPosition(cut, leaf);
            for (Diagnostic d : info.getDiagnostics()) {
                long position = d.getPosition();
                if (d.getCode().equals(PREMATURE_EOF_CODE) && position > start && position < end) {
                    return null;
                }
            }
        }
        
        if (!isUsableElement) {
            //if the parent of path.getLeaf is an error, the situation probably is like:
            //new Runnable {}
            //(missing '()' for constructor)
            //do not propose the hint in this case:
            final boolean[] parentError = new boolean[] {false};
            new TreePathScanner() {
                @Override
                public Object visitNewClass(NewClassTree nct, Object o) {
                    if (leaf == nct) {
                        parentError[0] = getCurrentPath().getParentPath().getLeaf().getKind() == Kind.ERRONEOUS;
                    }
                    return super.visitNewClass(nct, o);
                }
            }.scan(path.getParentPath(), null);
            if (parentError[0]) {
                // ignore
                return null;
            }
        }
        
        TypeElement tel = (saved instanceof TypeElement) ? (TypeElement)saved : null;
        
        if (e == null) {
            if (leaf.getKind() == Kind.NEW_CLASS) {
                fixes.add(new ImplementAbstractMethodsFix(info, path, tel));
            }
        }
        
        X: if (isUsableElement) {
            for (ExecutableElement ee : ElementFilter.methodsIn(e.getEnclosedElements())) {
                if (ee.getModifiers().contains(Modifier.ABSTRACT)) {
                    // make class abstract. In case of enums, suggest to implement the
                    // abstract methods on all enum values.
                    if (e.getKind() == ElementKind.ENUM) {
                        // cannot make enum abstract, but can generate abstract methods skeleton
                        // to all enum members
                        fixes.add(new ImplementOnEnumValues2(info,  e));
                        // avoid other possible fixes:
                        break X;
                    }
                }
            }
            // offer to fix all abstract methods
            
            fixes.add(new ImplementAbstractMethodsFix(info, path, tel));
            if (e.getKind() == ElementKind.CLASS && e.getSimpleName() != null && !e.getSimpleName().contentEquals("")) {
                fixes.add(new MakeAbstractFix(info, path, e.getSimpleName().toString()).toEditorFix());
            }
        } else if (e != null && e.getKind() == ElementKind.ENUM_CONSTANT) {
            fixes.add(new ImplementAbstractMethodsFix(info, path, tel));
        }
        return fixes;
    }
    
    public void cancel() {
        //XXX: not done yet
    }

    public String getId() {
        return ImplementAllAbstractMethods.class.getName();
    }
    
    public String getDisplayName() {
        return NbBundle.getMessage(ImplementAllAbstractMethods.class, "LBL_Impl_Abstract_Methods"); // NOI18N
    }
    
    public String getDescription() {
        return NbBundle.getMessage(ImplementAllAbstractMethods.class, "DSC_Impl_Abstract_Methods"); // NOI18N
    }
    
    private static TreePath deepTreePath(CompilationInfo info, int offset) {
        TreePath basic = info.getTreeUtilities().pathFor(offset);
        TreePath plusOne = info.getTreeUtilities().pathFor(offset + 1);
        
        if (plusOne.getParentPath() != null && plusOne.getParentPath().getLeaf() == basic.getLeaf()) {
            return plusOne;
        }
        
        return basic;
    }
    
    static abstract class ImplementFixBase implements Fix, Task<WorkingCopy>, DebugFix {
        protected final JavaSource      source;
        protected final TreePathHandle  handle;
        protected final ElementHandle<TypeElement>  implementType;
        
        protected TreePath  path;
        private  boolean   commit;
        protected WorkingCopy copy;
        
        private   int round;
        private List<ElementHandle<? extends Element>> elementsToImplement;

        protected ImplementFixBase(CompilationInfo info, TreePath p, TypeElement el) {
            this.source = info.getJavaSource();
            this.handle = TreePathHandle.create(p, info);
            this.implementType = el == null ? null : ElementHandle.create(el);
        }

        protected ImplementFixBase(CompilationInfo info, Element p, TypeElement el) {
            this.source = info.getJavaSource();
            this.handle = TreePathHandle.create(p, info);
            this.implementType = el == null ? null : ElementHandle.create(el);
        }

        @Override
        public ChangeInfo implement() throws Exception {
            if (implementType != null) {
                final Future[] selector = { null };
                source.runUserActionTask(new Task<CompilationController>() {
                    @Override
                    public void run(CompilationController ctrl) throws Exception {
                        TypeElement tel = implementType.resolve(ctrl);
                        selector[0] = ImplementOverrideMethodGenerator.selectMethodsToImplement(ctrl, tel);
                    }
                }, true);
                if (selector[0] != null) {
                    Future< List<ElementHandle<? extends Element>> > f = (Future< List<ElementHandle<? extends Element>> > )selector[0];
                    elementsToImplement = f.get();
                    if (elementsToImplement == null) {
                        // cancelled.
                        return null;
                    }
                }
            }
            // first round, generate curly braces after each member which does
            // not have any
            ModificationResult res = source.runModificationTask(this);
            if (!commit) {
                return null;
            }
            
            commit = false;
            round++;
            res = source.runModificationTask(this);
            if (commit) {
                res.commit();
            }
            return null;
        }
        
        protected abstract boolean executeRound(Element el, int round) throws Exception;

        @Override
        public void run(WorkingCopy parameter) throws Exception {
            this.copy = parameter;
            parameter.toPhase(Phase.RESOLVED);
            path = handle.resolve(parameter);
            if (path == null) {
                return;
            }

            Element el = copy.getTrees().getElement(path);
            if (el == null) {
                return;
            }
            commit = executeRound(el, round);
        }
        
        protected boolean generateClassBody(TreePath p) throws Exception {
            Element e = copy.getTrees().getElement(p);
            boolean isUsableElement = e != null && (e.getKind().isClass() || e.getKind().isInterface());
            if (isUsableElement) {
                return true;
            }
            if (e.getKind() == ElementKind.ENUM_CONSTANT) {
                VariableTree var = (VariableTree) p.getLeaf();
                if (var.getInitializer() != null && var.getInitializer().getKind() == Kind.NEW_CLASS) {
                    NewClassTree nct = (NewClassTree) var.getInitializer();
                    if (nct.getClassBody() != null) {
                        return true;
                    }
                }
            }
            return !generateClassBody2(copy, p);
        }
        
        protected boolean generateImplementation(TreePath p) {
            Tree leaf = p.getLeaf();
            
            if (TreeUtilities.CLASS_TREE_KINDS.contains(leaf.getKind())) {
                generateAllAbstractMethodImplementations(copy, p, elementsToImplement);
                return true;
            }
            Element e = copy.getTrees().getElement(p);
            if (e != null && e.getKind() == ElementKind.ENUM_CONSTANT) {
                VariableTree var = (VariableTree) leaf;
                if (var.getInitializer() != null && var.getInitializer().getKind() == Kind.NEW_CLASS) {
                    NewClassTree nct = (NewClassTree) var.getInitializer();
                    assert nct.getClassBody() != null;
                    TreePath enumInit = new TreePath(p, nct);
                    TreePath toModify = new TreePath(enumInit, nct.getClassBody());
                    generateAllAbstractMethodImplementations(copy, toModify, elementsToImplement);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Fix which implements the [missing] abstract methods on all enum's values. It does so
     * in two phases: during the first, each of the enum values, which does not (yet) specify
     * class body, gets curly braces. The source is then reparsed to get fresh trees.
     * After that, method generation is applied on all enum values.
     */
    @NbBundle.Messages({
        "LBL_FIX_Impl_Methods_Enum_Values2=XImplement abstract methods on all enum values"
    })
    static final class ImplementOnEnumValues2 extends ImplementFixBase {

        public ImplementOnEnumValues2(CompilationInfo info, Element e) {
            super(info, e, (TypeElement)e);
        }

        @Override
        public String getText() {
            return Bundle.LBL_FIX_Impl_Methods_Enum_Values2();
        }
        
        @Override
        protected boolean executeRound(Element el, int round) throws Exception {
            if (el.getKind() != ElementKind.ENUM) {
                return false;
            }
            ClassTree ct = (ClassTree)path.getLeaf();
            for (ListIterator<? extends Tree> it = ct.getMembers().listIterator(ct.getMembers().size()); it.hasPrevious(); ) {
                Tree t = it.previous();
                
                if (t.getKind() != Tree.Kind.VARIABLE) {
                    continue;
                }
                TreePath p = new TreePath(path, t);
                Element e = copy.getTrees().getElement(p);
                if (e == null || e.getKind() != ElementKind.ENUM_CONSTANT) {
                    continue;
                }

                switch (round) {
                    case 0:
                        if (!generateClassBody(p)) {
                            return false;
                        }
                        break;
                    case 1:
                        if (!generateImplementation(p)) {
                            return false;
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
            return true;
        }
        
        @Override
        public String toDebugString() {
             return "IOEV";
        }
    }
    
    private static boolean generateClassBody2(WorkingCopy copy, TreePath p) throws Exception {
        int insertOffset = (int) copy.getTrees().getSourcePositions().getEndPosition(copy.getCompilationUnit(), p.getLeaf());
        if (insertOffset == -1) {
            return false;
        }
        try {
            copy.getDocument().insertString(insertOffset, " {}", null);
        } catch (GuardedException ex) {
            String message = NbBundle.getMessage(ImplementAllAbstractMethods.class, "ERR_CannotApplyGuarded");
            StatusDisplayer.getDefault().setStatusText(message);
            return true;
        } catch (BadLocationException | IOException ex) {
            Exceptions.printStackTrace(ex);
            return true;
        }
        return false;
    }

    /**
     * Makes the class abstract. If the class is final, the final modifier is removed.
     */
    private static class MakeAbstractFix extends JavaFix implements DebugFix {
        private final String makeClassAbstractName;

        public MakeAbstractFix(CompilationInfo info, TreePath tp, String makeClassAbstractName) {
            super(info, tp);
            this.makeClassAbstractName = makeClassAbstractName;
        }

        @Override
        protected String getText() {
            return NbBundle.getMessage(ImplementAllAbstractMethods.class, "LBL_FIX_Make_Class_Abstract", makeClassAbstractName); // MOI18N 
        }

        @Override
        protected void performRewrite(TransformationContext ctx) throws Exception {
            WorkingCopy wc = ctx.getWorkingCopy();
            Tree.Kind k = ctx.getPath().getLeaf().getKind();
            if (!TreeUtilities.CLASS_TREE_KINDS.contains(k)) {
                // TODO: report
                return;
            }
            ClassTree ct = (ClassTree)ctx.getPath().getLeaf();
            ModifiersTree mt = ct.getModifiers();
            Set<Modifier> mods = new HashSet<>(mt.getFlags());
            mods.remove(Modifier.FINAL);
            mods.add(Modifier.ABSTRACT);
            ModifiersTree newMt = wc.getTreeMaker().Modifiers(mods, mt.getAnnotations());
            wc.rewrite(mt, newMt);
        }

        @Override
        public String toDebugString() {
            return "MA:" + makeClassAbstractName;
        }
    }
    
    private static class ImplementAbstractMethodsFix extends ImplementFixBase {
        public ImplementAbstractMethodsFix(CompilationInfo info, TreePath path, TypeElement e) {
            super(info, path, e);
        }
        
        @Override
        public String getText() {
            return NbBundle.getMessage(ImplementAbstractMethodsFix.class, "LBL_FIX_Impl_Abstract_Methods"); // MOI18N 
        }

        @Override
        protected boolean executeRound(Element el, int round) throws Exception {
            switch (round) {
                case 0:
                    return generateClassBody(path);
                case 1:
                    return generateImplementation(path);
            }
            return false;
        }
        @Override
        public String toDebugString() {
            return "IAAM";
        }
    }
    
    // copy from GeneratorUtils, need to change the processing a little.
    public static Map<? extends ExecutableElement, ? extends ExecutableElement> generateAllAbstractMethodImplementations(
            WorkingCopy wc, TreePath path, List<ElementHandle<? extends Element>> toImplementHandles) {
        assert TreeUtilities.CLASS_TREE_KINDS.contains(path.getLeaf().getKind());
        TypeElement te = (TypeElement)wc.getTrees().getElement(path);
        if (te == null) {
            return null;
        }
        Map<? extends ExecutableElement, ? extends ExecutableElement> ret;
        ClassTree clazz = (ClassTree)path.getLeaf();
        GeneratorUtilities gu = GeneratorUtilities.get(wc);
        ElementUtilities elemUtils = wc.getElementUtilities();
        List<? extends ExecutableElement> toImplement;
        if (toImplementHandles != null) {
            List<ExecutableElement> els = new ArrayList<>();
            for (ElementHandle<? extends Element> h : toImplementHandles) {
                Element e = h.resolve(wc);
                if (e.getKind() == ElementKind.METHOD) {
                    els.add((ExecutableElement)e);
                }
            }
            toImplement = els;
        } else {
            toImplement = elemUtils.findUnimplementedMethods(te);
        }
        ret = Utilities.findConflictingMethods(wc, te, toImplement);
        if (ret.size() < toImplement.size()) {
            toImplement.removeAll(ret.keySet());
            List<? extends MethodTree> res = gu.createAbstractMethodImplementations(te, toImplement);
            clazz = gu.insertClassMembers(clazz, res);
            wc.rewrite(path.getLeaf(), clazz);
        }
        if (ret.isEmpty()) {
            return ret;
        }
        // should be probably elsewhere: UI separation
        String msg = ret.size() == 1 ?
                NbBundle.getMessage(ImplementAllAbstractMethods.class, "WARN_FoundConflictingMethods1", 
                        ret.keySet().iterator().next().getSimpleName()) :
                NbBundle.getMessage(ImplementAllAbstractMethods.class, "WARN_FoundConflictingMethodsMany", 
                        ret.keySet().size());
        
        StatusDisplayer.getDefault().setStatusText(msg, StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
        return ret;
    }
    interface DebugFix {
        public String toDebugString();
    }
}
