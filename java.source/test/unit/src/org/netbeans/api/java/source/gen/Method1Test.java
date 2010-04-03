/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.api.java.source.gen;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import java.io.File;
import java.util.*;
import java.io.IOException;
import javax.lang.model.element.Modifier;
import org.netbeans.junit.NbTestSuite;
import junit.textui.TestRunner;
import org.netbeans.api.java.source.*;
import static org.netbeans.api.java.source.JavaSource.*;
import org.openide.filesystems.FileUtil;

/**
 * Tests the method generator.
 *
 * @author  Pavel Flaska
 */
public class Method1Test extends GeneratorTestMDRCompat {
    
    /** Need to be defined because of JUnit */
    public Method1Test(String name) {
        super(name);
    }
    
    public static NbTestSuite suite() {
        NbTestSuite suite = new NbTestSuite();
        suite.addTest(new Method1Test("testMethodModifiers"));
        suite.addTest(new Method1Test("testMethodName"));
        suite.addTest(new Method1Test("testMethodParameters"));
        suite.addTest(new Method1Test("testMethodParameterChange"));
        suite.addTest(new Method1Test("testMethodThrows"));
        suite.addTest(new Method1Test("testMethodReturnType"));
        suite.addTest(new Method1Test("test159944"));
        // suite.addTest(new Method1Test("testMethodBody"));
        // suite.addTest(new Method1Test("testParameterizedMethod"));
        // suite.addTest(new Method1Test("testAddRemoveInOneTrans"));
        // suite.addTest(new Method1Test("testCreateNewMethod"));
        return suite;
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        testFile = getFile(getSourceDir(), getSourcePckg() + "MethodTest1.java");
    }

    /**
     * Changes the modifiers on method. Removes public modifier, sets static
     * and private modifier.
     */
    public void testMethodModifiers() throws IOException {
        System.err.println("testMethodModifiers");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(1);
                ModifiersTree origMods = method.getModifiers();
                Set<Modifier> njuMods = new HashSet<Modifier>();
                njuMods.add(Modifier.PRIVATE);
                njuMods.add(Modifier.STATIC);
                workingCopy.rewrite(origMods, make.Modifiers(njuMods));
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodModifiers.pass");
    }
    
    /**
     * Changes the name on the method.
     */
    public void testMethodName() throws IOException {
        System.err.println("testMethodName");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(2);
                workingCopy.rewrite(method, make.setLabel(method, "druhaMetoda"));
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodName.pass");
    }
    
    /**
     * Removes the first parameter and adds it to the end
     */
    public void testMethodParameters() throws IOException {
        System.err.println("testMethodParameters");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(3);
                VariableTree vt = method.getParameters().get(0);
                VariableTree vtCopy = make.Variable(vt.getModifiers(), vt.getName(), vt.getType(), vt.getInitializer());
                MethodTree njuMethod = make.removeMethodParameter(method, 0);
                njuMethod = make.addMethodParameter(njuMethod, vt);
                workingCopy.rewrite(method, njuMethod);
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodParameters.pass");
    }
    
    /**
     * Changes the name of the parameter.
     */
    public void testMethodParameterChange() throws IOException {
        System.err.println("testMethodParameters");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(4);
                List<? extends VariableTree> parameters = method.getParameters();
                VariableTree vt = parameters.get(0);
                MethodTree njuMethod = make.removeMethodParameter(method, 0);
                VariableTree njuParameter = make.setLabel(vt, "aParNewName");
                workingCopy.rewrite(vt, njuParameter);
                // is this really needed? -- Probably bug, setting label for varTree
                // should be enough.
                njuMethod = make.insertMethodParameter(njuMethod, 0, njuParameter);
                workingCopy.rewrite(method, njuMethod);
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodParameterChange.pass");
    }

    /**
     * Removes first exception thrown, adds another one to the end.
     */
    public void testMethodThrows() throws IOException {
        System.err.println("testMethodThrows");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(5);
                ExpressionTree ident = make.Identifier("java.lang.IllegalMonitorStateException");
                MethodTree njuMethod = make.removeMethodThrows(method, 0);
                njuMethod = make.addMethodThrows(njuMethod, ident);
                workingCopy.rewrite(method, njuMethod);
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodThrows.pass");
    }
    
    /**
     * Changes return type to String.
     */
    public void testMethodReturnType() throws IOException {
        System.err.println("testMethodReturnType");
        JavaSource testSource = JavaSource.forFileObject(FileUtil.toFileObject(testFile));
        Task task = new Task<WorkingCopy>() {

            public void run(WorkingCopy workingCopy) throws java.io.IOException {
                workingCopy.toPhase(Phase.RESOLVED);
                TreeMaker make = workingCopy.getTreeMaker();
                ClassTree clazz = (ClassTree) workingCopy.getCompilationUnit().getTypeDecls().get(0);
                MethodTree method = (MethodTree) clazz.getMembers().get(6);
                Tree returnType = method.getReturnType();
                IdentifierTree identifier = make.Identifier("String");
                workingCopy.rewrite(returnType, identifier);
            }
            
        };
        testSource.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(testFile);
        System.err.println(res);
        assertFiles("testMethodReturnType.pass");
    }
    
