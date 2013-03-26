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
package org.netbeans.modules.refactoring.java.plugins;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.*;
import org.netbeans.modules.refactoring.java.api.ReplaceConstructorWithBuilderRefactoring.Setter;
import org.netbeans.modules.refactoring.java.api.ReplaceConstructorWithBuilderRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import static org.netbeans.modules.refactoring.java.plugins.Bundle.*;
import org.netbeans.api.java.source.matching.Occurrence;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.java.spi.DiffElement;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import org.netbeans.spi.java.hints.support.TransformationSupport;
import org.netbeans.spi.java.hints.support.TransformationSupport.Transformer;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Becicka
 */
@NbBundle.Messages({"# {0} - ParameterName", "WRN_NODEFAULT=Parameter {0}'s setter is optional but has no default value.",
                    "# {0} - ParameterName", "ERR_GenericOptional=Parameter {0} is a generic type, it's setter cannot be optional.",
                    "ERR_ReplaceAbstract=Cannot Replace Constructor with Builder in an abstract class."})
public class ReplaceConstructorWithBuilderPlugin extends JavaRefactoringPlugin {
 
    private final ReplaceConstructorWithBuilderRefactoring refactoring;
    
    private final AtomicBoolean cancel = new AtomicBoolean();
    private TreePathHandle treePathHandle;

    public ReplaceConstructorWithBuilderPlugin(ReplaceConstructorWithBuilderRefactoring refactoring) {
        this.refactoring = refactoring;
        this.treePathHandle = refactoring.getRefactoringSource().lookup(TreePathHandle.class);
    }

    @Override
    protected Problem preCheck(CompilationController javac) throws IOException {
        Element constr = treePathHandle.resolveElement(javac);
        if(constr.getKind() != ElementKind.CONSTRUCTOR) {
            return new Problem(true, ERR_ReplaceWrongType());
        }
        Element enclosingElement = constr.getEnclosingElement();
        if(enclosingElement.getModifiers().contains(Modifier.ABSTRACT)) {
            return new Problem(true, ERR_ReplaceAbstract());
        }
        if(!enclosingElement.getModifiers().contains(Modifier.STATIC) && enclosingElement.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            return new Problem(true, ERR_ReplaceWrongInnerType());
        }
        return null;
    }

    @Override
    protected Problem checkParameters(CompilationController javac) throws IOException {
        Problem problem = null;
        javac.toPhase(JavaSource.Phase.RESOLVED);
        TreePath constrPath = treePathHandle.resolve(javac);
        TypeElement type = (TypeElement) javac.getTrees().getElement(constrPath.getParentPath());
        for (Setter setter : refactoring.getSetters()) {
            if(setter.isOptional()) {
                TypeMirror parsed = javac.getTreeUtilities().parseType(setter.getType(), type);
                if(parsed != null && parsed.getKind() == TypeKind.TYPEVAR) {
                    problem = JavaPluginUtils.chainProblems(problem, new Problem(true, NbBundle.getMessage(ReplaceConstructorWithBuilderPlugin.class, "ERR_GenericOptional", setter.getVarName())));
                }
            }
        }
        return problem;
    }

    @Override
    public Problem fastCheckParameters() {
        String builderName = refactoring.getBuilderName();
        if (builderName == null || builderName.length() == 0) {
            return new Problem(true, NbBundle.getMessage(ReplaceConstructorWithBuilderPlugin.class, "ERR_NoFactory"));
        }
        if (!SourceVersion.isName(builderName)) {
            return new Problem(true, NbBundle.getMessage(ReplaceConstructorWithBuilderPlugin.class, "ERR_NotIdentifier", builderName));
        }
        final TreePathHandle constr = treePathHandle;
        ClassPath classPath = ClassPath.getClassPath(constr.getFileObject(), ClassPath.SOURCE);
        String name = refactoring.getBuilderName().replace(".", "/") + ".java";
        FileObject resource = classPath.findResource(name);
        if (resource !=null) {
            return new Problem(true, NbBundle.getMessage(ReplaceConstructorWithBuilderPlugin.class, "ERR_FileExists", name));
        }
        Problem problem = null;
        for (Setter set : refactoring.getSetters()) {
            if(set.isOptional() && set.getDefaultValue() == null) {
                problem = JavaPluginUtils.chainProblems(problem, new Problem(false, NbBundle.getMessage(ReplaceConstructorWithBuilderPlugin.class, "WRN_NODEFAULT", set.getVarName())));
            }
        }
        return problem;
    }

