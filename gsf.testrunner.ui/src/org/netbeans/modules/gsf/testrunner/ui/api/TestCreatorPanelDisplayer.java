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
package org.netbeans.modules.gsf.testrunner.ui.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.netbeans.api.actions.Savable;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider;
import org.netbeans.modules.gsf.testrunner.ui.CommonTestsCfgOfCreate;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.LifecycleManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.RequestProcessor;

public final class TestCreatorPanelDisplayer {

    private static final TestCreatorPanelDisplayer INSTANCE = new TestCreatorPanelDisplayer();
    private static final RequestProcessor RP = new RequestProcessor(TestCreatorPanelDisplayer.class);

    private TestCreatorPanelDisplayer() {}
    /**
     * Get the default <code>TestCreatorPanelDisplayer</code>
     * @return the default instance
     */
    public static TestCreatorPanelDisplayer getDefault() {
        return INSTANCE;
    }

    public void displayPanel(FileObject[] activatedFOs, Object location, String testingFramework) {
//        TODO - replace this with new parsing.api from tzezula...
//	final DataObject[] modified = DataObject.getRegistry().getModified();
        CommonTestsCfgOfCreate cfg = new CommonTestsCfgOfCreate(activatedFOs);
        boolean isJ2MEProject = isJ2MEProject(activatedFOs);
	cfg.createCfgPanel(false, isJ2MEProject);

	ArrayList<String> testingFrameworks = new ArrayList<String>();
	Collection<? extends Lookup.Item<TestCreatorProvider>> providers = Lookup.getDefault().lookupResult(TestCreatorProvider.class).allItems();
        if (!isJ2MEProject) {
            for (Lookup.Item<TestCreatorProvider> provider : providers) {
                testingFrameworks.add(provider.getDisplayName());
            }
        }
	cfg.addTestingFrameworks(testingFrameworks);
	cfg.setPreselectedLocation(location);
	cfg.setPreselectedFramework(testingFramework);
	if (!cfg.configure()) {
	    return;
	}
//	saveAll(modified); // #149048
	String selected = cfg.getSelectedTestingFramework();

	for (final Lookup.Item<TestCreatorProvider> provider : providers) {
	    if (provider.getDisplayName().equals(selected)) {
		final TestCreatorProvider.Context context = new TestCreatorProvider.Context(activatedFOs);
		context.setSingleClass(cfg.isSingleClass());
		context.setTargetFolder(cfg.getTargetFolder());
		context.setTestClassName(cfg.getTestClassName());
                context.setIntegrationTests(cfg.isIntegrationTests());
                final Collection<? extends SourceGroup> createdSourceRoots = cfg.getCreatedSourceRoots();
                RP.execute(new Runnable() {
                    @Override
                    public void run() {
                        //Todo: display some progress
                        for (SourceGroup sg : createdSourceRoots) {
                            IndexingManager.getDefault().refreshIndexAndWait(sg.getRootFolder().toURL(), null);
                        }
                        Mutex.EVENT.readAccess(new Runnable() {
                            @Override
                            public void run() {
                                provider.getInstance().createTests(context);
                            }
                        });
                    }
                });
		cfg = null;
		break;
	    }
	}
    }

    private boolean isJ2MEProject(FileObject[] activatedFOs) {
        FileObject fileObject = activatedFOs[0];
        if (fileObject != null) {
            Project p = FileOwnerQuery.getOwner(fileObject);
            if (p != null) {
                return p.getLookup().lookup(TestCreatorPanelDisplayerProjectServiceProvider.class) != null;
            }
        }
        return false;
    }

    @ProjectServiceProvider(service = TestCreatorPanelDisplayerProjectServiceProvider.class, projectType = "org-netbeans-modules-mobility-project")
    public static class TestCreatorPanelDisplayerProjectServiceProvider {

        public TestCreatorPanelDisplayerProjectServiceProvider(Project p) {
        }
    }

    private void saveAll(Node[] activatedNodes) {
        for(Node node : activatedNodes) {
            SaveCookie saveCookie = node.getLookup().lookup(SaveCookie.class);
            System.out.println("node="+node.getDisplayName()+", saveCookie="+saveCookie);
            if (saveCookie != null) {
                try {
                    saveCookie.save();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
}