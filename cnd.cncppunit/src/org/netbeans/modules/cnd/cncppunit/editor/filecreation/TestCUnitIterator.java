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

package org.netbeans.modules.cnd.cncppunit.editor.filecreation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.cncppunit.codegeneration.CUnitCodeGenerator;
import org.netbeans.modules.cnd.makeproject.api.MakeProjectOptions;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptorProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.Folder;
import org.netbeans.modules.cnd.makeproject.api.configurations.FolderConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.makeproject.api.configurations.LibrariesConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.LibraryItem;
import org.netbeans.modules.cnd.makeproject.api.configurations.LinkerConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfigurationDescriptor;
import org.netbeans.modules.cnd.simpleunit.spi.wizard.AbstractUnitTestIterator;
import org.netbeans.modules.cnd.utils.CndPathUtilitities;
import org.netbeans.modules.cnd.utils.MIMEExtensions;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.CreateFromTemplateHandler;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;

/**
 * @author Nikolay Krasilnikov (http://nnnnnk.name)
 */
public class TestCUnitIterator extends AbstractUnitTestIterator {
    private WizardDescriptor.Panel<WizardDescriptor> targetChooserDescriptorPanel;

    private static final String C_HEADER_MIME_TYPE = "text/x-c/text/x-h"; // NOI18N

    @Override
    public Set<DataObject> instantiate(TemplateWizard wiz) throws IOException {
        Set<DataObject> dataObjects = new HashSet<DataObject>();

        if(getTestFileName() == null || getTestName() == null) {
            return dataObjects;
        }

        Project project = Templates.getProject(wiz);

//        CsmProject csmProject = CsmModelAccessor.getModel().getProject(project);
//        List<CsmFunction> funs = new ArrayList<CsmFunction>();
//        for (CsmOffsetableDeclaration decl : csmProject.getGlobalNamespace().getDeclarations()) {
//            if(CsmKindUtilities.isClass(decl)) {
//                for (CsmMember member : ((CsmClass)decl).getMembers()) {
//                    if(CsmKindUtilities.isMethod(member)) {
//                        funs.add((CsmMethod)member);
//                    }
//                }
//            }
//            if(CsmKindUtilities.isFunction(decl)) {
////                funs.add((CsmFunction)decl);
//            }
//        }
//        wiz.putProperty(CNDUNITTESTFUNCTIONS, funs);

        DataFolder targetFolder = wiz.getTargetFolder();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(CreateFromTemplateHandler.FREE_FILE_EXTENSION, true);

        List<CsmFunction> fs = new ArrayList<CsmFunction>();
        Object listObj = wiz.getProperty(CND_UNITTEST_FUNCTIONS);
        if(listObj instanceof List<?>) {
            List<?> list = (List<?>) listObj;
            for (Object obj : list) {
                if(obj instanceof CsmFunction) {
                    fs.add((CsmFunction)obj);
                }
            }
        }
        params.putAll(CUnitCodeGenerator.generateTemplateParamsForFunctions(
                getTestFileName().replaceFirst("[.].*", ""), // NOI18N
                targetFolder.getPrimaryFile().getPath(),
                fs));
        
        DataObject formDataObject = NewTestCUnitPanel.getTemplateDataObject("cunittestfile.c"); // NOI18N
        
        DataObject dataObject = formDataObject.createFromTemplate(targetFolder, getTestFileName(), params);

        Folder folder = null;
        Folder testsRoot = getTestsRootFolder(project);
        if(testsRoot != null) {
            Folder newFolder = testsRoot.addNewFolder(true, Folder.Kind.TEST);
            newFolder.setDisplayName(getTestName());
            folder = newFolder;
        }

        if(folder == null) {
            return dataObjects;
        }

        setCUnitLinkerOptions(project, folder);

        MakeConfigurationDescriptor makeConfigurationDescriptor = getMakeConfigurationDescriptor(project);

        FileObject file = dataObject.getPrimaryFile();
        Project owner = FileOwnerQuery.getOwner(file);

        if (owner != null && owner.getProjectDirectory() == project.getProjectDirectory()) {
            File ioFile = FileUtil.toFile(file);
            if (ioFile.isDirectory()) {
                return dataObjects;
            } // don't add directories.
            if (!makeConfigurationDescriptor.okToChange()) {
                return dataObjects;
            }
            String itemPath;
            if (MakeProjectOptions.getPathMode() == MakeProjectOptions.REL_OR_ABS) {
                itemPath = CndPathUtilitities.toAbsoluteOrRelativePath(makeConfigurationDescriptor.getBaseDir(), ioFile.getPath());
            } else if (MakeProjectOptions.getPathMode() == MakeProjectOptions.REL) {
                itemPath = CndPathUtilitities.toRelativePath(makeConfigurationDescriptor.getBaseDir(), ioFile.getPath());
            } else {
                itemPath = ioFile.getPath();
            }
            itemPath = CndPathUtilitities.normalize(itemPath);
            Item item = new Item(itemPath);

            folder.addItemAction(item);
        }
        
        dataObjects.add(dataObject);
        return dataObjects;
    }