    @Override
    public final Problem prepare(RefactoringElementsBag refactoringElements) {
        cancel.set(false);
        final TreePathHandle constr = refactoring.getRefactoringSource().lookup(TreePathHandle.class);
        final String[] ruleCode = new String[1];
        final String[] parentSimpleName = new String[1];

        try {
            ModificationResult mod = JavaSource.forFileObject(constr.getFileObject()).runModificationTask(new Task<WorkingCopy>() {

                @Override
                public void run(WorkingCopy workingCopy) throws Exception {
                    workingCopy.toPhase(JavaSource.Phase.RESOLVED);
                    TreePath constrPath = constr.resolve(workingCopy);
                    ExecutableElement element = (ExecutableElement) workingCopy.getTrees().getElement(constrPath);
                    MethodTree constructor = (MethodTree) constrPath.getLeaf();
                    TypeElement parent = (TypeElement) workingCopy.getTrees().getElement(constrPath.getParentPath());
                    parentSimpleName[0] = parent.getSimpleName().toString();
                    TreeMaker make = workingCopy.getTreeMaker();
                    StringBuilder parameters = new StringBuilder();
                    StringBuilder constraints = new StringBuilder();
                    StringBuilder realParameters = new StringBuilder();
                    for (int count = 0; count < constructor.getParameters().size(); count++) {
                        VariableTree vt = constructor.getParameters().get(count);
                        if (count > 0) {
                            parameters.append(", "); //NOI18N
                            constraints.append(" && "); //NOI18N
                            realParameters.append(", "); //NOI18N
                        }
                        realParameters.append(vt.getName());
                        parameters.append("$").append(count+1); //NOI18N
                        if(count == constructor.getParameters().size() -1 &&
                                element.isVarArgs()) {
                            parameters.append("$");
                        } else {
                            constraints.append("$").append(count+1).append(" instanceof ").append(workingCopy.getTrees().getTypeMirror(new TreePath(new TreePath(constrPath, vt), vt.getType()))); //NOI18N
                        }
                    }
                    List members = new ArrayList();
                    final String simpleName = refactoring.getBuilderName().substring(refactoring.getBuilderName().lastIndexOf('.') + 1);

                    StringBuilder args = null;

                    for (Setter set : refactoring.getSetters()) {
                        String type = set.getType();
                        boolean varargs = false;
                        if(type.endsWith("...")) { //NOI18N
                            type = type.substring(0, type.length() -3);
                            varargs = true;
                        }
                        ExpressionTree ident = make.QualIdent(type);
                        if(varargs) {
                            ident = (ExpressionTree) make.ArrayType(ident);
                        }
                        members.add(make.Variable(
                                make.Modifiers(Collections.singleton(Modifier.PRIVATE)),
                                set.getVarName(), ident,
                                set.getDefaultValue() == null ? null : make.Identifier(set.getDefaultValue())));
                        if (args == null) {
                            args = new StringBuilder();
                        } else {
                            args.append(", "); //NOI18N
                        }
                        args.append(set.getVarName());
                    }

                    members.add(make.Constructor(
                            make.Modifiers(EnumSet.of(Modifier.PUBLIC)),
                            Collections.<TypeParameterTree>emptyList(),
                            Collections.<VariableTree>emptyList(),
                            Collections.<ExpressionTree>emptyList(),
                            "{}")); //NOI18N
                    
                    ClassTree parentTree = (ClassTree) constrPath.getParentPath().getLeaf();
                    List<? extends TypeParameterTree> typeParameters = parentTree.getTypeParameters();
                    List<ExpressionTree> typeArguments = new LinkedList<ExpressionTree>();
                    for (TypeParameterTree vt : typeParameters) {
                        typeArguments.add(make.Identifier(vt.getName()));
                    }
                    
                    Tree buildertype = make.Type(simpleName);
                    if(!typeParameters.isEmpty()) {
                        buildertype = make.ParameterizedType(buildertype, typeArguments);
                    }

                    for (Setter set : refactoring.getSetters()) {
                        List<StatementTree> stmts = new LinkedList<StatementTree>();
                        stmts.add(make.ExpressionStatement(make.Assignment(make.Identifier("this." + set.getVarName()), make.Identifier(set.getVarName())))); //NOI18N
                        stmts.add(make.Return(make.Identifier("this"))); //NOI18N
                        BlockTree body = make.Block(stmts, false);
                        String type = set.getType();
                        boolean varargs = false;
                        if(type.endsWith("...")) { //NOI18N
                            type = type.substring(0, type.length() -3);
                            varargs = true;
                        }
                        ExpressionTree ident = make.QualIdent(type);
                        members.add(make.Method(
                                make.Modifiers(EnumSet.of(Modifier.PUBLIC)),
                                set.getName(),
                                buildertype,
                                Collections.<TypeParameterTree>emptyList(),
                                Collections.<VariableTree>singletonList(make.Variable(make.Modifiers(Collections.<Modifier>emptySet()), set.getVarName(), ident, null)),
                                Collections.<ExpressionTree>emptyList(),
                                body,
                                null,
                                varargs));
                    }
                    
                    List<ExpressionTree> arguments = new LinkedList<ExpressionTree>();
                    for (VariableTree vt : constructor.getParameters()) {
                        arguments.add(make.Identifier(vt.getName()));
                    }
                    
                    ExpressionTree ident = make.QualIdent(parent);
                    if(!typeArguments.isEmpty()) {
                        ident = (ExpressionTree) make.ParameterizedType(ident, typeArguments);
                    }
                    
                    BlockTree body = make.Block(Collections.singletonList(make.Return(make.NewClass(null, Collections.EMPTY_LIST, ident, arguments, null))), false);

                    members.add(make.Method(
                            make.Modifiers(EnumSet.of(Modifier.PUBLIC)),
                            "create" + parent.getSimpleName(), //NOI18N
                            make.Type(parent.asType()),
                            Collections.<TypeParameterTree>emptyList(),
                            Collections.<VariableTree>emptyList(),
                            Collections.<ExpressionTree>emptyList(),
                            body,
                            //"{return new " + parent.getSimpleName() + (hasTypeParams? "<$modifiers$>(" : "(") + (args==null?"":args) + ");}", //NOI18N
                            null));


                    ClassTree builder = make.Class(make.Modifiers(EnumSet.of(Modifier.PUBLIC)), simpleName,
                            typeParameters,
                            null,
                            Collections.EMPTY_LIST,
                            members);
                    FileObject root = ClassPath.getClassPath(constr.getFileObject(), ClassPath.SOURCE).findOwnerRoot(constr.getFileObject());
                    CompilationUnitTree builderUnit = make.CompilationUnit(root, refactoring.getBuilderName().replace('.', '/') + ".java", Collections.EMPTY_LIST, Collections.singletonList(builder));
                    workingCopy.rewrite(null, builderUnit);
                    StringBuilder rule = new StringBuilder();
                    boolean hasTypeParams = !parent.getTypeParameters().isEmpty();
                    rule.append("new ").append(parent.getQualifiedName()).append(hasTypeParams? "<$modifiers$>(" : "(").append(parameters).append(")"); //NOI18N
                    if (constraints.length() > 0) {
                        rule.append(" :: ").append(constraints); //NOI18N
                    }
                    rule.append(";;"); //NOI18N
                    ruleCode[0] = rule.toString();
                }
            });

            List<ModificationResult> results = new ArrayList<ModificationResult>();

            results.add(mod);

            results.addAll(TransformationSupport.create(ruleCode[0], new Transformer() {

                @Override
                public void transform(WorkingCopy copy, Occurrence occurrence) {
                    final TreeMaker make = copy.getTreeMaker();
                    Element element = copy.getTrees().getElement(occurrence.getOccurrenceRoot());
                    ExecutableElement constrElement = (ExecutableElement) constr.resolveElement(copy);
                    if(!constrElement.equals(element)) {
                        return;
                    }
                    Collection<? extends TreePath> modifiers = occurrence.getMultiVariables().get("$modifiers$");
                    ExpressionTree ident = make.QualIdent(refactoring.getBuilderName());
                    if(modifiers != null) {
                        LinkedList<Tree> arguments = new LinkedList<Tree>();
                        for (TreePath treePath : modifiers) {
                            arguments.add(treePath.getLeaf());
                        }
                        ident = (ExpressionTree) make.ParameterizedType(ident, arguments);
                    }
                    ExpressionTree expression = make.NewClass(null, Collections.EMPTY_LIST, ident, Collections.EMPTY_LIST, null);
                    
                    Map<String, TreePath> variables = occurrence.getVariables();
                    Map<String, Collection<? extends TreePath>> multiVariables = occurrence.getMultiVariables();
                    for (int i = 1; i <= refactoring.getSetters().size(); i++) {
                        Setter set = refactoring.getSetters().get(i-1);
                        final List<ExpressionTree> arguments;
                        if(variables.containsKey("$" + i)) { //NOI18N
                            final Tree value = variables.get("$" + i).getLeaf(); //NOI18N
                            if (set.isOptional() && treeEquals(copy, value, set.getDefaultValue())) {
                                continue;
                            }
                            arguments = Collections.singletonList((ExpressionTree)value);
                        } else {
                            Collection<? extends TreePath> value = multiVariables.get("$" + i + "$"); //NOI18N
                            if (set.isOptional() && treeEquals(copy, value, set.getDefaultValue())) {
                                continue;
                            }
                            arguments = createArguments(copy, value);
                        }
                        
                        expression = make.MethodInvocation(
                                Collections.<ExpressionTree>emptyList(),
                                make.MemberSelect(expression, set.getName()),
                                arguments);
                    }

                    MethodInvocationTree create = make.MethodInvocation(
                            Collections.<ExpressionTree>emptyList(),
                            make.MemberSelect(expression, "create" + parentSimpleName[0]), //NOI18N
                            Collections.<ExpressionTree>emptyList());


                    copy.rewrite(occurrence.getOccurrenceRoot().getLeaf(), create);
                }
            }).setCancel(cancel).processAllProjects());

            createAndAddElements(refactoring, refactoringElements, results);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }
    
    //TODO: only one copy!
    public static void createAndAddElements(AbstractRefactoring refactoring, RefactoringElementsBag elements, Collection<ModificationResult> results) {
        elements.registerTransaction(JavaRefactoringPlugin.createTransaction(results));
        for (ModificationResult result:results) {
            for (FileObject jfo : result.getModifiedFileObjects()) {
                for (ModificationResult.Difference diff: result.getDifferences(jfo)) {
                    elements.add(refactoring, DiffElement.create(diff, jfo, result));
                }
            }
        }
    }


    private boolean treeEquals(WorkingCopy copy, Tree value, String defaultValue) {
        if (defaultValue == null) {
            return false;
        }
        defaultValue = defaultValue.trim();
        if (value instanceof LiteralTree) {
            ExpressionTree parseExpression = copy.getTreeUtilities().parseExpression(defaultValue, new SourcePositions[1]);
            if (parseExpression instanceof LiteralTree) {
                return ((LiteralTree) value).getValue().equals(((LiteralTree) parseExpression).getValue());
            }
        }
        return defaultValue.equals(value.toString());
    }
    
    private boolean treeEquals(WorkingCopy copy, Collection<? extends TreePath> value, String defaultValue) {
        if (defaultValue == null) {
            return false;
        }
        String[] values = defaultValue.split(",");
        if(values.length != value.size()) {
            return false;
        }
        for (String defValue : values) {
            defValue = defValue.trim();
            boolean parsed = false;
            if (value instanceof LiteralTree) {
                ExpressionTree parseExpression = copy.getTreeUtilities().parseExpression(defValue, new SourcePositions[1]);
                parsed = parseExpression instanceof LiteralTree;
                if (parsed && !((LiteralTree) value).getValue().equals(((LiteralTree) parseExpression).getValue())) {
                    return false;
                }
            }
            if(!parsed && !defValue.equals(value.toString())) {
                return false;
            }
        }
        return true;
    }

    private List<ExpressionTree> createArguments(WorkingCopy copy, Collection<? extends TreePath> value) {
        List<ExpressionTree> arguments = new LinkedList<ExpressionTree>();
        for (TreePath treePath : value) {
            arguments.add((ExpressionTree)treePath.getLeaf());
        }
        return arguments;
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
