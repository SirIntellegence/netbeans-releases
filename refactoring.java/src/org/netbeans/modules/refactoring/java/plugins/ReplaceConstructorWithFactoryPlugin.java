/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.refactoring.java.plugins;

import org.netbeans.modules.refactoring.java.api.ReplaceConstructorWithFactoryRefactoring;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.java.source.*;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import static org.netbeans.modules.refactoring.java.plugins.Bundle.*;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.spi.java.hints.support.TransformationSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
@NbBundle.Messages({"ERR_ReplaceWrongType=Cannot Replace Constructor with Factory of this object. A constructor has to be selected.",
                    "ERR_ReplaceWrongInnerType=Cannot Replace Constructor with Factory in non-static inner class."})
public class ReplaceConstructorWithFactoryPlugin extends JavaRefactoringPlugin {

    private final ReplaceConstructorWithFactoryRefactoring refactoring;
    
    private final AtomicBoolean cancel = new AtomicBoolean();
    private final TreePathHandle treePathHandle;

    public ReplaceConstructorWithFactoryPlugin(ReplaceConstructorWithFactoryRefactoring replaceConstructorRefactoring) {
        this.refactoring = replaceConstructorRefactoring;
        treePathHandle = refactoring.getRefactoringSource().lookup(TreePathHandle.class);
    }

    @Override
    protected Problem preCheck(CompilationController javac) throws IOException {
        Element constr = treePathHandle.resolveElement(javac);
        if(constr.getKind() != ElementKind.CONSTRUCTOR) {
            return new Problem(true, ERR_ReplaceWrongType());
        }
        Element enclosingElement = constr.getEnclosingElement();
        if(!enclosingElement.getModifiers().contains(Modifier.STATIC) && enclosingElement.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            return new Problem(true, ERR_ReplaceWrongInnerType());
        }
        return null;
    }

    @Override
    public Problem checkParameters() {
        return null;
    }

    @Override
    public Problem fastCheckParameters() {
        String factoryName = refactoring.getFactoryName();
        
        if (factoryName == null || factoryName.length() == 0) {
            return new Problem(true, "No factory method name specified.");
        }
        if (!SourceVersion.isIdentifier(factoryName)) {
            return new Problem(true, factoryName + " is not an identifier.");
        }
        return null;
    }

    public final Problem prepare(RefactoringElementsBag refactoringElements) {
        cancel.set(false);
//        TreePathHandle tph = refactoring.getRefactoringSource().lookup(TreePathHandle.class);
//        FileObject file = tph.getFileObject();
//        ClassPath source = ClassPath.getClassPath(file, ClassPath.SOURCE);
//        FileObject sourceRoot = source.findOwnerRoot(file);
        final TreePathHandle constr = treePathHandle;
        final String[] ruleCode = new String[1];
        final String[] toCode = new String[1];

        try {
            ModificationResult mod = JavaSource.forFileObject(constr.getFileObject()).runModificationTask(new Task<WorkingCopy>() {

                @Override
                public void run(WorkingCopy parameter) throws Exception {
                    parameter.toPhase(JavaSource.Phase.RESOLVED);
                    TreePath constrPath = constr.resolve(parameter);
                    MethodTree constructor = (MethodTree) constrPath.getLeaf();
                    TypeElement parent = (TypeElement) parameter.getTrees().getElement(constrPath.getParentPath());
                    TreeMaker make = parameter.getTreeMaker();
                    StringBuilder parameters = new StringBuilder();
                    StringBuilder constraints = new StringBuilder();
                    StringBuilder realParameters = new StringBuilder();
                    int count = 1;
                    for (VariableTree vt : constructor.getParameters()) {
                        if (count > 1) {
                            parameters.append(", ");
                            constraints.append(" && ");
                            realParameters.append(", ");
                        }
                        realParameters.append(vt.getName());
                        parameters.append("$").append(count);
                        constraints.append("$").append(count).append(" instanceof ").append(parameter.getTrees().getTypeMirror(new TreePath(new TreePath(constrPath, vt), vt.getType())));
                        count++;
                    }
                    EnumSet<Modifier> factoryMods = EnumSet.of(Modifier.STATIC);
                    factoryMods.addAll(constructor.getModifiers().getFlags());
                    
                    ClassTree parentTree = (ClassTree) constrPath.getParentPath().getLeaf();
                    List<? extends TypeParameterTree> typeParameters = parentTree.getTypeParameters();
                    
                    List<ExpressionTree> arguments = new LinkedList<ExpressionTree>();
                    for (VariableTree vt : constructor.getParameters()) {
                        arguments.add(make.Identifier(vt.getName()));
                    }
                    
                    List<ExpressionTree> typeArguments = new LinkedList<ExpressionTree>();
                    for (TypeParameterTree vt : typeParameters) {
                        typeArguments.add(make.Identifier(vt.getName()));
                    }
                    ExpressionTree ident = make.QualIdent(parent);
                    if(!typeArguments.isEmpty()) {
                        ident = (ExpressionTree) make.ParameterizedType(ident, typeArguments);
                    }
                    
                    BlockTree body = make.Block(Collections.singletonList(make.Return(make.NewClass(null, Collections.EMPTY_LIST, ident, arguments, null))), false);
                    
                    MethodTree factory = make.Method(make.Modifiers(factoryMods), refactoring.getFactoryName(), ident, typeParameters, constructor.getParameters(), Collections.<ExpressionTree>emptyList(), body, null);
                    parameter.rewrite(constrPath.getParentPath().getLeaf(), GeneratorUtilities.get(parameter).insertClassMember(parentTree, factory));
                    EnumSet<Modifier> constructorMods = EnumSet.of(Modifier.PRIVATE);
                    parameter.rewrite(constructor.getModifiers(), make.Modifiers(constructorMods));
                    StringBuilder rule = new StringBuilder();
                    boolean hasTypeParams = !parent.getTypeParameters().isEmpty();
                    rule.append("new ").append(parent.getQualifiedName()).append(hasTypeParams? "<$modifiers$>(" : "(").append(parameters).append(")");
                    if (constraints.length() > 0) {
                        rule.append(" :: ").append(constraints);
                    }
//                    rule.append(" => ").append(parent.getQualifiedName()).append(".").append(replaceConstructorRefactoring.getFactoryName()).append("(").append(parameters).append(");;");
                    ruleCode[0] = rule.toString();
                    toCode[0] = parent.getQualifiedName() + (hasTypeParams?".<$modifiers$>":".") + refactoring.getFactoryName() + "(" + parameters + ")";
                    toCode[0]+=";;";
                }
            });

            List<ModificationResult> results = new ArrayList<ModificationResult>();

            results.add(mod);

            results.addAll(TransformationSupport.create(ruleCode[0] + " => " + toCode[0]).setCancel(cancel).processAllProjects());

            ReplaceConstructorWithBuilderPlugin.createAndAddElements(refactoring, refactoringElements, results);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null/*XXX*/;
    }

    @Override
    public void cancelRequest() {
        cancel.set(true);
    }

    @Override
    protected JavaSource getJavaSource(Phase p) {
        ClasspathInfo cpInfo = getClasspathInfo(refactoring);
        return JavaSource.create(cpInfo, treePathHandle.getFileObject());
    }

}
