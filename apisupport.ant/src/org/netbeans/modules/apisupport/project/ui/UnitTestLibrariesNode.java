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

package org.netbeans.modules.apisupport.project.ui;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.apisupport.project.NbModuleProject;
import org.netbeans.modules.apisupport.project.ProjectXMLManager;
import org.netbeans.modules.apisupport.project.ui.customizer.AddModulePanel;
import org.netbeans.modules.apisupport.project.ui.customizer.EditTestDependencyPanel;
import org.netbeans.modules.apisupport.project.ModuleDependency;
import org.netbeans.modules.apisupport.project.api.UIUtil;
import org.netbeans.modules.apisupport.project.ui.customizer.SingleModuleProperties;
import org.netbeans.modules.apisupport.project.universe.ModuleEntry;
import org.netbeans.modules.apisupport.project.universe.ModuleList;
import org.netbeans.modules.apisupport.project.universe.TestModuleDependency;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.support.ant.AntProjectEvent;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.AntProjectListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.actions.FindAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import static org.netbeans.modules.apisupport.project.ui.Bundle.*;

/**
 * @author Tomas Musil
 */
final class UnitTestLibrariesNode extends AbstractNode {

    private static final Logger LOG = Logger.getLogger(UnitTestLibrariesNode.class.getName());

    private final String testType;
    private final NbModuleProject project;
    private boolean missingJUnit4;
    
    @Messages({
        "LBL_unit_test_libraries=Unit Test Libraries",
        "LBL_qa-functional_test_libraries=Functional Test Libraries"
    })
    public UnitTestLibrariesNode(String testType, final NbModuleProject project) {
        super(new LibrariesChildren(testType, project), org.openide.util.lookup.Lookups.fixed(project));
        this.testType = testType;
        this.project = project;
        setName(testType);
        if (testType.equals("unit")) {
            setDisplayName(LBL_unit_test_libraries());
        } else if (testType.equals("qa-functional")) {
            setDisplayName(LBL_qa_functional_test_libraries());
        }
    }
    
    public @Override Image getIcon(int type) {
        return getIcon(false);
    }
    
    public @Override Image getOpenedIcon(int type) {
        return getIcon(true);
    }

    private void setMissingJUnit4(boolean missingJUnit4) {
        this.missingJUnit4 = missingJUnit4;
        fireIconChange();
        fireOpenedIconChange();
        fireShortDescriptionChange(null, null);
    }
    
    private Image getIcon(boolean opened) {
        Image badge = ImageUtilities.loadImage("org/netbeans/modules/apisupport/project/ui/resources/libraries-badge.png", true);
        Image img = ImageUtilities.mergeImages(ApisupportAntUIUtils.getTreeFolderIcon(opened), badge, 8, 8);
        if (missingJUnit4) {
            badge = ImageUtilities.loadImage("org/netbeans/modules/java/api/common/project/ui/resources/brokenProjectBadge.gif", true);
            img = ImageUtilities.mergeImages(img, badge, 0, 0);
        }
        return img;
    }
    
