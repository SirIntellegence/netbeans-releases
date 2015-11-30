/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.docker.ui.rename;

import java.awt.Dialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.docker.api.DockerContainer;
import org.netbeans.modules.docker.api.DockerException;
import org.netbeans.modules.docker.api.DockerAction;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author Petr Hejl
 */
public class RenameContainerAction extends NodeAction {

    private static final Logger LOGGER = Logger.getLogger(RenameContainerAction.class.getName());

    @NbBundle.Messages({
        "LBL_Rename=&Rename",
        "# {0} - container name",
        "LBL_RenameContainer=Rename {0}"
    })
    @Override
    protected void performAction(Node[] activatedNodes) {
        DockerContainer container = activatedNodes[0].getLookup().lookup(DockerContainer.class);
        if (container != null) {
            JButton rename = new JButton();
            Mnemonics.setLocalizedText(rename, Bundle.LBL_Rename());
            RenamePanel panel = new RenamePanel();

            DialogDescriptor descriptor
                    = new DialogDescriptor(panel, Bundle.LBL_RenameContainer(container.getName()),
                            true, new Object[] {rename, DialogDescriptor.CANCEL_OPTION}, rename,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
            descriptor.setClosingOptions(new Object[] {rename, DialogDescriptor.CANCEL_OPTION});
            panel.setMessageLine(descriptor.createNotificationLineSupport());
            Dialog dlg = null;

            try {
                dlg = DialogDisplayer.getDefault().createDialog(descriptor);
                dlg.setVisible(true);

                if (descriptor.getValue() == rename) {
                    perform(container, panel.getContainerName());
                }
            } finally {
                if (dlg != null) {
                    dlg.dispose();
                }
            }
        }
    }

    @NbBundle.Messages({
        "# {0} - container name",
        "MSG_Renaming=Renaming {0}"
    })
    private void perform(final DockerContainer container, final String name) {
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                ProgressHandle handle = ProgressHandle.createHandle(Bundle.MSG_Renaming(container.getName()));
                handle.start();
                try {
                    DockerAction facade = new DockerAction(container.getInstance());
                    facade.rename(container, name);
                } catch (DockerException ex) {
                    // FIXME inform user
                    LOGGER.log(Level.INFO, null, ex);
                } finally {
                    handle.finish();
                }
            }
        });
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length != 1) {
            return false;
        }
        return activatedNodes[0].getLookup().lookup(DockerContainer.class) != null;
    }

    @NbBundle.Messages("LBL_RenameContainerAction=Rename...")
    @Override
    public String getName() {
        return Bundle.LBL_RenameContainerAction();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

}
