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

package org.netbeans.modules.projectimport.jbuilder;
import java.io.IOException;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.projectimport.j2seimport.ui.BasicWizardIterator;
import org.netbeans.modules.projectimport.j2seimport.ui.ProgressPanel;
import org.netbeans.modules.projectimport.j2seimport.ui.WarningMessage;
import org.netbeans.modules.projectimport.jbuilder.ui.JBWizardData;
import org.netbeans.modules.projectimport.jbuilder.ui.JBuilderWizardIterator;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.netbeans.modules.projectimport.j2seimport.ImportProcess;
import org.netbeans.modules.projectimport.j2seimport.ImportUtils;
import org.netbeans.modules.projectimport.j2seimport.ui.WizardSupport;

/**
 * Runs JBuilder Importer.
 *
 * @author Radek Matous
 */
public class ImportAction extends CallableSystemAction {
    private BasicWizardIterator wizardIterator;    
    public ImportAction() {
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
    }
    
    public void performAction() {
        try {
            JBWizardData wizardData = showWizard();
            if (wizardData != null) {
                performImport(wizardData);
            }
        } catch (Throwable thr) {
            ErrorManager.getDefault().notify(thr);
        }
    }

    private JBWizardData showWizard() {
        if (wizardIterator == null) {
            wizardIterator = JBuilderWizardIterator.createIterator();                
        }
        wizardIterator.setData(new JBWizardData()); 
        return (JBWizardData)WizardSupport.show(wizardIterator);
    }

    private void performImport(final JBWizardData wizardData) throws IOException {
        ImportProcess iProcess;
        FileObject prjDir = FileUtil.createFolder(wizardData.getDestinationDir());
        assert prjDir != null;

        iProcess = ImportUtils.createImportProcess(prjDir,wizardData.getProjectDefinition(),
                wizardData.isIncludeDependencies());

        ProgressPanel.showProgress(iProcess);
        WarningMessage.showMessages(iProcess);
        OpenProjects.getDefault().open(iProcess.getProjectsToOpen(), true);
    }
    
    
    public String getName() {
        return NbBundle.getMessage(ImportAction.class, "CTL_MenuItem"); // NOI18N
    }
    
    public HelpCtx getHelpCtx() {
        return null;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}
