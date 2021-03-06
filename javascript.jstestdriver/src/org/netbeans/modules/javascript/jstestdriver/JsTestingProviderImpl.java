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

package org.netbeans.modules.javascript.jstestdriver;

import java.awt.EventQueue;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.javascript.jstestdriver.api.JsTestDriver;
import org.netbeans.modules.javascript.jstestdriver.api.RunTests;
import org.netbeans.modules.javascript.jstestdriver.preferences.JsTestDriverPreferences;
import org.netbeans.modules.javascript.jstestdriver.preferences.JsTestDriverPreferencesValidator;
import org.netbeans.modules.javascript.jstestdriver.ui.customizer.CustomizerPanel;
import org.netbeans.modules.web.clientproject.api.ProjectDirectoriesProvider;
import org.netbeans.modules.web.clientproject.api.jstesting.JsTestingProviders;
import org.netbeans.modules.web.clientproject.api.jstesting.TestRunInfo;
import org.netbeans.modules.web.clientproject.spi.CustomizerPanelImplementation;
import org.netbeans.modules.web.clientproject.spi.jstesting.JsTestingProviderImplementation;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.netbeans.modules.web.common.api.WebUtils;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = JsTestingProviderImplementation.class, path = JsTestingProviders.JS_TESTING_PATH, position = 200)
public class JsTestingProviderImpl implements JsTestingProviderImplementation {

    private static final Logger LOGGER = Logger.getLogger(JsTestingProviderImpl.class.getName());


    @Override
    public String getIdentifier() {
        return "JsTestDriver"; // NOI18N
    }

    @NbBundle.Messages("JsTestingProviderImpl.displayName=JS Test Driver")
    @Override
    public String getDisplayName() {
        return Bundle.JsTestingProviderImpl_displayName();
    }

    @Override
    public boolean isEnabled(Project project) {
        return JsTestDriverPreferences.isEnabled(project);
    }

    @Override
    public boolean isCoverageSupported(Project project) {
        return false;
    }

    @Override
    public void runTests(Project project, TestRunInfo runInfo) {
        assert !EventQueue.isDispatchThread();
        FileObject configFile = getValidConfigFile(project);
        if (configFile == null) {
            LOGGER.log(Level.INFO, "Cannot run tests for \"{0}\" project, no jsTestDriver.conf provided", ProjectUtils.getInformation(project).getName());
            return;
        }
        try {
            RunTests.runAllTests(project, project.getProjectDirectory(), configFile);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "cannot execute tests", t); // NOI18N
        }
    }

    @Override
    public FileObject fromServer(Project project, URL serverUrl) {
        String serverU = WebUtils.urlToString(serverUrl);
        String prefix = JsTestDriver.getServerURL();
        if (!prefix.endsWith("/")) { // NOI18N
            prefix += "/"; // NOI18N
        }
        prefix += "test/"; // NOI18N
        if (!serverU.startsWith(prefix)) {
            return null;
        }
        String projectRelativePath = serverU.substring(prefix.length());
        if (projectRelativePath.isEmpty()) {
            return null;
        }
        try {
            projectRelativePath = URLDecoder.decode(projectRelativePath, "UTF-8"); // NOI18N
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        // try relative project path
        FileObject projectDirectory = project.getProjectDirectory();
        FileObject fileObject = projectDirectory.getFileObject(projectRelativePath);
        if (fileObject != null) {
            return fileObject;
        }
        // try absolute url for tests outside project folder
        FileObject testsFolder = getTestsFolder(project);
        if (testsFolder != null
                && !isUnderneath(projectDirectory, testsFolder)) {
            File file = new File(projectRelativePath);
            if (file.isFile()) {
                return FileUtil.toFileObject(file);
            }
        }
        return null;
    }

    @Override
    public URL toServer(Project project, FileObject projectFile) {
        String prefix = JsTestDriver.getServerURL();
        if (!prefix.endsWith("/")) { // NOI18N
            prefix += "/"; // NOI18N
        }
        prefix += "test/"; // NOI18N
        String relativePath = FileUtil.getRelativePath(project.getProjectDirectory(), projectFile);
        if (relativePath != null) {
            try {
                return new URL(prefix + relativePath);
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        // maybe a test file from outside tests folder
        FileObject testsFolder = getTestsFolder(project);
        if (testsFolder == null) {
            return null;
        }
        if (isUnderneath(testsFolder, projectFile)) {
            // it is project file
            String absolutePath = FileUtil.toFile(projectFile).getAbsolutePath();
            try {
                return new URL(prefix + absolutePath);
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        return null;
    }

    @Override
    public CustomizerPanelImplementation createCustomizerPanel(Project project) {
        return new CustomizerPanel(project);
    }

    @Override
    public void notifyEnabled(Project project, boolean enabled) {
        JsTestDriverPreferences.setEnabled(project, enabled);
    }

    @Override
    public void projectOpened(Project project) {
        // noop
    }

    @Override
    public void projectClosed(Project project) {
        // noop
    }

    @Override
    public NodeList<Node> createNodeList(Project project) {
        return null;
    }

    @NbBundle.Messages({
        "JsTestingProviderImpl.chooser.title=Select jsTestDriver.conf",
        "JsTestingProviderImpl.error.config=Cannot run tests, no jsTestDriver.conf provided.",
    })
    private FileObject getValidConfigFile(Project project) {
        // existing config
        String config = JsTestDriverPreferences.getConfig(project);
        ValidationResult result = new JsTestDriverPreferencesValidator()
                .validateConfig(config)
                .getResult();
        if (result.isFaultless()) {
            File configFile = new File(config);
            FileObject fo = FileUtil.toFileObject(configFile);
            assert fo != null : "FileObject must be found for " + config;
            assert fo.isValid() : "Valid FileObject must be found for " + config;
            return fo;
        } else {
            String message;
            if (result.hasErrors()) {
                message = result.getErrors().get(0).getMessage();
            } else {
                assert result.hasWarnings() : result;
                message = result.getWarnings().get(0).getMessage();
            }
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(message));
        }
        // ask user
        File file = new FileChooserBuilder(JsTestingProviderImpl.class)
                .setTitle(Bundle.JsTestingProviderImpl_chooser_title())
                .setFilesOnly(true)
                .setDefaultWorkingDirectory(FileUtil.toFile(project.getProjectDirectory()))
                .forceUseOfDefaultWorkingDirectory(true)
                .showOpenDialog();
        if (file != null) {
            JsTestDriverPreferences.setConfig(project, file.getAbsolutePath());
            FileObject fo = FileUtil.toFileObject(file);
            assert fo != null : "FileObject must be found for " + file;
            assert fo.isValid() : "Valid FileObject must be found for " + file;
            return fo;
        }
        // no file selected
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(Bundle.JsTestingProviderImpl_error_config()));
        return null;
    }

    @CheckForNull
    private FileObject getTestsFolder(Project project) {
        ProjectDirectoriesProvider directoriesProvider = project.getLookup().lookup(ProjectDirectoriesProvider.class);
        if (directoriesProvider == null) {
            return null;
        }
        return directoriesProvider.getTestDirectory(false);
    }

    private boolean isUnderneath(FileObject root, FileObject folder) {
        return root.equals(folder)
                || FileUtil.isParentOf(root, folder);
    }

}
