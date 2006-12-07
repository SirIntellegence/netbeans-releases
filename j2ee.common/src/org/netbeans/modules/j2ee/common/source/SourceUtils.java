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

package org.netbeans.modules.j2ee.common.source;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;

/**
 *
 * @author Andrei Badea, Martin Adamek
 */
public class SourceUtils {

    // TODO we could probably also have a SourceUtils(CompilationController, TypeElement) factory method

    /**
     * The compilation controller this instance works with.
     */
    private final CompilationController controller;

    /**
     * The type element this instance works with. Do not use directly, use
     * {@link #getTypeElement} instead.
     */
    private TypeElement typeElement;

    /**
     * The class tree corresponding to {@link #typeElement}. Do not use directly,
     * use {@link #getClassTree} instead.
     */
    private ClassTree classTree;

    // <editor-fold defaultstate="collapsed" desc="Constructors and factory methods">

    SourceUtils(CompilationController controller, TypeElement typeElement) {
        this.controller = controller;
        this.typeElement = typeElement;
    }

    SourceUtils(CompilationController controller, ClassTree classTree) {
        this.controller = controller;
        this.classTree = classTree;
    }

    public static SourceUtils newInstance(CompilationController controller, TypeElement typeElement) {
        Parameters.notNull("controller", controller); // NOI18N
        Parameters.notNull("typeElement", typeElement); // NOI18N

        return new SourceUtils(controller, typeElement);
    }

    public static SourceUtils newInstance(CompilationController controller, ClassTree classTree) {
        Parameters.notNull("controller", controller); // NOI18N
        Parameters.notNull("classTree", classTree); // NOI18N

        return new SourceUtils(controller, classTree);
    }

    public static SourceUtils newInstance(CompilationController controller) throws IOException {
        Parameters.notNull("controller", controller); // NOI18N

        ClassTree classTree = findPublicTopLevelClass(controller);
        if (classTree != null) {
            return newInstance(controller, classTree);
        }
        return null;
    }

    // </editor-fold>

    // <editor-fold desc="Public static methods">

