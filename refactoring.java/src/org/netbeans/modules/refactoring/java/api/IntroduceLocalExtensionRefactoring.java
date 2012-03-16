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
package org.netbeans.modules.refactoring.java.api;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.Lookups;

/**
 * Introduce Local Extension Refactoring.
 *
 * A class you are using needs several additional methods, but you can’t
 * modify the class.
 *
 * Create a new class that contains these extra methods. Make this extension
 * class a subclass or a wrapper of the original.
 *
 * After running this refactoring use Move Refactoring to move the extra methods
 * to the newly created class.
 *
 * @author Ralph Ruijs
 * @since 1.34
 */
public final class IntroduceLocalExtensionRefactoring extends AbstractRefactoring {

    private String newName;
    private String packageName;
    private FileObject sourceRoot;
    private boolean wrap;
    private Equality equality;
    private boolean replace;

    /**
     * Create a new instance of IntroduceLocalExtensionRefactoring.
     * Equality will be {@code DELEGATE}.
     *
     * @param handle Path to the type to refactor.
     */
    public IntroduceLocalExtensionRefactoring(@NonNull TreePathHandle handle) {
        super(Lookups.singleton(handle));
        this.equality = Equality.DELEGATE;
        this.wrap = false;
        this.replace = false;
    }

    /**
     * Getter for new name of the local extension.
     *
     * @return the new name
     */
    @CheckForNull
    public String getNewName() {
        return newName;
    }

    /**
     * Setter for new name of the local extension.
     *
     * @param newName new value for the new name
     */
    public void setNewName(@NullAllowed String newName) {
        this.newName = newName;
    }

    /**
     * Target for the local extension.
     *
     * The target for the local extension consists of the package name in
     * combination with the source root.
     *
     * @see #setSourceRoot
     * @param packageName
     */
    public void setPackageName(@NullAllowed String packageName) {
        this.packageName = packageName;
    }

    /**
     * Target for the local extension.
     *
     * @see #setPackageName
     * @return target
     */
    @CheckForNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * Target for the local extension.
     *
     * The target for the local extension consists of the package name in
     * combination with the source root.
     *
     * @see #setPackageName
     * @param sourceRoot
     */
    public void setSourceRoot(FileObject sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    /**
     * Target for the local extension.
     *
     * @see #setSourceRoot
     * @return target
     */
    @CheckForNull
    public FileObject getSourceRoot() {
        return sourceRoot;
    }

    /**
     * The extension class can be a subclass or a wrapper of the original.
     *
     * When creating a wrapper class, a new class will be created with an
     * instance of the original. It will delegate all methods to the original.
     *
     * @param wrap true if the local extension should be a wrapper class.
     */
    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    /**
     * The extension class can be a subclass or a wrapper of the original.
     *
     * @return true if the local extension should be a wrapper class.
     */
    public boolean getWrap() {
        return wrap;
    }

    /**
     * Set how the equals and hashCode methods should be handled.
     *
     * Only makes sense if the extension is a wrapper class.
     *
     * @see Equality
     * @param equality how the equals and hashcode methods should be handled.
     */
    public void setEquality(@NonNull Equality equality) {
        this.equality = equality;
    }

    /**
     * Get how the equals and hashCode methods will be handled.
     *
     * Only makes sense if the extension is a wrapper class.
     *
     * @see Equality
     * @return how the equals and hashcode methods should be handled.
     */
    public @NonNull
    Equality getEquality() {
        return equality;
    }

    /**
     * Only create the extension class, or should all usages be replaced with
     * the new local extension.
     *
     * @param replace true if usages need to use the new type.
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * Only create the extension class, or should all usages be replaced with
     * the new local extension.
     *
     * @param replace true if usages need to use the new type.
     */
    public boolean getReplace() {
        return replace;
    }

    /**
     * Equality defines the different ways the equals and hashcode methods can
     * be handled.
     *
     * When creating a wrapper class the equals and hashcode methods should be
     * handled. Simply delegating it not always the best option, as the system
     * expects that equals is symmetric.
     */
    public enum Equality {

        /**
         * Delegate to the equals and hashcode methods to the original class.
         *
         * {@code
         * boolean equals(Object o) {
         *     Object target = o;
         *     if(o instanceof THIS) {
         *         target = ((THIS)o).delegate;
         *     }
         * return this.delegate.equals(target);
         * }
         *
         * int hashCode() {
         * return this.delegate.hashCode();
         * }
         * }
         */
        DELEGATE,
        /**
         * Generate new hashcode and equals methods using the editors
         * codegenerator.
         *
         * The field {@code delegate} will be supplied to the equals and
         * hashcode generator.
         */
        GENERATE,
        /**
         * Separate the equals method into two.
         * A new method is added to check if the original class equals the
         * extension class.
         * {@code
         * boolean equals(Object o) {
         *     return this.delegate.equals(o);
         * }
         *
         * boolean equalsSOURCE(THIS o) {
         * return this.delegate.equals(o.delegate);
         * }
         *
         * int hashCode() {
         * return this.delegate.hashCode();
         * }
         * }
         */
        SEPARATE
    }
}
