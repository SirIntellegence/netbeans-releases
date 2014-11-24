/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject.createprojectapi;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.filesystems.FileObject;
import org.openide.util.Parameters;

/**
 * Project properties. It contains usually relative paths of:
 * <ul>
 * <li>Source folder</li>
 * <li>Site Root folder</li>
 * <li>Unit Tests folder</li>
 * <li>Config folder</li>
 * </ul>
 * And additionally basic project information:
 * <ul>
 * <li>Project name</li>
 * <li>Project directory</li>
 * </ul>
 *
 * <p>
 * This class is not thread-safe.
 *
 * @since 1.37
 * @author Martin Janicek
 */
public final class CreateProjectProperties {

    private FileObject projectDir;
    private String projectName;
    private String sourceFolder;
    private String siteRootFolder;
    private String testFolder;
    private String jsTestingProvider;
    private String platformProvider;
    private boolean autoconfigured = false;


    /**
     * Get project directory, usually relative path.
     *
     * @return project directory, usually relative path; never returns {@code null} or empty string
     * @since 1.37
     */
    @NonNull
    public FileObject getProjectDir() {
        return projectDir;
    }

    /**
     * Set project directory, never {@code null}.
     *
     * @param projectDir project directory, never {@code null}
     * @return itself
     * @since 1.37
     */
    @NonNull
    public CreateProjectProperties setProjectDir(@NonNull FileObject projectDir) {
        Parameters.notNull("projectDir", projectDir); // NOI18N
        this.projectDir = projectDir;
        return this;
    }

    /**
     * Get project name.
     *
     * @return project name; never returns {@code null} or empty string
     * @since 1.37
     */
    @NonNull
    public String getProjectName() {
        return projectName;
    }

    /**
     * Set project name, never {@code null} or empty string.
     *
     * @param projectName project name, never {@code null} or empty string
     * @return itself
     * @since 1.37
     */
    @NonNull
    public CreateProjectProperties setProjectName(@NonNull String projectName) {
        Parameters.notEmpty("projectName", projectName); // NOI18N
        this.projectName = projectName;
        return this;
    }

    /**
     * Get Source folder, usually relative path.
     *
     * @return Source folder, usually relative path; can be {@code null} if no Source folder is present
     * @since 1.61
     */
    @CheckForNull
    public String getSourceFolder() {
        return sourceFolder;
    }

    /**
     * Set Source folder, usually relative path or empty string.
     *
     * @param sourceFolder Source folder, can be {@code null} if no Source folder is present
     * @return itself
     * @since 1.61
     */
    @NonNull
    public CreateProjectProperties setSourceFolder(@NullAllowed String sourceFolder) {
        this.sourceFolder = sourceFolder;
        return this;
    }

    /**
     * Get Site Root folder, usually relative path.
     *
     * @return Site Root folder, usually relative path; can be {@code null} if no Site Root is present
     * @since 1.37
     */
    @CheckForNull
    public String getSiteRootFolder() {
        return siteRootFolder;
    }

    /**
     * Set Site Root folder, usually relative path or empty string.
     *
     * @param siteRootFolder Site Root folder, can be {@code null} if no Site Root is present
     * @return itself
     * @since 1.37
     */
    @NonNull
    public CreateProjectProperties setSiteRootFolder(@NullAllowed String siteRootFolder) {
        this.siteRootFolder = siteRootFolder;
        return this;
    }

    /**
     * Get Test folder, usually relative path.
     *
     * @return Test folder, usually relative path; can be {@code null} if no Test folder is present
     * @since 1.37
     */
    @CheckForNull
    public String getTestFolder() {
        return testFolder;
    }

    /**
     * Set Test folder, can be {@code null} if there are no tests available.
     *
     * @param testFolder Test folder, can be {@code null} if there is no test folder available
     * @return itself
     * @since 1.37
     */
    @NonNull
    public CreateProjectProperties setTestFolder(@NullAllowed String testFolder) {
        this.testFolder = testFolder;
        return this;
    }

    /**
     * Get JS testing provider (its identifier).
     * @return JS testing provider (its identifier); can be {@code null} if there is no default JS testing provider
     * @since 1.50
     */
    @CheckForNull
    public String getJsTestingProvider() {
        return jsTestingProvider;
    }

    /**
     * Set JS testing provider (its identifier), can be {@code null} if there is no default JS testing provider.
     * @param jsTestingProvider JS testing provider (its identifier), can be {@code null} if there is no default JS testing provider
     * @return itself
     * @since 1.50
     */
    public CreateProjectProperties setJsTestingProvider(@NullAllowed String jsTestingProvider) {
        this.jsTestingProvider = jsTestingProvider;
        return this;
    }

    /**
     * Get platform provider (its identifier).
     * @return platform provider (its identifier); can be {@code null} if there is no default platform provider
     * @since 1.62
     */
    @CheckForNull
    public String getPlatformProvider() {
        return platformProvider;
    }

    /**
     * Set platform provider (its identifier), can be {@code null} if there is no default platform provider.
     * @param platformProvider platform provider (its identifier), can be {@code null} if there is no default platform provider
     * @return itself
     * @since 1.62
     */
    public CreateProjectProperties setPlatformProvider(@NullAllowed String platformProvider) {
        this.platformProvider = platformProvider;
        return this;
    }

    /**
     * Return {@code true} if this project was automatically configured.
     * <p>
     * Such project shows notification on its first open.
     * @return {@code true} if this project was automatically configured
     * @since 1.72
     */
    public boolean isAutoconfigured() {
        return autoconfigured;
    }

    /**
     * Set {@code true} if the project is automatically configured.
     * <p>
     * Such project shows notification on its first open.
     * @param autoconfigured {@code true} if the project is automatically configured
     * @return itself
     * @since 1.72
     */
    public CreateProjectProperties setAutoconfigured(boolean autoconfigured) {
        this.autoconfigured = autoconfigured;
        return this;
    }

}
