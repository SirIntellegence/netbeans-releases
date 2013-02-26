/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.project.ui.zip;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import static org.netbeans.modules.project.ui.zip.Bundle.*;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.DialogDisplayer;
import org.openide.NotificationLineSupport;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

public class ImportZIP extends JPanel {

    private static final RequestProcessor RP = new RequestProcessor(ImportZIP.class);
    private static final Logger LOG = Logger.getLogger(ImportZIP.class.getName());

    @ActionID(category="Project", id="org.netbeans.modules.project.ui.zip.import")
    @ActionRegistration(iconInMenu=false, displayName="#CTL_ImportZIPAction")
    @ActionReference(path="Menu/File/Import", position=500)
    @Messages({
        "CTL_ImportZIPAction=From &ZIP...",
        "LBL_import=Import",
        "TITLE_import=Import Project(s) from ZIP"
    })
    public static final class ImportZIPAction implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) {
            final ImportZIP panel = new ImportZIP();
            final JButton ok = new JButton(LBL_import());
            NotifyDescriptor d = new NotifyDescriptor(panel, TITLE_import(), NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.PLAIN_MESSAGE, new Object[] {ok, NotifyDescriptor.CANCEL_OPTION}, null);
            final NotificationLineSupport notifications = d.createNotificationLineSupport();
            panel.addPropertyChangeListener(new PropertyChangeListener() {
                @Override public void propertyChange(PropertyChangeEvent evt) {
                    ok.setEnabled(panel.check(notifications));
                }
            });
            if (DialogDisplayer.getDefault().notify(d) == ok) {
                final File zip = new File(panel.zipField.getText());
                final File root = new File(panel.folderField.getText());
                ProjectChooser.setProjectsFolder(root);
                RP.post(new Runnable() {
                    @Override public void run() {
                        try {
                            unpackAndOpen(zip, root);
                        } catch (IOException x) {
                            LOG.log(Level.WARNING, null, x);
                        }
                    }
                });
            }
        }
    }

    @Messages({
        "# {0} - ZIP file", "MSG_unpacking=Unpacking {0}",
        "# {0} - ZIP entry name", "MSG_creating=Creating {0}",
        "# {0} - folder", "MSG_checking=Checking for project: {0}"
    })
    private static void unpackAndOpen(File zip, File root) throws IOException {
        final AtomicBoolean canceled = new AtomicBoolean();
        List<Project> projects = new ArrayList<Project>();
        ProgressHandle handle = ProgressHandleFactory.createHandle(MSG_unpacking(zip.getName()), new Cancellable() {
            @Override public boolean cancel() {
                return canceled.compareAndSet(false, true);
            }
        });
        handle.start();
        try {
            List<File> folders = new ArrayList<File>();
            InputStream is = new FileInputStream(zip);
            try {
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (canceled.get()) {
                        return;
                    }
                    String n = entry.getName();
                    if ("Thumbs.db".equals(n)) {
                        continue; //#226620
                    }
                    File f = new File(root, n);
                    if (n.endsWith("/")) {
                        if (!f.isDirectory()) {
                            if (!f.mkdirs()) {
                                throw new IOException("could not make " + f);
                            }
                            if (entry.getTime() > 0) {
                                if (!f.setLastModified(entry.getTime())) {
                                    // oh well
                                }
                            }
                        }
                        folders.add(f);
                    } else {
                        handle.progress(MSG_creating(n));
                        File p = f.getParentFile();
                        if (!p.isDirectory() && !p.mkdirs()) {
                            throw new IOException("could not make " + p);
                        }
                        OutputStream os = new FileOutputStream(f);
                        try {
                            FileUtil.copy(zis, os);
                        } finally {
                            os.close();
                        }
                        if (entry.getTime() > 0) {
                            if (!f.setLastModified(entry.getTime())) {
                                // oh well
                            }
                        }
                    }
                }
            } finally {
                is.close();
            }
            handle.switchToDeterminate(folders.size());
            for (int i = 0; i < folders.size(); i++) {
                if (canceled.get()) {
                    return;
                }
                File folder = folders.get(i);
                handle.progress(MSG_checking(folder), i);
                FileObject fo = FileUtil.toFileObject(folder);
                if (fo != null) {
                    Project p = ProjectManager.getDefault().findProject(fo);
                    if (p != null) {
                        projects.add(p);
                    }
                }
            }
        } finally {
            handle.finish();
        }
        OpenProjects.getDefault().open(projects.toArray(new Project[projects.size()]), false, true);
    }

    @Messages({
        "ERR_no_zip_open=Must select a ZIP to import from.",
        "# {0} - file", "ERR_zip_nonexistent={0} does not exist.",
        "# {0} - file", "ERR_not_zip={0} is not in ZIP format.",
        "ERR_no_folder=Must select a folder to unpack into.",
        "# {0} - folder", "ERR_folder_nonexistent={0} does not exist."
    })
    private boolean check(NotificationLineSupport notifications) {
        notifications.clearMessages();
        if (zipField.getText().isEmpty()) {
            notifications.setInformationMessage(ERR_no_zip_open());
            return false;
        }
        File zip = new File(zipField.getText());
        if (!zip.isFile()) {
            notifications.setErrorMessage(ERR_zip_nonexistent(zip));
            return false;
        }
        try {
            if (!FileUtil.isArchiveFile(Utilities.toURI(zip).toURL())) {
                notifications.setErrorMessage(ERR_not_zip(zip));
                return false;
            }
        } catch (MalformedURLException x) {
            assert false : x;
        }
        if (folderField.getText().isEmpty()) {
            notifications.setInformationMessage(ERR_no_folder());
            return false;
        }
        if (!new File(folderField.getText()).isDirectory()) {
            notifications.setErrorMessage(ERR_folder_nonexistent(folderField.getText()));
            return false;
        }
        return true;
    }

    private ImportZIP() {
        initComponents();
        folderField.setText(ProjectChooser.getProjectsFolder().getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        zipLabel = new javax.swing.JLabel();
        zipField = new javax.swing.JTextField();
        zipButton = new javax.swing.JButton();
        folderLabel = new javax.swing.JLabel();
        folderField = new javax.swing.JTextField();
        folderButton = new javax.swing.JButton();

        zipLabel.setLabelFor(zipField);
        org.openide.awt.Mnemonics.setLocalizedText(zipLabel, org.openide.util.NbBundle.getMessage(ImportZIP.class, "ImportZIP.zipLabel.text")); // NOI18N

        zipField.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(zipButton, org.openide.util.NbBundle.getMessage(ImportZIP.class, "ImportZIP.zipButton.text")); // NOI18N
        zipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zipButtonActionPerformed(evt);
            }
        });

        folderLabel.setLabelFor(folderField);
        org.openide.awt.Mnemonics.setLocalizedText(folderLabel, org.openide.util.NbBundle.getMessage(ImportZIP.class, "ImportZIP.folderLabel.text")); // NOI18N

        folderField.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(folderButton, org.openide.util.NbBundle.getMessage(ImportZIP.class, "ImportZIP.folderButton.text")); // NOI18N
        folderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(zipLabel)
                    .addComponent(folderLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(folderField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                    .addComponent(zipField, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(zipButton)
                    .addComponent(folderButton))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zipLabel)
                    .addComponent(zipField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(zipButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(folderLabel)
                    .addComponent(folderField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(folderButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    @Messages("LBL_zip_files=ZIP files")
    private void zipButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zipButtonActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(LBL_zip_files(), "zip", "jar"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            zipField.setText(fc.getSelectedFile().getAbsolutePath());
            firePropertyChange("validity", null, null);
        }
    }//GEN-LAST:event_zipButtonActionPerformed

    private void folderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderButtonActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            folderField.setText(fc.getSelectedFile().getAbsolutePath());
            firePropertyChange("validity", null, null);
        }
    }//GEN-LAST:event_folderButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton folderButton;
    private javax.swing.JTextField folderField;
    private javax.swing.JLabel folderLabel;
    private javax.swing.JButton zipButton;
    private javax.swing.JTextField zipField;
    private javax.swing.JLabel zipLabel;
    // End of variables declaration//GEN-END:variables
}
