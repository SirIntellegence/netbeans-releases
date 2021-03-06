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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.project.ui.actions.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.project.PhpActionProvider;
import org.netbeans.modules.php.project.PhpProject;
import org.netbeans.modules.php.project.ProjectPropertiesSupport;
import org.netbeans.modules.php.project.ui.codecoverage.PhpCoverageProvider;
import org.netbeans.modules.php.project.ui.testrunner.ControllableRerunHandler;
import org.netbeans.modules.php.project.ui.testrunner.UnitTestRunner;
import org.netbeans.modules.php.project.util.UsageLogging;
import org.netbeans.modules.php.spi.testing.PhpTestingProvider;
import org.netbeans.modules.php.spi.testing.run.TestRunInfo;
import org.netbeans.modules.web.clientproject.api.jstesting.JsTestingProvider;
import org.netbeans.modules.web.clientproject.api.jstesting.JsTestingProviders;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.Pair;
import org.openide.util.RequestProcessor;

/**
 * Action implementation for TEST configuration.
 * It means running and debugging tests.
 * @author Tomas Mysik
 */
class ConfigActionTest extends ConfigAction {

    private static final RequestProcessor RP = new RequestProcessor(ConfigActionTest.class);


    protected ConfigActionTest(PhpProject project) {
        super(project);
    }

    private PhpCoverageProvider getCoverageProvider() {
        return project.getLookup().lookup(PhpCoverageProvider.class);
    }

    protected List<FileObject> getTestDirectories(boolean showCustomizer) {
        return ProjectPropertiesSupport.getTestDirectories(project, showCustomizer);
    }

    @Override
    public boolean isProjectValid() {
        throw new IllegalStateException("Validation is not needed for tests");
    }

    @Override
    public boolean isFileValid() {
        throw new IllegalStateException("Validation is not needed for tests");
    }

    @Override
    public boolean isDebugProjectEnabled() {
        throw new IllegalStateException("Debug project tests action is not supported");
    }

    @Override
    public boolean isRunFileEnabled(Lookup context) {
        FileObject file = CommandUtils.fileForContextOrSelectedNodes(context);
        return file != null && FileUtils.isPhpFile(file);
    }

    @Override
    public boolean isDebugFileEnabled(Lookup context) {
        if (DebugStarterFactory.getInstance() == null) {
            return false;
        }
        return isRunFileEnabled(context);
    }

