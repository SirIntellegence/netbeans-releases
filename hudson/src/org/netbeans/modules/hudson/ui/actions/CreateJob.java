/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.hudson.ui.actions;

import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import org.netbeans.api.project.Project;
import org.netbeans.modules.hudson.api.ConnectionBuilder;
import org.netbeans.modules.hudson.api.HudsonJob;
import org.netbeans.modules.hudson.impl.HudsonInstanceImpl;
import org.netbeans.modules.hudson.spi.ProjectHudsonJobCreatorFactory.ProjectHudsonJobCreator;
import org.netbeans.modules.hudson.spi.ProjectHudsonProvider;
import org.netbeans.modules.hudson.util.Utilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;

/**
 * Submenu action to create a job on this server from one of the open projects.
 */
public class CreateJob extends AbstractAction {

    private final HudsonInstanceImpl instance;

    public CreateJob(HudsonInstanceImpl instance) {
        super("Create Job..."); // XXX I18N
        this.instance = instance;
    }

    public void actionPerformed(ActionEvent e) {
        Set<String> takenNames = new HashSet<String>();
        for (HudsonJob job : instance.getJobs()) {
            takenNames.add(job.getName());
        }
        final CreateJobPanel panel = new CreateJobPanel();
        DialogDescriptor dd = new DialogDescriptor(panel, "New Continuous Build"); // XXX I18N
        panel.init(takenNames, dd);
        Object result = DialogDisplayer.getDefault().notify(dd);
        if (result == NotifyDescriptor.OK_OPTION) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    finalizeJob(panel.creator, panel.name(), panel.selectedProject());
                }
            });
        }
    }

    private void finalizeJob(ProjectHudsonJobCreator creator, String name, Project project) {
        try {
            Document doc = creator.configure();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLUtil.write(doc, baos, "UTF-8"); // NOI18N
            String createItemURL = instance.getUrl() + "createItem?name=" + Utilities.uriEncode(name); // NOI18N
            new ConnectionBuilder().instance(instance).url(createItemURL).
                    header("Content-Type", "text/xml").
                    postData(baos.toByteArray()).
                    httpConnection().disconnect();
            URLDisplayer.getDefault().showURL(new URL(instance.getUrl() + "job/" + Utilities.uriEncode(name) + "/")); // NOI18N
            instance.synchronize();
            ProjectHudsonProvider.getDefault().recordAssociation(project,
                    new ProjectHudsonProvider.Association(instance.getUrl(), name));
        } catch (IOException x) {
            // XXX too harsh, should report at a low level and show message
            Exceptions.printStackTrace(x);
        }
    }

}
