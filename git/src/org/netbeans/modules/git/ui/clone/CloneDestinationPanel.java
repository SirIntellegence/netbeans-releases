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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.git.ui.clone;

import javax.swing.JPanel;

import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.versioning.util.AccessibleJFileChooser;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Stupka
 */
public final class CloneDestinationPanel extends JPanel implements ActionListener {
    private String message;
    
    /** Creates new form CloneVisualPanel2 */
    public CloneDestinationPanel() {
        initComponents();
        directoryBrowseButton.addActionListener(this);    
        scanForProjectsCheckBox.addActionListener(this);    
        directoryField.setText(defaultWorkingDirectory().getPath());
        scanForProjectsCheckBox.setSelected(GitModuleConfig.getDefault().getShowCloneCompleted());
    }
    
    public String getName() {
        if (destinationDirectoryPanel == null) {
            return null;
        }
        return destinationDirectoryPanel.getName();
    }
    
    public String getDirectory() {
        return directoryField.getText();
    }

    public String getCloneName() {
        return nameField.getText();
    }

    public String getMessage() {
        return message;
    }

    /**
     * Validates user's input
     * @return <code>true</code> if directory does not exist, <code>false</code> otherwise
     */
    public boolean isInputValid() {
        String dir = directoryField.getText();
        String name = nameField.getText();
        File file = new File (dir, name);
        if (file.exists()) {
            message = NbBundle.getMessage(CloneDestinationPanel.class, "MSG_TARGET_EXISTS"); // NOI18N
            return false;
        } else {
            message = "";
            return true;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        destinationDirectoryPanel = new javax.swing.JPanel();
        directoryLabel = new javax.swing.JLabel();
        directoryBrowseButton = new javax.swing.JButton();
        nameLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();

        destinationDirectoryPanel.setName(org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "destinationDirectoryPanel.Name")); // NOI18N

        directoryLabel.setLabelFor(directoryField);
        org.openide.awt.Mnemonics.setLocalizedText(directoryLabel, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "directoryLabel.Name")); // NOI18N

        directoryField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryFieldActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(directoryBrowseButton, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "directoryBrowseButton.Name")); // NOI18N

        nameLabel.setLabelFor(nameField);
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "nameLabel.Name")); // NOI18N

        scanForProjectsCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(scanForProjectsCheckBox, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "CTL_Scan_After_Clone")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "directoryTitleLabel.Name")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "CloneDestinationPanel.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "CloneDestinationPanel.jLabel3.text")); // NOI18N

        jTextField1.setText(org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "CloneDestinationPanel.jTextField1.text")); // NOI18N
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout destinationDirectoryPanelLayout = new javax.swing.GroupLayout(destinationDirectoryPanel);
        destinationDirectoryPanel.setLayout(destinationDirectoryPanelLayout);
        destinationDirectoryPanelLayout.setHorizontalGroup(
            destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 597, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                        .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(nameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(directoryLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameField, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                            .addComponent(directoryField, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(directoryBrowseButton)))
                .addGap(0, 0, 0))
            .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                .addComponent(scanForProjectsCheckBox)
                .addContainerGap())
            .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField1)
                    .addComponent(jComboBox1, 0, 260, Short.MAX_VALUE))
                .addGap(222, 222, 222))
        );
        destinationDirectoryPanelLayout.setVerticalGroup(
            destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(20, 20, 20)
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                        .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(directoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(directoryBrowseButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(destinationDirectoryPanelLayout.createSequentialGroup()
                        .addComponent(directoryLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameLabel)
                        .addGap(37, 37, 37)))
                .addGap(5, 5, 5)
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destinationDirectoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scanForProjectsCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        scanForProjectsCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CloneDestinationPanel.class, "ACSD_Scan_After_Clone")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(destinationDirectoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(destinationDirectoryPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(64, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void directoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directoryFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_directoryFieldActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed
    
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == directoryBrowseButton) {
            onBrowseClick();
        } else if (evt.getSource() == scanForProjectsCheckBox) {
            GitModuleConfig.getDefault().setShowCloneCompleted(scanForProjectsCheckBox.isSelected());
        }
    }

    private void onBrowseClick() {
        File oldFile = defaultWorkingDirectory();
        JFileChooser fileChooser = new AccessibleJFileChooser(NbBundle.getMessage(CloneDestinationPanel.class, "ACSD_BrowseFolder"), oldFile);   // NO I18N
        fileChooser.setDialogTitle(NbBundle.getMessage(CloneDestinationPanel.class, "Browse_title"));                                            // NO I18N
        fileChooser.setMultiSelectionEnabled(false);
        FileFilter[] old = fileChooser.getChoosableFileFilters();
        for (int i = 0; i < old.length; i++) {
            FileFilter fileFilter = old[i];
            fileChooser.removeChoosableFileFilter(fileFilter);
        }
        fileChooser.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
            public String getDescription() {
                return NbBundle.getMessage(CloneDestinationPanel.class, "Folders"); // NOI18N
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showDialog(this, NbBundle.getMessage(CloneDestinationPanel.class, "OK_Button"));                                            // NO I18N
        File f = fileChooser.getSelectedFile();
        if (f != null) {
            directoryField.setText(f.getAbsolutePath());
        }
    }
    
    /**
     * Returns file to be initally used.
     * <ul>
     * <li>first is takes text in workTextField
     * <li>then recent project folder
     * <li>finally <tt>user.home</tt>
     * <ul>
     */
    private File defaultWorkingDirectory() {
        File defaultDir = null;
        String current = directoryField.getText();
        if (current != null && !(current.trim().equals(""))) {  // NOI18N
            File currentFile = new File(current);
            while (currentFile != null && currentFile.exists() == false) {
                currentFile = currentFile.getParentFile();
            }
            if (currentFile != null) {
                if (currentFile.isFile()) {
                    defaultDir = currentFile.getParentFile();
                } else {
                    defaultDir = currentFile;
                }
            }
        }

        if (defaultDir == null) {
            File projectFolder = ProjectChooser.getProjectsFolder();
            if (projectFolder.exists() && projectFolder.isDirectory()) {
                defaultDir = projectFolder;
            }

        }

        if (defaultDir == null) {
            defaultDir = new File(System.getProperty("user.home"));  // NOI18N
        }

        return defaultDir;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel destinationDirectoryPanel;
    private javax.swing.JButton directoryBrowseButton;
    final javax.swing.JTextField directoryField = new javax.swing.JTextField();
    private javax.swing.JLabel directoryLabel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jTextField1;
    final javax.swing.JTextField nameField = new javax.swing.JTextField();
    private javax.swing.JLabel nameLabel;
    final javax.swing.JCheckBox scanForProjectsCheckBox = new javax.swing.JCheckBox();
    // End of variables declaration//GEN-END:variables
    
}