    public void test159944() throws Exception {
        String test =
                "class Test {\n" +
                "    void m() {\n" +
                "        plus(|1, Math.abs(2));\n" +
                "    }\n" +
                "}";
        String golden =
                "class Test {\n" +
                "    void m() {\n" +
                "        plus(Math.abs(2), 1);\n" +
                "    }\n" +
                "}";
        File file = new File(getWorkDir(), "Test.java");
        final int indexA = test.indexOf("|");
        assertTrue(indexA != -1);
        TestUtilities.copyStringToFile(file, test.replace("|", ""));
        JavaSource src = getJavaSource(file);
        Task<WorkingCopy> task = new Task<WorkingCopy>() {

            public void run(WorkingCopy copy) throws Exception {
                if (copy.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                    return;
                }
                Tree node = copy.getTreeUtilities().pathFor(indexA).getLeaf();
                assertEquals(Kind.METHOD_INVOCATION, node.getKind());
                TreeMaker make = copy.getTreeMaker();
                MethodInvocationTree original = (MethodInvocationTree) node;
                List<? extends ExpressionTree> oldArgs = original.getArguments();
                List<ExpressionTree> newArgs = new ArrayList<ExpressionTree>();
                newArgs.add(oldArgs.get(1));
                newArgs.add(oldArgs.get(0));
                @SuppressWarnings("unchecked")
                MethodInvocationTree modified = make.MethodInvocation(
                        (List<? extends ExpressionTree>) original.getTypeArguments(),
                        original.getMethodSelect(), newArgs);
                System.out.println("original: " + node);
                System.out.println("modified: " + modified);
                copy.rewrite(node, modified);            }
        };
        src.runModificationTask(task).commit();
        String res = TestUtilities.copyFileToString(file);
        assertEquals(golden, res);
    }