    public @Override Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<Action>(2);
        if (missingJUnit4) {
            actions.add(new AddJUnit4Action(testType, project));
        }
        actions.add(new AddUnitTestDependencyAction(testType, project));
        return actions.toArray(new Action[actions.size()]);
    }

    @Messages("HINT_missing_junit4=Incomplete test libraries. Use context menu to resolve.")
    public @Override String getShortDescription() {
        if (missingJUnit4) {
            return HINT_missing_junit4();
        } else {
            return null;
        }
    }
    
    @Messages({
        "CTL_test=Include also tests of module",
        "CTL_compile=Compile-time dependency",
        "CTL_recursive=Include dependencies recursively"
    })
    private static String createHtmlDescription(final TestModuleDependency dep) {
        StringBuilder shortDesc = new StringBuilder("<html><u>" + dep.getModule().getCodeNameBase() + "</u><br>"); // NOI18N
        if (dep.isTest()) {
            shortDesc.append("<br>").append(CTL_test());
        }
        if (dep.isCompile()) {
            shortDesc.append("<br>").append(CTL_compile());
        }
        if (dep.isRecursive()) {
            shortDesc.append("<br>").append(CTL_recursive());
        }
        shortDesc.append("</html>"); // NOI18N
        return shortDesc.toString();
    }
    
    private static final class LibrariesChildren extends Children.Keys<TestModuleDependency> implements AntProjectListener {
        
        static final Action REMOVE_DEPENDENCY_ACTION = new RemoveDependencyAction();
        
        private final String testType;
        private final NbModuleProject project;
        
        private ImageIcon librariesIcon;
        
        LibrariesChildren(String testType, final NbModuleProject project) {
            this.testType = testType;
            this.project = project;
        }
        
        protected @Override void addNotify() {
            super.addNotify();
            project.getHelper().addAntProjectListener(this);
            refreshKeys();
        }
        
        protected @Override void removeNotify() {
            setKeys(Collections.<TestModuleDependency>emptySet());
            project.getHelper().removeAntProjectListener(this);
            super.removeNotify();
        }
        
        private void refreshKeys() {
            try {
                ProjectManager.mutex().readAccess(new Mutex.ExceptionAction<Object>() {
                    public @Override Object run() throws Exception {
                        final Collection<TestModuleDependency> deps = new TreeSet<TestModuleDependency>(TestModuleDependency.CNB_COMPARATOR);
                        final AtomicBoolean missingJUnit4 = new AtomicBoolean(true);
                        Set<TestModuleDependency> tmds = new ProjectXMLManager(project).getTestDependencies(project.getModuleList()).get(testType);
                        if (tmds != null) { // will be null if have no <test-dependencies> of this type
                            for (TestModuleDependency tmd : tmds) {
                                deps.add(tmd);
                                if (tmd.getModule().getCodeNameBase().equals("org.netbeans.libs.junit4")) { // NOI18N
                                    missingJUnit4.set(false);
                                }
                            }
                        }
                        ImportantFilesNodeFactory.getNodesSyncRP().post(new Runnable() {
                            public @Override void run() {
                                ((UnitTestLibrariesNode) getNode()).setMissingJUnit4(missingJUnit4.get());
                                setKeys(deps);
                            }
                        });
                        return null;
                    }
                });
            } catch (MutexException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
        
        protected @Override Node[] createNodes(TestModuleDependency dep) {
            Node node = null;
            File srcF = dep.getModule().getSourceLocation();
            if (srcF == null) {
                File jarF = dep.getModule().getJarLocation();
                URL jarRootURL = FileUtil.urlForArchiveOrDir(jarF);
                assert jarRootURL != null;
                ModuleEntry me = dep.getModule();
                String name = me.getLocalizedName() + " - " + me.getCodeNameBase(); // NOI18N
                Icon icon = getLibrariesIcon();
                FileObject root = URLMapper.findFileObject(jarRootURL);
                if (root == null) {
                    LOG.log(Level.WARNING, "#195341: no FO for {0}", jarRootURL);
                    root = FileUtil.createMemoryFileSystem().getRoot();
                }
                Node pvNode = ActionFilterNode.create(
                        PackageView.createPackageView(new LibrariesSourceGroup(root, name, icon, icon)));
                node = new LibraryDependencyNode(dep, testType, project, pvNode);
                node.setName(me.getLocalizedName());
            } else {
                node = new ProjectDependencyNode(dep, testType, project);
                node.setName(dep.getModule().getLocalizedName());
            }
            assert node != null;
            return new Node[] { node };
        }

        private boolean refreshScheduled = false;
        public @Override void configurationXmlChanged(AntProjectEvent ev) {
            // XXX this is a little strange but happens during project move. Bad ordering.
            // Probably bug in moving implementation (our or in general Project API).
            if (project.getHelper().resolveFileObject(AntProjectHelper.PROJECT_XML_PATH) != null) {
                Runnable r = new Runnable() {
                    public @Override void run() {
                        refreshKeys();
                        refreshScheduled = false;
                    }
                };
                if (project.isRunInAtomicAction()) {
                    if (! refreshScheduled) {
                        refreshScheduled = true;
                        EventQueue.invokeLater(r);
                    }
                } else {
                    r.run();
                }
            }
        }
        
        public @Override void propertiesChanged(AntProjectEvent ev) {
            // do not need
            LOG.log(Level.FINE, "propertiesChanged: {0}, expected: {1}", new Object[] {ev.getPath(), ev.isExpected()});
        }
        
        
        private Icon getLibrariesIcon() {
            if (librariesIcon == null) {
                librariesIcon = ImageUtilities.loadImageIcon(UIUtil.LIBRARIES_ICON, true);
            }
            return librariesIcon;
        }
        
    }
    
    private static final class ProjectDependencyNode extends AbstractNode {
        
        private final TestModuleDependency dep;
        private final String testType;
        private final NbModuleProject project;
        private Action[] actions;
        
        ProjectDependencyNode(final TestModuleDependency dep, String testType, final NbModuleProject project) {
            super(Children.LEAF, Lookups.fixed(new Object[] { dep, project, dep.getModule(), testType}));
            this.dep = dep;
            this.testType = testType;
            this.project = project;
            ModuleEntry me = dep.getModule();
            setIconBaseWithExtension(NbModuleProject.NB_PROJECT_ICON_PATH);
            setDisplayName(me.getLocalizedName());
            setShortDescription(UnitTestLibrariesNode.createHtmlDescription(dep));
        }
        
        public @Override Action[] getActions(boolean context) {
            
            if (actions == null) {
                Set<Action> result = new LinkedHashSet<Action>();
                // Open project action
                result.add(SystemAction.get(LibrariesNode.OpenProjectAction.class));
                // Edit dependency action
                result.add(new EditTestDependencyAction(dep, testType, project));
                // Remove dependency
                result.add(LibrariesChildren.REMOVE_DEPENDENCY_ACTION);
                actions = result.toArray(new Action[result.size()]);
            }
            return actions;
        }
        
        public @Override Action getPreferredAction() {
            return getActions(false)[0]; // open
        }
        
    }
    
    private static final class LibraryDependencyNode extends FilterNode {
        
        private final TestModuleDependency dep;
        private final String testType;
        private final NbModuleProject project;
        private Action[] actions;
        
        LibraryDependencyNode(final TestModuleDependency dep,
                String testType,
                final NbModuleProject project, final Node original) {
            super(original, null, new ProxyLookup(new Lookup[] {
                original.getLookup(),
                Lookups.fixed(new Object[] { dep, project, testType })
            }));
            this.dep = dep;
            this.testType = testType;
            this.project = project;
            setShortDescription(UnitTestLibrariesNode.createHtmlDescription(dep));
        }
        
        public @Override Action[] getActions(boolean context) {
            if (actions == null) {
                Set<Action> result = new LinkedHashSet<Action>();
                result.add(new EditTestDependencyAction(dep, testType, project));
                Action[] superActions = super.getActions(false);
                for (int i = 0; i < superActions.length; i++) {
                    if (superActions[i] instanceof FindAction) {
                        result.add(superActions[i]);
                    }
                }
                result.add(LibrariesChildren.REMOVE_DEPENDENCY_ACTION);
                actions = result.toArray(new Action[result.size()]);
            }
            return actions;
        }
        
        public @Override Action getPreferredAction() {
            return getActions(false)[0];
        }
        
    }
    
    static final class AddUnitTestDependencyAction extends AbstractAction {
        
        private final String testType;
        private final NbModuleProject project;
        
        @Messages({
            "CTL_AddTestDependency_unit=Add Unit Test Dependency",
            "CTL_AddTestDependency_qa-functional=Add Functional Test Dependency"
        })
        AddUnitTestDependencyAction(String testType, final NbModuleProject project) {
            if (testType.equals("unit")) {
                putValue(NAME, CTL_AddTestDependency_unit());
            } else if (testType.equals("qa-functional")) {
                putValue(NAME, CTL_AddTestDependency_qa_functional());
            }
            this.testType = testType;
            this.project = project;
        }
        
        //COPIED FROM LIBRARIES MOSTLY
        public @Override void actionPerformed(ActionEvent ev) {
            SingleModuleProperties props = SingleModuleProperties.getInstance(project);
            final AddModulePanel addPanel = new AddModulePanel(props);
            String title = testType.equals("unit") ? CTL_AddTestDependency_unit() : CTL_AddTestDependency_qa_functional();
            final DialogDescriptor descriptor = new DialogDescriptor(addPanel, title);
            descriptor.setHelpCtx(new HelpCtx(AddModulePanel.class));
            descriptor.setClosingOptions(new Object[0]);
            final Dialog d = DialogDisplayer.getDefault().createDialog(descriptor);
            descriptor.setButtonListener(new ActionListener() {
                public @Override void actionPerformed(ActionEvent e) {
                    if (DialogDescriptor.OK_OPTION.equals(e.getSource()) &&
                            addPanel.getSelectedDependencies().length == 0) {
                        return;
                    }
                    d.setVisible(false);
                    d.dispose();
                }
            });
            d.setVisible(true);
            if (descriptor.getValue().equals(DialogDescriptor.OK_OPTION)) {
                // dialog returns
                ModuleDependency[] newDeps = addPanel.getSelectedDependencies();
                ProjectXMLManager pxm = new ProjectXMLManager(project);
                try {
                    for (int i = 0; i < newDeps.length; i++) {
                        // by default, add compile-time dependency
                        pxm.addTestDependency(testType
                                ,new TestModuleDependency(newDeps[i].getModuleEntry(), false, false, true));
                        ProjectManager.getDefault().saveProject(project);
                    }
                } catch (Exception e) {
                    Exceptions.attachMessage(e, "Cannot add dependencies, probably IO error: " + Arrays.asList(newDeps)); // NOI18N
                    Exceptions.printStackTrace(e);
                }
            }
            d.dispose();
        }
        
    }

    private static class AddJUnit4Action extends AbstractAction {
        private static final RequestProcessor RP = new RequestProcessor(AddJUnit4Action.class);
        private final String testType;
        private final NbModuleProject project;
        @Messages("LBL_resolve_missing_junit4=Add Missing Dependencies")
        AddJUnit4Action(String testType, NbModuleProject project) {
            super(LBL_resolve_missing_junit4());
            this.testType = testType;
            this.project = project;
        }
        @Messages({
            "LBL_also_add_nbjunit_question=Adding JUnit 4. Also add NB JUnit (and dependencies)?",
            "LBL_also_add_nbjunit_title=Adding Missing Dependencies",
            "LBL_cannot_resolve_missing_junit4=JUnit 4 and/or NB JUnit test libraries cannot be found in platform."
        })
        public @Override void actionPerformed(ActionEvent e) {
            Object result = DialogDisplayer.getDefault().notify(new NotifyDescriptor.Confirmation(
                    LBL_also_add_nbjunit_question(),
                    LBL_also_add_nbjunit_title()));
            final boolean addNBJUnit;
            if (result == NotifyDescriptor.NO_OPTION) {
                addNBJUnit = false;
            } else if (result == NotifyDescriptor.YES_OPTION) {
                addNBJUnit = true;
            } else {
                return; // cancelled
            }
            ModuleList moduleList;
            try {
                moduleList = project.getModuleList();
            } catch (IOException x) {
                LOG.log(Level.INFO, null, x);
                return;
            }
            try {
                resolveJUnitDependencies(project, moduleList, testType, addNBJUnit);
            } catch (IOException ex) {
                String msg = Exceptions.findLocalizedMessage(ex);
                if (msg == null) {
                    msg = LBL_cannot_resolve_missing_junit4();
                }
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                return;
            }
            try {
                ProjectManager.getDefault().saveProject(project);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        private static final String JUNIT_MODULE = "org.netbeans.libs.junit4";
        private static final String NBJUNIT_MODULE = "org.netbeans.modules.nbjunit";
        @Messages({
            "ERR_could_not_find_junit4=Could not find the JUnit 4 library in the target platform; you need to install JUnit.",
            "ERR_could_not_find_nbjunit=Could not find the NB JUnit library in the target platform; perhaps the harness cluster is missing?"
        })
        private static void resolveJUnitDependencies(NbModuleProject project, ModuleList moduleList, String testType, boolean addNBJUnit) throws IOException {
            ModuleEntry junit4 = moduleList.getEntry(JUNIT_MODULE);
            if (junit4 == null) {
                IOException e = new IOException("no libs.junit4");
                Exceptions.attachLocalizedMessage(e, ERR_could_not_find_junit4());
                throw e;
            }
            ModuleEntry nbjunit = moduleList.getEntry(NBJUNIT_MODULE);
            if (addNBJUnit && nbjunit == null) {
                IOException e = new IOException("no nbjunit");
                Exceptions.attachLocalizedMessage(e, ERR_could_not_find_nbjunit());
                throw e;
            }
            ProjectXMLManager pxm = new ProjectXMLManager(project);
            pxm.addTestDependency(testType, new TestModuleDependency(junit4, false, false, true));
            if (addNBJUnit) {
                pxm.addTestDependency(testType, new TestModuleDependency(nbjunit, false, true, true));
            }
        }
    }

    private static final class Pair<F, S> {
        public F first;
        public S second;
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
    
    static final class RemoveDependencyAction extends CookieAction {
        protected @Override void performAction(Node[] activatedNodes) {
            Map<NbModuleProject, Set<Pair<TestModuleDependency, String>>> map =
                    new HashMap<NbModuleProject, Set<Pair<TestModuleDependency, String>>>();
            for (int i = 0; i < activatedNodes.length; i++) {
                Lookup lkp = activatedNodes[i].getLookup();
                TestModuleDependency dep = lkp.lookup(TestModuleDependency.class);
                assert dep != null;
                NbModuleProject project = lkp.lookup(NbModuleProject.class);
                assert project != null;
                String testType = lkp.lookup(String.class);
                assert testType != null;
                Set<Pair<TestModuleDependency, String>> deps = map.get(project);
                if (deps == null) {
                    deps = new HashSet<Pair<TestModuleDependency, String>>();
                    map.put(project, deps);
                }
                deps.add(new Pair<TestModuleDependency, String>(dep, testType));
            }
            for (Map.Entry<NbModuleProject,Set<Pair<TestModuleDependency, String>>> me : map.entrySet()) {
                NbModuleProject project = me.getKey();
                ProjectXMLManager pxm = new ProjectXMLManager(project);
                //remove dep one by one
                for (Pair<TestModuleDependency, String> pair : me.getValue()) {
                    pxm.removeTestDependency(pair.second, pair.first.getModule().getCodeNameBase());
                }
                try {
                    ProjectManager.getDefault().saveProject(project);
                } catch (IOException e) {
                    Exceptions.attachMessage(e, "Problem during test dependencies removing"); // NOI18N
                    Exceptions.printStackTrace(e);
                }
            }
            
        }
        
        @Messages("CTL_RemoveDependency=Remove")
        public @Override String getName() {
            return CTL_RemoveDependency();
        }
        
        public @Override HelpCtx getHelpCtx() {
            return HelpCtx.DEFAULT_HELP;
        }
        
        protected @Override boolean asynchronous() {
            return false;
        }
        
        protected @Override int mode() {
            return CookieAction.MODE_ALL;
        }
        
        protected @Override Class<?>[] cookieClasses() {
            return new Class<?>[] {TestModuleDependency.class, NbModuleProject.class};
        }
        
        
    }
    
    
    static final class EditTestDependencyAction extends AbstractAction {
        
        private final TestModuleDependency testDep;
        private final String testType;
        private final NbModuleProject project;
        
        @Messages("CTL_EditDependency=Edit...")
        EditTestDependencyAction(final TestModuleDependency testDep, String testType, final NbModuleProject project) {
            super(CTL_EditDependency());
            this.testDep = testDep;
            this.testType = testType;
            this.project = project;
        }
        
        
        @Messages("CTL_EditModuleDependencyTitle=Edit \"{0}\" Dependency")
        public @Override void actionPerformed(ActionEvent ev) {
            final EditTestDependencyPanel editTestPanel = new EditTestDependencyPanel(testDep);
            DialogDescriptor descriptor = new DialogDescriptor(editTestPanel,
                    CTL_EditModuleDependencyTitle(testDep.getModule().getLocalizedName()));
            descriptor.setHelpCtx(new HelpCtx(EditTestDependencyPanel.class));
            Dialog d = DialogDisplayer.getDefault().createDialog(descriptor);
            d.setVisible(true);
            if (descriptor.getValue().equals(DialogDescriptor.OK_OPTION)) {
                TestModuleDependency editedDep = editTestPanel.getEditedDependency();
                try {
                    ProjectXMLManager pxm = new ProjectXMLManager(project);
                    pxm.removeTestDependency(testType, testDep.getModule().getCodeNameBase());
                    pxm.addTestDependency(testType, editedDep);
                    ProjectManager.getDefault().saveProject(project);
                    
                    
                } catch (IOException e) {
                    Exceptions.attachMessage(e, "Cannot store dependency: " + editedDep); // NOI18N
                    Exceptions.printStackTrace(e);
                }
                
                
            }
            d.dispose();
        }
    }
    
}

