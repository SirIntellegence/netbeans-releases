/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
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
package org.netbeans.modules.profiler.attach.panels;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.netbeans.modules.profiler.attach.providers.TargetPlatformEnum;
import org.netbeans.modules.profiler.attach.spi.IntegrationProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author  Jaroslav Bachorik
 */
public class ManualIntegrationPanelUI extends javax.swing.JPanel implements ListDataListener {

    private ManualIntegrationPanel.Model model;
    private DefaultListModel listModel = null;

    /**
     * Creates new form ManualIntegrationPanelUI
     */
    ManualIntegrationPanelUI(ManualIntegrationPanel.Model aModel) {
        this.model = aModel;
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        generateRemotePack = new javax.swing.JButton();
        stepsPanel = new org.netbeans.modules.profiler.attach.panels.components.StepsPanelComponent();
        label = new javax.swing.JTextArea();
        comboJvms = new javax.swing.JComboBox();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setMaximumSize(new java.awt.Dimension(800, 600));
        setMinimumSize(new java.awt.Dimension(200, 300));
        setPreferredSize(new java.awt.Dimension(500, 400));

        org.openide.awt.Mnemonics.setLocalizedText(generateRemotePack, org.openide.util.NbBundle.getMessage(ManualIntegrationPanelUI.class, "ManualIntegrationPanelUI.generateRemotePack.text")); // NOI18N
        generateRemotePack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateRemotePackActionPerformed(evt);
            }
        });

        stepsPanel.setTitle(org.openide.util.NbBundle.getMessage(ManualIntegrationPanelUI.class, "ManualIntegrationStepsWizardPanelUI_ManualIntegrationHintMsg")); // NOI18N

        label.setEditable(false);
        label.setLineWrap(true);
        label.setText(org.openide.util.NbBundle.getMessage(ManualIntegrationPanelUI.class, "TargetJVMWizardPanelUI_SelectJvmString")); // NOI18N
        label.setWrapStyleWord(true);
        label.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("Label.foreground"));
        label.setEnabled(false);
        label.setMinimumSize(new java.awt.Dimension(0, 0));
        label.setOpaque(false);

        comboJvms.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Java" }));
        comboJvms.setMaximumSize(new java.awt.Dimension(470, 24));
        comboJvms.setMinimumSize(new java.awt.Dimension(200, 24));
        comboJvms.setPreferredSize(new java.awt.Dimension(466, 24));
        comboJvms.getModel().addListDataListener(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboJvms, 0, 456, Short.MAX_VALUE)
                    .addComponent(stepsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
                    .addComponent(generateRemotePack, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboJvms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stepsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generateRemotePack)
                .addContainerGap())
        );

        comboJvms.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ManualIntegrationPanelUI.class, "ManualIntegrationPanelUI.comboJvms.AccessibleContext.accessibleName")); // NOI18N
        comboJvms.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ManualIntegrationPanelUI.class, "ManualIntegrationPanelUI.comboJvms.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void generateRemotePackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateRemotePackActionPerformed
    RequestProcessor.getDefault().post(new Runnable() {

        public void run() {
            try {
                final JFileChooser chooser = new JFileChooser();
                final File tmpDir = new File(System.getProperty("java.io.tmpdir")); // NOI18N
                chooser.setDialogTitle(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/attach/panels/Bundle").getString("ManualIntegrationPanelUI_ChooseRemotePackDestination")); // NOI18N
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setSelectedFile(tmpDir);
                chooser.setCurrentDirectory(tmpDir);
                chooser.setMultiSelectionEnabled(false);
                if ((JFileChooser.CANCEL_OPTION & chooser.showSaveDialog(ManualIntegrationPanelUI.this)) == 0) {
                    String packPath = model.exportRemotePack(chooser.getSelectedFile().getAbsolutePath());
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/attach/panels/Bundle").getString("ManualIntegrationPanelUI_RemotePackSavedAs") + packPath, NotifyDescriptor.INFORMATION_MESSAGE)); // NOI18N
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    });
}//GEN-LAST:event_generateRemotePackActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboJvms;
    private javax.swing.JButton generateRemotePack;
    private javax.swing.JTextArea label;
    private org.netbeans.modules.profiler.attach.panels.components.StepsPanelComponent stepsPanel;
    // End of variables declaration//GEN-END:variables
    public ComboBoxModel getJvmListModel() {
        return new DefaultComboBoxModel() {

            public Object getElementAt(int index) {
                return model.getSupportedJvms() != null ? model.getSupportedJvms().get(index) : null;
            }

            public Object getSelectedItem() {
                return model.getJvm();
            }

            public int getSize() {
                return model.getSupportedJvms() != null ? model.getSupportedJvms().size() : 0;
            }

            public void setSelectedItem(Object anItem) {
                model.setJvm((TargetPlatformEnum) anItem);
                showIntegrationHints();
            }
        };
    }

    public void refresh() {
        final List supportedJvms = model.getSupportedJvms();

        TargetPlatformEnum defaultPlatform = TargetPlatformEnum.JDK5;
        if (model.getJvm() == null || !supportedJvms.contains(model.getJvm())) {
            if (!supportedJvms.contains(defaultPlatform)) {
                defaultPlatform = (TargetPlatformEnum) supportedJvms.get(0);
            }
        } else {
            defaultPlatform = model.getJvm();
        }

        ((DefaultComboBoxModel) comboJvms.getModel()).removeAllElements();
        for (Iterator it = model.getSupportedJvms().iterator(); it.hasNext();) {
            ((DefaultComboBoxModel) comboJvms.getModel()).addElement(it.next());
        }

        comboJvms.setSelectedItem(defaultPlatform);
//    comboJvms.invalidate();

        showIntegrationHints();

        label.setText(MessageFormat.format(NbBundle.getMessage(ManualIntegrationPanel.class, "TargetJVMWizardPanelUI_SelectJvmString"), new Object[]{model.getApplication()})); // NOI18N

        generateRemotePack.setVisible(model.isRemote());
    }

    private void showIntegrationHints() {
        IntegrationProvider.IntegrationHints hints = this.model.getIntegrationHints();

        stepsPanel.setTitle(MessageFormat.format(NbBundle.getMessage(ManualIntegrationPanel.class, "ManualIntegrationStepsWizardPanelUI_ManualIntegrationHintMsg"), new Object[]{model.getApplication()})); // NOI18N
        stepsPanel.setSteps(this.model.getIntegrationHints());
    }

    public void contentsChanged(ListDataEvent e) {
        model.setJvm((TargetPlatformEnum) comboJvms.getSelectedItem());
        showIntegrationHints();
    }

    public void intervalAdded(ListDataEvent e) {
    }

    public void intervalRemoved(ListDataEvent e) {
    }
}