    /**
     * Tests method body.
     */
//    public void testMethodBody() throws IOException {
//        System.err.println("testMethodBody");
//        process(
//            new Transformer<Void, Object>() {
//                public Void visitMethod(MethodTree node, Object p) {
//                    super.visitMethod(node, p);
//                    if ("seventhMethod".contentEquals(node.getName())) {
//                        Set<Modifier> njuMods = new HashSet<Modifier>();
//                        njuMods.add(Modifier.PUBLIC);
//                        MethodTree njuMethod = make.Method(
//                            make.Modifiers(njuMods),
//                            node.getName(),
//                            (ExpressionTree) node.getReturnType(),
//                            node.getTypeParameters(),
//                            node.getParameters(),
//                            node.getThrows(),
//                            make.Block(Collections.EMPTY_LIST, false),
//                            (ExpressionTree) node.getDefaultValue()
//                        );
//                        changes.rewrite(node, njuMethod);
//                    } else if ("eighthMethod".contentEquals(node.getName())) {
//                        Set<Modifier> njuMods = new HashSet<Modifier>();
//                        njuMods.add(Modifier.PUBLIC);
//                        njuMods.add(Modifier.ABSTRACT);
//                        MethodTree njuMethod = make.Method(
//                            make.Modifiers(njuMods),
//                            node.getName(),
//                            (ExpressionTree) node.getReturnType(),
//                            node.getTypeParameters(),
//                            node.getParameters(),
//                            node.getThrows(),
//                            null,
//                            (ExpressionTree) node.getDefaultValue()
//                        );
//                        changes.rewrite(node, njuMethod);
//                    }
//                    else if ("interfaceMethod".contentEquals(node.getName())) {
//                        Set<Modifier> njuMods = new HashSet<Modifier>();
//                        njuMods.add(Modifier.PUBLIC);
//                        njuMods.add(Modifier.ABSTRACT);
//                        ExpressionTree ident = make.Identifier("java.io.IOException");
//                        MethodTree njuMethod = make.Method(
//                            make.Modifiers(njuMods),
//                            node.getName(),
//                            (ExpressionTree) node.getReturnType(),
//                            node.getTypeParameters(),
//                            node.getParameters(),
//                            Collections.<ExpressionTree>singletonList(ident),
//                            null,
//                            (ExpressionTree) node.getDefaultValue()
//                        );
//                        changes.rewrite(node, njuMethod);
//                    }
//                    return null;
//                }
//        });
//        assertFiles("testMethodBody.pass");
//    }
//
//    public void testCreateNewMethod() throws IOException {
//        System.err.println("testCreateNewMethod");
//        process(new Transformer<Void, Object>() {
//            public Void visitClass(ClassTree node, Object p) {
//                super.visitClass(node, p);
//                if ("TestInterface".equals(node.getSimpleName().toString())) {
//                    return null; // do it just for outer class
//                }
//                // create method modifiers
//                ModifiersTree parMods = make.Modifiers(Collections.EMPTY_SET, Collections.EMPTY_LIST);
//                // create parameters
//                VariableTree par1 = make.Variable(parMods, "a", make.PrimitiveType(TypeKind.INT), null);
//                VariableTree par2 = make.Variable(parMods, "b", make.PrimitiveType(TypeKind.FLOAT), null);
//                List<VariableTree> parList = new ArrayList<VariableTree>(2);
//                parList.add(par1);
//                parList.add(par2);
//                // create method
//                MethodTree newMethod = make.Method(
//                    make.Modifiers(
//                        Collections.singleton(Modifier.PUBLIC), // modifiers
//                        Collections.EMPTY_LIST // annotations
//                    ), // modifiers and annotations
//                    "newlyCreatedMethod", // name
//                    make.PrimitiveType(TypeKind.VOID), // return type
//                    Collections.EMPTY_LIST, // type parameters for parameters
//                    parList, // parameters
//                    Collections.singletonList(make.Identifier("java.io.IOException")), // throws 
//                    make.Block(Collections.EMPTY_LIST, false), // empty statement block
//                    null // default value - not applicable here, used by annotations
//                );
//                changes.rewrite(node, make.addClassMember(node, newMethod));
//                return null;
//            }
//        });
//        assertFiles("testCreateNewMethod.pass");
//    }
//        
//    public void testParameterizedMethod() throws IOException {
//        //Transformer tm; tm.
//        System.err.println("testParameterizedMethod");
//        process(new MutableTransformer<Void, Object>() {
//            public Void visitClass(ClassTree node, Object p) {
//                super.visitClass(node, p);
//                if ("TestInterface".equals(node.getSimpleName().toString())) {
//                    return null; // do it just for outer class
//                }
//                // create method modifiers
//                ModifiersTree parMods = make.Modifiers(Collections.EMPTY_SET, Collections.EMPTY_LIST);
//                // create type params/parameters
//                TypeParameterTree tpt = make.TypeParameter("T", Collections.<ExpressionTree>emptyList());
//                VariableTree par1 = make.Variable(parMods, "cl", make.Identifier("T"), null);
//                // create method
//                MethodTree newMethod = Method( // remove mutable transformer
//                    make.Modifiers(
//                        Collections.singleton(Modifier.PUBLIC), // modifiers
//                        Collections.EMPTY_LIST // annotations
//                    ), // modifiers and annotations
//                    "getIt", // name
//                    make.Identifier("T"), // return type
//                    Collections.<TypeParameterTree>singletonList(tpt), // type parameters for parameters
//                    Collections.<VariableTree>singletonList(par1), // parameters
//                    Collections.<ExpressionTree>emptyList(), // throws 
//                    "{ return null; }",
//                    null // default value - not applicable here, used by annotations
//                );
//                attach(newMethod, "New method.");
//                changes.rewrite(node, make.addClassMember(node, newMethod));
//                return null;
//            }
//        });
//        assertFiles("testParameterizedMethod.pass");
//    }
//    
//    public void testAddRemoveInOneTrans() throws IOException {
//        System.err.println("testAddRemoveInOneTrans");
//        process(new MutableTransformer<Void, Object>() {
//            public Void visitClass(ClassTree node, Object p) {
//                super.visitClass(node, p);
//                if ("TestInterface".equals(node.getSimpleName().toString())) {
//                    return null; // do it just for outer class
//                }
//                // create method modifiers
//                ModifiersTree parMods = make.Modifiers(Collections.EMPTY_SET, Collections.EMPTY_LIST);
//                TypeParameterTree typePar = make.TypeParameter("T2", Collections.<ExpressionTree>emptyList());
//                // create parameter "T newPar"
//                VariableTree param = make.Variable(parMods, "newPar", make.Identifier("T2"), null);
//                // create the method
//                MethodTree method = Method(
//                    make.Modifiers(
//                        Collections.singleton(Modifier.PUBLIC), // modifiers
//                        Collections.EMPTY_LIST // annotations
//                    ), // modifiers and annotations
//                    "setIt",
//                    make.Identifier("T2"),
//                    Collections.<TypeParameterTree>singletonList(typePar), // type params
//                    Collections.<VariableTree>singletonList(param), // parameters
//                    Collections.<ExpressionTree>emptyList(), // exceptions
//                    "{ return null; }",
//                    null
//                );
//                ClassTree newClass = make.removeClassMember(node, node.getMembers().size()-1);
//                newClass = make.addClassMember(newClass, method);
//                changes.rewrite(node, newClass);
//                return null;
//            }
//        });
//        assertFiles("testAddRemoveInOneTrans.pass");
//    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    String getSourcePckg() {
        return "org/netbeans/test/codegen/";
    }

    String getGoldenPckg() {
        return "org/netbeans/jmi/javamodel/codegen/MethodTest1/MethodTest1/";
    }

}
