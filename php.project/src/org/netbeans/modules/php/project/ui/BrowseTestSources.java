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

package org.netbeans.modules.php.project.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.php.project.PhpProject;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotificationLineSupport;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 * @author Tomas Mysik
 */
public class BrowseTestSources extends JPanel {
    private static final long serialVersionUID = 1463321897654268L;

    private final PhpProject phpProject;
    private final String info;

    private volatile DialogDescriptor dialogDescriptor;
    private volatile NotificationLineSupport notificationLineSupport;
    private volatile String testSources = null;


    public BrowseTestSources(PhpProject phpProject, String title) {
        this(phpProject, title, null);
    }

    public BrowseTestSources(PhpProject phpProject, String title, String info) {
        assert EventQueue.isDispatchThread();
        assert phpProject != null;
        assert title != null;

        this.phpProject = phpProject;
        this.info = info;

        initComponents();
        infoLabel.setText(title);
        testSourcesTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                processUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                processUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                processUpdate();
            }

            private void processUpdate() {
                testSources = testSourcesTextField.getText();
                validateTestSources();
            }
        });
    }

    /**
     * @return <code>true</code> if OK button is chosen.
     */
    public boolean open() {
        dialogDescriptor = new DialogDescriptor(
                this,
                NbBundle.getMessage(BrowseTestSources.class, "LBL_DirectoryForProject", ProjectUtils.getInformation(phpProject).getDisplayName()),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        notificationLineSupport = dialogDescriptor.createNotificationLineSupport();
        if (info != null) {
            notificationLineSupport.setInformationMessage(info);
        }
        dialogDescriptor.setValid(false);
        return DialogDisplayer.getDefault().notify(dialogDescriptor) == DialogDescriptor.OK_OPTION;
    }

    public String getTestSources() {
        return testSources;
    }

    @NbBundle.Messages("BrowseTestSources.includePath.info=Add testing provider classes (e.g. PHPUnit) to Global Include Path (Tools > Options > PHP).")
    void validateTestSources() {
        assert notificationLineSupport != null;

        assert testSources.equals(testSourcesTextField.getText()) : testSources + " != " + testSourcesTextField.getText();
        String error = Utils.validateTestSources(phpProject, testSources);
        if (error != null) {
            notificationLineSupport.setErrorMessage(error);
            dialogDescriptor.setValid(false);
            return;
        }

        String warning = Utils.warnTestSources(phpProject, testSources);
        if (warning != null) {
            notificationLineSupport.setWarningMessage(warning);
            dialogDescriptor.setValid(true);
            return;
        }

        notificationLineSupport.setInformationMessage(Bundle.BrowseTestSources_includePath_info());
        dialogDescriptor.setValid(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        infoLabel = new JLabel();
        testSourcesLabel = new JLabel();
        testSourcesTextField = new JTextField();
        testSourcesBrowseButton = new JButton();

        Mnemonics.setLocalizedText(infoLabel, "dummy"); // NOI18N

        testSourcesLabel.setLabelFor(testSourcesTextField);
        Mnemonics.setLocalizedText(testSourcesLabel, NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(testSourcesBrowseButton, NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesBrowseButton.text")); // NOI18N
        testSourcesBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                testSourcesBrowseButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(infoLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(testSourcesLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(testSourcesTextField, GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(testSourcesBrowseButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(infoLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(testSourcesLabel)
                    .addComponent(testSourcesTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(testSourcesBrowseButton))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        testSourcesLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesLabel.AccessibleContext.accessibleName")); // NOI18N
        testSourcesLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesLabel.AccessibleContext.accessibleDescription")); // NOI18N
        testSourcesTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesTextField.AccessibleContext.accessibleName")); // NOI18N
        testSourcesTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesTextField.AccessibleContext.accessibleDescription")); // NOI18N
        testSourcesBrowseButton.getAccessibleContext().setAccessibleName(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesBrowseButton.AccessibleContext.accessibleName")); // NOI18N
        testSourcesBrowseButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.testSourcesBrowseButton.AccessibleContext.accessibleDescription")); // NOI18N

        getAccessibleContext().setAccessibleName(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(BrowseTestSources.class, "BrowseTestSources.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void testSourcesBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_testSourcesBrowseButtonActionPerformed
        Utils.browseTestSources(testSourcesTextField, phpProject);
    }//GEN-LAST:event_testSourcesBrowseButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel infoLabel;
    private JButton testSourcesBrowseButton;
    private JLabel testSourcesLabel;
    private JTextField testSourcesTextField;
    // End of variables declaration//GEN-END:variables

}