    @Override
    protected Panel<WizardDescriptor>[] createPanels() {
        if (targetChooserDescriptorPanel == null) {
            TemplateWizard wiz = getWizard();
            DataObject dobj = wiz.getTemplate();
            FileObject fobj = dobj.getPrimaryFile();
            String mimeType = fobj.getMIMEType();
            MIMEExtensions extensions = MIMEExtensions.get(mimeType);
            if (extensions != null) {
                Project project = Templates.getProject(wiz);
                Sources sources = ProjectUtils.getSources(project);
                SourceGroup[] groups = sources.getSourceGroups(Sources.TYPE_GENERIC);
                if (MIMENames.HEADER_MIME_TYPE.equals(extensions.getMIMEType())) {
                    // this is the only place where we want to differ c headers from cpp headers (creation of new one)
                    if (dobj.getPrimaryFile().getAttribute(C_HEADER_MIME_TYPE) != null) {
                        MIMEExtensions cHeaderExtensions = MIMEExtensions.get(C_HEADER_MIME_TYPE);
                        if ((cHeaderExtensions == null) || !C_HEADER_MIME_TYPE.equals(cHeaderExtensions.getMIMEType())) {
                            System.err.println("not found extensions for C Headers"); // NOI18N
                        } else {
                            extensions = cHeaderExtensions;
                        }
                    }
                }
                String defaultExt = null; // let the chooser panel decide default extension
                if (mimeType.equals(MIMENames.SHELL_MIME_TYPE)) {
                    // for shell scripts set default extension explicitly
                    defaultExt = fobj.getExt();
                } else if (mimeType.equals(MIMENames.HEADER_MIME_TYPE) && fobj.getExt().length() == 0) {
                    // for standard header without extension
                    defaultExt = fobj.getExt();
                }

                targetChooserDescriptorPanel = new NewTestCUnitPanel(project, groups, null, extensions, defaultExt);
            } else {
                targetChooserDescriptorPanel = wiz.targetChooser();
            }
        }
        @SuppressWarnings("unchecked")
        Panel<WizardDescriptor>[] panels = new WizardDescriptor.Panel[]{targetChooserDescriptorPanel};
        return panels;
    }

    private String getTestFileName() {
        return ((NewTestCUnitPanelGUI)targetChooserDescriptorPanel.getComponent()).getTestFileName();
    }

    private String getTestName() {
        return ((NewTestCUnitPanelGUI)targetChooserDescriptorPanel.getComponent()).getTestName();
    }

    private void setCUnitLinkerOptions(Project project, Folder testFolder) {
        ConfigurationDescriptorProvider cdp = project.getLookup().lookup(ConfigurationDescriptorProvider.class);
        MakeConfigurationDescriptor projectDescriptor = cdp.getConfigurationDescriptor();
        FolderConfiguration folderConfiguration = testFolder.getFolderConfiguration(projectDescriptor.getActiveConfiguration());
        LinkerConfiguration linkerConfiguration = folderConfiguration.getLinkerConfiguration();
        LibrariesConfiguration librariesConfiguration = linkerConfiguration.getLibrariesConfiguration();
        librariesConfiguration.add(new LibraryItem.StdLibItem("CUnit", "CUnit", new String[]{"cunit"})); // NOI18N
        linkerConfiguration.setLibrariesConfiguration(librariesConfiguration);
    }
}