    /**
     * Returns true if the public top-level element (if any) in the given
     * file contains a <code>public static void main(String[])</code> method.
     *
     * @param  fileObject the file to search for a main method.
     * @return true if a main method was found, false otherwise.
     */
    public static boolean hasMainMethod(FileObject fileObject) throws IOException {
        Parameters.notNull("fileObject", fileObject); // NOI18N

        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        final boolean[] result = { false };
        javaSource.runUserActionTask(new AbstractTask<CompilationController>() {
            public void run(CompilationController controller) throws Exception {
                SourceUtils sourceUtils = SourceUtils.newInstance(controller);
                if (sourceUtils != null) {
                    result[0] = sourceUtils.hasMainMethod();
                }
            }
        }, true);
        return result[0];
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Non-public static methods">

    /**
     * Finds the first public top-level type in the compilation unit given by the
     * given <code>CompilationController</code>.
     *
     * This method assumes the restriction that there is at most a public
     * top-level type declaration in a compilation unit, as described in the
     * section 7.6 of the JLS.
     */
    static ClassTree findPublicTopLevelClass(CompilationController controller) throws IOException {
        controller.toPhase(Phase.ELEMENTS_RESOLVED);

        final String mainElementName = controller.getFileObject().getName();
        for (Tree tree : controller.getCompilationUnit().getTypeDecls()) {
            if (tree.getKind() != Tree.Kind.CLASS) {
                continue;
            }
            ClassTree classTree = (ClassTree)tree;
            if (!classTree.getSimpleName().contentEquals(mainElementName)) {
                continue;
            }
            if (!classTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                continue;
            }
            return classTree;
        }
        return null;
    }

    // </editor-fold>

    // <editor-fold desc="Public methods">

    /**
     * Returns the type element that this instance works with
     * (corresponding to {@link #getClassTree}.
     *
     * @return the type element that this instance works with; never null.
     */
    public TypeElement getTypeElement() {
        if (typeElement == null) {
            assert classTree != null;
            TreePath classTreePath = controller.getTrees().getPath(getCompilationController().getCompilationUnit(), classTree);
            typeElement = (TypeElement)controller.getTrees().getElement(classTreePath);
        }
        return typeElement;
    }

    /**
     * Returns the class tree that this instance works with
     * (corresponding to {@link #getTypeElement}.
     *
     * @return the class tree that this instance works with; never null.
     */
    public ClassTree getClassTree() {
        if (classTree == null) {
            assert typeElement != null;
            classTree = controller.getTrees().getTree(typeElement);
        }
        return classTree;
    }

    /**
     * Returns true if {@link #getTypeElement} is a subtype of the given type.
     *
     * @param  type the string representation of a type. The type will be parsed
     *         in the context of {@link #getTypeElement}.
     * @return true {@link #getTypeElement} is a subtype of the given type,
     *         false otherwise.
     */
    public boolean isSubtype(String type) {
        Parameters.notNull("type", type); // NOI18N

        TypeMirror typeMirror = getCompilationController().getTreeUtilities().parseType(type, getTypeElement());
        if (typeMirror != null) {
            return getCompilationController().getTypes().isSubtype(getTypeElement().asType(), typeMirror);
        }
        return false;
    }

    // TODO: will be replaced by Tomas's implementation from J2SE Project
    // covered by hasMainMethod(FileObject) test
    /**
     * Returns true if {@link #getTypeElement} has a main method.
     */
    public boolean hasMainMethod() throws IOException {
        for (ExecutableElement method : ElementFilter.methodsIn(getTypeElement().getEnclosedElements())) {
            if (isMainMethod(method)) {
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Non-public methods">

    /**
     * Returns the <code>CompilationController</code> that this instance
     * works with.
     */
    CompilationController getCompilationController() {
        return controller;
    }

    /**
     * Returns the non-synthetic constructor of the main type element.
     */
    ExecutableElement getDefaultConstructor() throws IOException {
        controller.toPhase(Phase.ELEMENTS_RESOLVED);

        ElementUtilities elementUtils = controller.getElementUtilities();
        for (Element element : getTypeElement().getEnclosedElements()) {
            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructor = (ExecutableElement)element; // XXX is casting allowed after getKind()?
                if (constructor.getParameters().size() == 0 && !elementUtils.isSyntetic(constructor)) {
                    return constructor;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the given method is a main method.
     */
    private boolean isMainMethod(ExecutableElement method) {
        // check method name
        if (!method.getSimpleName().contentEquals("main")) { // NOI18N
            return false;
        }
        // check modifiers
        Set<Modifier> modifiers = method.getModifiers();
        if (!modifiers.contains(Modifier.PUBLIC) || !modifiers.contains(Modifier.STATIC)) {
            return false;
        }
        // check return type
        if (TypeKind.VOID != method.getReturnType().getKind()) {
            return false;
        }
        // check parameters
        // there must be just one parameter
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() != 1) {
            return false;
        }
        VariableElement param = params.get(0); // it is ok to take first item, it was tested before
        TypeMirror paramType = param.asType();
        // parameter must be an array
        if (TypeKind.ARRAY != paramType.getKind()) {
            return false;
        }
        ArrayType arrayType = (ArrayType) paramType;
        TypeElement stringTypeElement = controller.getElements().getTypeElement(String.class.getName());
        // array must be array of Strings
        if (!controller.getTypes().isSameType(stringTypeElement.asType(), arrayType.getComponentType())) {
            return false;
        }
        return true;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inner classes">

    static final class Parameters {

        public static void notNull(CharSequence name, Object value) {
            if (value == null) {
                throw new NullPointerException("The " + name + " parameter cannot be null"); // NOI18N
            }
        }

        public static void notEmpty(CharSequence name, CharSequence value) {
            notNull(name, value);
            if (value.length() == 0) {
                throw new IllegalArgumentException("The " + name + " parameter cannot be an empty string"); // NOI18N
            }
        }

        public static void notWhitespace(CharSequence name, CharSequence value) {
            notNull(name, value);
            notEmpty(name, value.toString().trim());
        }

        public static void javaIdentifier(CharSequence name, CharSequence value) {
            notNull(name, value);
            javaIdentifierOrNull(name, value);
        }

        public static void javaIdentifierOrNull(CharSequence name, CharSequence value) {
            if (value != null && !Utilities.isJavaIdentifier(value.toString())) {
                throw new IllegalArgumentException("The " + name + " parameter ('" + value + "') is not a valid Java identifier"); // NOI18N
            }
        }
    }

    // </editor-fold>
}
