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
package org.netbeans.modules.web.clientproject.validation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * Validator for project running.
 */
public final class RunProjectValidator {

    public static final String START_FILE = "start.file"; // NOI18N
    public static final String PROJECT_URL = "project.url"; // NOI18N


    public ValidationResult validate(File siteRootFolder, File startFile, String projectUrl) {
        ValidationResult result = new ProjectFoldersValidator().validateSiteRootFolder(siteRootFolder);
        validateStartFile(result, siteRootFolder, startFile);
        validateProjectUrl(result, projectUrl);
        return result;
    }

    @NbBundle.Messages({
        "RunProjectValidator.error.startFile.invalid=Start File must be a valid file.",
        "RunProjectValidator.error.startFile.notUnderSiteRoot=Start File must be underneath Site Root directory."
    })
    private void validateStartFile(ValidationResult result, File siteRootFolder, File startFile) {
        if (startFile == null || !startFile.isFile()) {
            result.addError(new ValidationResult.Message(START_FILE, Bundle.RunProjectValidator_error_startFile_invalid()));
            return;
        }
        if (!FileUtil.isParentOf(FileUtil.toFileObject(siteRootFolder), FileUtil.toFileObject(startFile))) {
            result.addError(new ValidationResult.Message(START_FILE, Bundle.RunProjectValidator_error_startFile_notUnderSiteRoot()));
        }
    }

    @NbBundle.Messages({
        "RunProjectValidator.error.projectUrl.missing=Project URL is missing.",
        "RunProjectValidator.error.projectUrl.invalidProtocol=Project URL must start with http(s):// or file://.",
        "RunProjectValidator.error.projectUrl.invalid=Project URL is invalid."
    })
    private void validateProjectUrl(ValidationResult result, String projectUrl) {
//        if (!jProjectURLTextField.isVisible()) {
//            return null;
//        }
        if (projectUrl == null || projectUrl.isEmpty()) {
            result.addError(new ValidationResult.Message(PROJECT_URL, Bundle.RunProjectValidator_error_projectUrl_missing()));
            return;
        }
        if (!projectUrl.startsWith("http://") // NOI18N
                && !projectUrl.startsWith("https://") // NOI18N
                && !projectUrl.startsWith("file://")) { // NOI18N
            result.addError(new ValidationResult.Message(PROJECT_URL, Bundle.RunProjectValidator_error_projectUrl_invalidProtocol()));
            return;
        }
        try {
            URL url = new URL(projectUrl);
            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                result.addError(new ValidationResult.Message(PROJECT_URL, Bundle.RunProjectValidator_error_projectUrl_invalid()));
            }
        } catch (MalformedURLException ex) {
            result.addError(new ValidationResult.Message(PROJECT_URL, Bundle.RunProjectValidator_error_projectUrl_invalid()));
        }
    }

}