    @Override
    public boolean isRunMethodEnabled(Lookup context) {
        SingleMethod singleMethod = CommandUtils.singleMethodForContext(context);
        if (singleMethod == null) {
            return false;
        }
        FileObject file = singleMethod.getFile();
        if (file == null) {
            return false;
        }
        if (!FileUtils.isPhpFile(file)) {
            return false;
        }
        PhpModule phpModule = project.getPhpModule();
        for (PhpTestingProvider testingProvider : project.getTestingProviders()) {
            if (testingProvider.isTestFile(phpModule, file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDebugMethodEnabled(Lookup context) {
        if (DebugStarterFactory.getInstance() == null) {
            return false;
        }
        return isRunMethodEnabled(context);
    }

    @Override
    public void runProject() {
        runJsTests();
        runPhpTests();
    }

    private void runPhpTests() {
        // first, let user select test directory
        List<FileObject> testDirs = getTestDirectories(true);
        if (testDirs.isEmpty()) {
            return;
        }
        TestRunInfo testRunInfo = getTestRunInfoForDirs(testDirs, false);
        assert testRunInfo != null;
        run(testRunInfo);
    }

    protected void runJsTests() {
        final JsTestingProvider jsTestingProvider = JsTestingProviders.getDefault().getJsTestingProvider(project, false);
        if (jsTestingProvider != null) {
            UsageLogging.logJsTestRun(project, jsTestingProvider);
            RP.post(new Runnable() {
                @Override
                public void run() {
                    org.netbeans.modules.web.clientproject.api.jstesting.TestRunInfo testRunInfo
                            = new org.netbeans.modules.web.clientproject.api.jstesting.TestRunInfo.Builder()
                                    .build();
                    jsTestingProvider.runTests(project, testRunInfo);
                }
            });
        }
    }

    @Override
    public void debugProject() {
        throw new IllegalStateException("Debug project tests action is not supported");
    }

    @Override
    public void runFile(final Lookup context) {
        FileObject fileObj = CommandUtils.fileForContextOrSelectedNodes(context);
        assert fileObj != null : "Fileobject not found for context: " + context;
        TestRunInfo testRunInfo = getTestRunInfoForFile(fileObj, false);
        assert testRunInfo != null;
        run(testRunInfo);
    }

    @Override
    public void debugFile(final Lookup context) {
        FileObject fileObj = CommandUtils.fileForContextOrSelectedNodes(context);
        assert fileObj != null : "Fileobject not found for context: " + context;
        TestRunInfo testRunInfo = getTestRunInfoForFile(fileObj, true);
        assert testRunInfo != null;
        run(testRunInfo);
    }

    @Override
    public void runMethod(Lookup context) {
        SingleMethod singleMethod = CommandUtils.singleMethodForContext(context);
        TestRunInfo testRunInfo = getTestRunInfo(singleMethod, false);
        assert testRunInfo != null;
        run(testRunInfo);
    }

    @Override
    public void debugMethod(Lookup context) {
        SingleMethod singleMethod = CommandUtils.singleMethodForContext(context);
        TestRunInfo testRunInfo = getTestRunInfo(singleMethod, true);
        assert testRunInfo != null;
        run(testRunInfo);
    }

    void run(final TestRunInfo testRunInfo) {
        new UnitTestRunner(project, testRunInfo, new RerunUnitTestHandler(testRunInfo))
                .run();
    }

    private TestRunInfo getTestRunInfo(SingleMethod singleMethod, boolean debug) {
        assert singleMethod != null;
        FileObject file = singleMethod.getFile();
        assert file != null;
        TestRunInfo testRunInfo = getTestRunInfoForFile(file, debug);
        assert testRunInfo != null;
        Pair<String, String> method = CommandUtils.decodeMethod(singleMethod.getMethodName());
        TestRunInfo.TestInfo testInfo = new TestRunInfo.TestInfo(TestRunInfo.TestInfo.UNKNOWN_TYPE, method.second(), method.first(),
                FileUtil.toFile(file).getAbsolutePath());
        testRunInfo.setInitialTests(Collections.singleton(testInfo));
        return testRunInfo;
    }

    @CheckForNull
    private TestRunInfo getTestRunInfoForDirs(List<FileObject> dirs, boolean debug) {
        assert dirs != null;
        return new TestRunInfo.Builder()
                .setSessionType(debug ? TestRunInfo.SessionType.DEBUG : TestRunInfo.SessionType.TEST)
                .setStartFiles(getValidFolders(dirs))
                .setCoverageEnabled(getCoverageProvider().isEnabled())
                .build();
    }

    private List<FileObject> getValidFolders(List<FileObject> dirs) {
        List<FileObject> validDirs = new ArrayList<>(dirs.size());
        for (FileObject dir : dirs) {
            assert dir.isFolder() : dir;
            if (dir.isValid()) {
                validDirs.add(dir);
            }
        }
        return validDirs;
    }

    /**
     * Get run info for a file or a folder.
     */
    @CheckForNull
    private TestRunInfo getTestRunInfoForFile(FileObject fileObj, boolean debug) {
        assert fileObj != null;

        if (!fileObj.isValid()) {
            return null;
        }
        final String name;
        if (fileObj.isFolder()) {
            // #195525 - run tests in folder
            name = fileObj.getNameExt();
        } else {
            name = fileObj.getName();
        }
        return new TestRunInfo.Builder()
                .setSessionType(debug ? TestRunInfo.SessionType.DEBUG : TestRunInfo.SessionType.TEST)
                .setStartFile(fileObj)
                .setSuiteName(name)
                .setCoverageEnabled(getCoverageProvider().isEnabled())
                .build();
    }

    //~ Inner classes

    protected final class RerunUnitTestHandler implements ControllableRerunHandler {

        final TestRunInfo info;
        private final ChangeSupport changeSupport = new ChangeSupport(this);

        private volatile boolean enabled = false;


        public RerunUnitTestHandler(TestRunInfo info) {
            assert info != null;
            this.info = info;
        }

        @Override
        public void rerun() {
            info.setRerun(true);
            PhpActionProvider.submitTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        ConfigActionTest.this.run(info);
                    } finally {
                        info.setRerun(false);
                        info.resetCustomTests();
                    }
                }
            });
        }

        @Override
        public void rerun(Set<Testcase> tests) {
            info.setCustomTests(map(tests));
            rerun();
        }

        @Override
        public boolean enabled(RerunType type) {
            boolean supportedType = false;
            switch (type) {
                case ALL:
                case CUSTOM:
                    supportedType = true;
                    break;
                default:
                    assert false : "Unknown RerunType: " + type;
                    break;
            }
            return supportedType && enabled;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            changeSupport.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            changeSupport.removeChangeListener(listener);
        }

        @Override
        public void enable() {
            if (!enabled) {
                enabled = true;
                changeSupport.fireChange();
            }
        }

        @Override
        public void disable() {
            if (enabled) {
                enabled = false;
                changeSupport.fireChange();
            }
        }

        //~ Mappers

        private Collection<TestRunInfo.TestInfo> map(Set<Testcase> tests) {
            Set<TestRunInfo.TestInfo> testCases = new HashSet<>();
            for (Testcase test : tests) {
                testCases.add(new TestRunInfo.TestInfo(test.getType(), test.getName(), test.getClassName(), test.getLocation()));
            }
            return testCases;
        }

    }

}
