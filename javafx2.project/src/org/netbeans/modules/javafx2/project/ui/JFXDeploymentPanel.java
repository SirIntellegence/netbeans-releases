/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

/*
 * JFXDeploymentPanel.java
 *
 * Created on 1.8.2011, 15:51:50
 */
package org.netbeans.modules.javafx2.project.ui;

import java.awt.Dialog;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;
import org.netbeans.modules.javafx2.project.JFXProjectProperties;
import org.netbeans.modules.javafx2.project.JFXProjectProperties.BundlingType;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Somol
 */
public class JFXDeploymentPanel extends javax.swing.JPanel implements HelpCtx.Provider {

    private File lastImageFolder = null;
    private JFXProjectProperties jfxProps;
    
    private static final Logger LOGGER = Logger.getLogger("javafx"); // NOI18N
    
    private volatile boolean comboBoxNativeBundlingActionRunning = false;
    
    /**
     * Creates new form JFXDeploymentPanel
     */
    public JFXDeploymentPanel(JFXProjectProperties props) {
        this.jfxProps = props;
        initComponents();
        if(JFXProjectProperties.isTrue(props.getEvaluator().getProperty(JFXProjectProperties.JAVAFX_SWING))) {
            // disable UI components irrelevant for FX-in-Swing project
            labelInitialRemark.setVisible(false);
            labelInitialRemark.setEnabled(false);
            labelInitialRemarkSwing.setVisible(true);
            labelInitialRemarkSwing.setEnabled(true);
            labelProperties.setVisible(false);
            labelProperties.setEnabled(false);
            labelPropertiesSwing.setVisible(true);
            labelPropertiesSwing.setEnabled(true);
            //checkBoxUpgradeBackground.setVisible(false);
            //checkBoxNoInternet.setVisible(false);
            checkBoxInstallPerm.setVisible(false);
            checkBoxDeskShortcut.setVisible(false);
            checkBoxMenuShortcut.setVisible(false);
            labelCustomJS.setVisible(false);
            labelCustomJSMessage.setVisible(false);
            buttonCustomJSMessage.setVisible(false);
            labelDownloadMode.setVisible(false);
            labelDownloadModeMessage.setVisible(false);
            buttonDownloadMode.setVisible(false);
            //checkBoxUpgradeBackground.setEnabled(false);
            //checkBoxNoInternet.setEnabled(false);
            checkBoxInstallPerm.setEnabled(false);
            checkBoxDeskShortcut.setEnabled(false);
            checkBoxMenuShortcut.setEnabled(false);
            labelCustomJS.setEnabled(false);
            labelCustomJSMessage.setEnabled(false);
            buttonCustomJSMessage.setEnabled(false);
            labelDownloadMode.setEnabled(false);
            labelDownloadModeMessage.setEnabled(false);
            buttonDownloadMode.setEnabled(false);
        } else {
            labelInitialRemark.setVisible(true);
            labelInitialRemark.setEnabled(true);
            labelInitialRemarkSwing.setVisible(false);
            labelInitialRemarkSwing.setEnabled(false);
            labelProperties.setVisible(true);
            labelProperties.setEnabled(true);
            labelPropertiesSwing.setVisible(false);
            labelPropertiesSwing.setEnabled(false);
            checkBoxInstallPerm.setModel(jfxProps.getInstallPermanentlyModel());
            checkBoxDeskShortcut.setModel(jfxProps.getAddDesktopShortcutModel());
            checkBoxMenuShortcut.setModel(jfxProps.getAddStartMenuShortcutModel());
            refreshCustomJSLabel();
            if(jfxProps.getRuntimeCP().isEmpty()) {
                buttonDownloadMode.setEnabled(false);
                labelDownloadMode.setEnabled(false);
                labelDownloadModeMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_DownloadModeNone")); // NOI18N
                labelDownloadModeMessage.setEnabled(false);
            } else {
                refreshDownloadModeControls();
            }
        }
        checkBoxUpgradeBackground.setModel(jfxProps.getBackgroundUpdateCheckModel());
        checkBoxNoInternet.setModel(jfxProps.getAllowOfflineModel());
        checkBoxDisableProxy.setModel(jfxProps.getDisableProxyModel());

        checkBoxUnrestrictedAcc.setSelected(jfxProps.getSigningEnabled());
        labelSigning.setEnabled(jfxProps.getSigningEnabled());
        labelSigningMessage.setEnabled(jfxProps.getSigningEnabled());
        buttonSigning.setEnabled(jfxProps.getSigningEnabled());
        refreshSigningLabel();
        refreshIconsLabel();
        setupNativeBundlingCombo();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panelTopLabel = new javax.swing.JPanel();
        labelInitialRemark = new javax.swing.JLabel();
        labelInitialRemarkSwing = new javax.swing.JLabel();
        panelBottom = new javax.swing.JPanel();
        labelIcons = new javax.swing.JLabel();
        labelIconsMessage = new javax.swing.JLabel();
        buttonIcons = new javax.swing.JButton();
        checkBoxBundle = new javax.swing.JCheckBox();
        comboBoxBundle = new javax.swing.JComboBox();
        checkBoxUnrestrictedAcc = new javax.swing.JCheckBox();
        labelSigning = new javax.swing.JLabel();
        labelSigningMessage = new javax.swing.JLabel();
        buttonSigning = new javax.swing.JButton();
        labelProperties = new javax.swing.JLabel();
        labelPropertiesSwing = new javax.swing.JLabel();
        panelWS1 = new javax.swing.JPanel();
        checkBoxNoInternet = new javax.swing.JCheckBox();
        checkBoxUpgradeBackground = new javax.swing.JCheckBox();
        panelWS2 = new javax.swing.JPanel();
        checkBoxInstallPerm = new javax.swing.JCheckBox();
        checkBoxDeskShortcut = new javax.swing.JCheckBox();
        checkBoxMenuShortcut = new javax.swing.JCheckBox();
        labelCustomJS = new javax.swing.JLabel();
        labelCustomJSMessage = new javax.swing.JLabel();
        buttonCustomJSMessage = new javax.swing.JButton();
        labelDownloadMode = new javax.swing.JLabel();
        labelDownloadModeMessage = new javax.swing.JLabel();
        buttonDownloadMode = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        checkBoxDisableProxy = new javax.swing.JCheckBox();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));

        setLayout(new java.awt.GridBagLayout());

        panelTopLabel.setLayout(new java.awt.GridBagLayout());

        labelInitialRemark.setText(org.openide.util.NbBundle.getBundle(JFXDeploymentPanel.class).getString("JFXDeploymentPanel.labelInitialRemark.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        panelTopLabel.add(labelInitialRemark, gridBagConstraints);

        labelInitialRemarkSwing.setText(org.openide.util.NbBundle.getBundle(JFXDeploymentPanel.class).getString("JFXDeploymentPanel.labelInitialRemarkSwing.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        panelTopLabel.add(labelInitialRemarkSwing, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.ABOVE_BASELINE_LEADING;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        add(panelTopLabel, gridBagConstraints);

        panelBottom.setLayout(new java.awt.GridBagLayout());

        labelIcons.setLabelFor(labelIconsMessage);
        org.openide.awt.Mnemonics.setLocalizedText(labelIcons, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelIcons.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 15, 10);
        panelBottom.add(labelIcons, gridBagConstraints);
        labelIcons.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.labelIcons.text")); // NOI18N
        labelIcons.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.labelIcons.text")); // NOI18N

        labelIconsMessage.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelIconsMessage.text")); // NOI18N
        labelIconsMessage.setPreferredSize(new java.awt.Dimension(200, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        panelBottom.add(labelIconsMessage, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(buttonIcons, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.buttonIcons.text")); // NOI18N
        buttonIcons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonIconsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        panelBottom.add(buttonIcons, gridBagConstraints);
        buttonIcons.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.buttonIcons.text")); // NOI18N
        buttonIcons.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.buttonIcons.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxBundle, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxBundle.text")); // NOI18N
        checkBoxBundle.setToolTipText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxBundle.toolTipText")); // NOI18N
        checkBoxBundle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBundleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 10, 0);
        panelBottom.add(checkBoxBundle, gridBagConstraints);
        checkBoxBundle.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxBundle.text")); // NOI18N
        checkBoxBundle.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxBundle.text")); // NOI18N

        comboBoxBundle.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Image", "Installer", " " }));
        comboBoxBundle.setEnabled(false);
        comboBoxBundle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxBundleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        panelBottom.add(comboBoxBundle, gridBagConstraints);
        comboBoxBundle.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.comboBoxBundle.text")); // NOI18N
        comboBoxBundle.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.comboBoxBundle.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxUnrestrictedAcc, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxUnrestrictedAcc.text")); // NOI18N
        checkBoxUnrestrictedAcc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUnrestrictedAccActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 5, 0);
        panelBottom.add(checkBoxUnrestrictedAcc, gridBagConstraints);
        checkBoxUnrestrictedAcc.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxUnrestrictedAcc.text")); // NOI18N
        checkBoxUnrestrictedAcc.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxUnrestrictedAcc.text")); // NOI18N

        labelSigning.setLabelFor(labelSigningMessage);
        org.openide.awt.Mnemonics.setLocalizedText(labelSigning, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelSigning.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 37, 15, 10);
        panelBottom.add(labelSigning, gridBagConstraints);
        labelSigning.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.labelSigning.text")); // NOI18N
        labelSigning.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.labelSigning.text")); // NOI18N

        labelSigningMessage.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelSigningMessage.text")); // NOI18N
        labelSigningMessage.setPreferredSize(new java.awt.Dimension(200, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        panelBottom.add(labelSigningMessage, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(buttonSigning, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.buttonSigning.text")); // NOI18N
        buttonSigning.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSigningActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
        panelBottom.add(buttonSigning, gridBagConstraints);
        buttonSigning.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.buttonSigning.text")); // NOI18N
        buttonSigning.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.buttonSigning.text")); // NOI18N

        labelProperties.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelProperties.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 10, 0);
        panelBottom.add(labelProperties, gridBagConstraints);

        labelPropertiesSwing.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelPropertiesSwing.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 10, 0);
        panelBottom.add(labelPropertiesSwing, gridBagConstraints);

        panelWS1.setLayout(new java.awt.GridBagLayout());

        checkBoxNoInternet.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(checkBoxNoInternet, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxNoInternet.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelWS1.add(checkBoxNoInternet, gridBagConstraints);
        checkBoxNoInternet.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxNoInternet.text")); // NOI18N
        checkBoxNoInternet.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxNoInternet.text")); // NOI18N

        checkBoxUpgradeBackground.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(checkBoxUpgradeBackground, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxUpgradeBackground.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        panelWS1.add(checkBoxUpgradeBackground, gridBagConstraints);
        checkBoxUpgradeBackground.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxUpgradeBackground.text")); // NOI18N
        checkBoxUpgradeBackground.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxUpgradeBackground.text")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 10, 0);
        panelBottom.add(panelWS1, gridBagConstraints);

        panelWS2.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxInstallPerm, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxInstallPerm.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        panelWS2.add(checkBoxInstallPerm, gridBagConstraints);
        checkBoxInstallPerm.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxInstallPerm.text")); // NOI18N
        checkBoxInstallPerm.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxInstallPerm.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxDeskShortcut, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxDeskShortcut.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelWS2.add(checkBoxDeskShortcut, gridBagConstraints);
        checkBoxDeskShortcut.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxDeskShortcut.text")); // NOI18N
        checkBoxDeskShortcut.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxDeskShortcut.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxMenuShortcut, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxMenuShortcut.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelWS2.add(checkBoxMenuShortcut, gridBagConstraints);
        checkBoxMenuShortcut.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxMenuShortcut.text")); // NOI18N
        checkBoxMenuShortcut.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxMenuShortcut.text")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 10, 0);
        panelBottom.add(panelWS2, gridBagConstraints);

        labelCustomJS.setLabelFor(labelCustomJSMessage);
        org.openide.awt.Mnemonics.setLocalizedText(labelCustomJS, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelCustomJS.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 15, 10);
        panelBottom.add(labelCustomJS, gridBagConstraints);
        labelCustomJS.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.labelCustomJS.text")); // NOI18N
        labelCustomJS.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.labelCustomJS.text")); // NOI18N

        labelCustomJSMessage.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelCustomJSMessage.text")); // NOI18N
        labelCustomJSMessage.setPreferredSize(new java.awt.Dimension(200, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        panelBottom.add(labelCustomJSMessage, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(buttonCustomJSMessage, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.buttonCustomJSMessage.text")); // NOI18N
        buttonCustomJSMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCustomJSMessageActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        panelBottom.add(buttonCustomJSMessage, gridBagConstraints);
        buttonCustomJSMessage.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.buttonCustomJSMessage.text")); // NOI18N
        buttonCustomJSMessage.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.buttonCustomJSMessage.text")); // NOI18N

        labelDownloadMode.setLabelFor(labelDownloadModeMessage);
        org.openide.awt.Mnemonics.setLocalizedText(labelDownloadMode, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelDownloadMode.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 0, 10);
        panelBottom.add(labelDownloadMode, gridBagConstraints);
        labelDownloadMode.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.labelDownloadMode.text")); // NOI18N
        labelDownloadMode.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.labelDownloadMode.text")); // NOI18N

        labelDownloadModeMessage.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.labelDownloadModeMessage.text")); // NOI18N
        labelDownloadModeMessage.setPreferredSize(new java.awt.Dimension(200, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        panelBottom.add(labelDownloadModeMessage, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(buttonDownloadMode, org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.buttonDownloadMode.text")); // NOI18N
        buttonDownloadMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownloadModeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelBottom.add(buttonDownloadMode, gridBagConstraints);
        buttonDownloadMode.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.buttonDownloadMode.text")); // NOI18N
        buttonDownloadMode.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.buttonDownloadMode.text")); // NOI18N

        jLabel1.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.jLabel1.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 10, 0);
        panelBottom.add(jLabel1, gridBagConstraints);

        checkBoxDisableProxy.setText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "JFXDeploymentPanel.checkBoxDisableProxy.text")); // NOI18N
        checkBoxDisableProxy.setToolTipText(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "TOOLTIP.JFXDeploymentPanel.checkBoxDisableProxy.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        panelBottom.add(checkBoxDisableProxy, gridBagConstraints);
        checkBoxDisableProxy.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AN_JFXDeploymentPanel.checkBoxDisableProxy.text")); // NOI18N
        checkBoxDisableProxy.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JFXDeploymentPanel.class, "AD_JFXDeploymentPanel.checkBoxDisableProxy.text")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.ABOVE_BASELINE_LEADING;
        add(panelBottom, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(filler2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void refreshDownloadModeControls() {
        if(jfxProps.getRuntimeCP().size() > jfxProps.getLazyJars().size()) {
            if(jfxProps.getLazyJars().isEmpty()) {
                labelDownloadModeMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_DownloadModeEager")); // NOI18N
            } else {
                labelDownloadModeMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_DownloadModeMixed")); // NOI18N
            }
        } else {
            labelDownloadModeMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_DownloadModeLazy")); // NOI18N
        }
    }

    private void buttonCustomJSMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCustomJSMessageActionPerformed
        final JFXJavaScriptCallbacksPanel rc = new JFXJavaScriptCallbacksPanel(jfxProps);
        final DialogDescriptor dd = new DialogDescriptor(rc,
                NbBundle.getMessage(JFXDeploymentPanel.class, "TXT_JSCallbacks"), // NOI18N
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
            jfxProps.setJSCallbacks(rc.getResources());
            jfxProps.setJSCallbacksChanged(true);
            refreshCustomJSLabel();
        }
    }//GEN-LAST:event_buttonCustomJSMessageActionPerformed

    private void buttonDownloadModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownloadModeActionPerformed
        final JFXDownloadModePanel rc = new JFXDownloadModePanel(
                jfxProps.getRuntimeCP(),
                jfxProps.getLazyJars());
        final DialogDescriptor dd = new DialogDescriptor(rc,
                NbBundle.getMessage(JFXDeploymentPanel.class, "TXT_ManageResources"), // NOI18N
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
            jfxProps.setLazyJars(rc.getResources());
            jfxProps.setLazyJarsChanged(true);
            refreshDownloadModeControls();
        }
    }//GEN-LAST:event_buttonDownloadModeActionPerformed

    private void checkBoxUnrestrictedAccActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUnrestrictedAccActionPerformed
        boolean sel = checkBoxUnrestrictedAcc.isSelected();
        labelSigning.setEnabled(sel);
        labelSigningMessage.setEnabled(sel);
        buttonSigning.setEnabled(sel);
        jfxProps.setSigningEnabled(sel);
        jfxProps.setPermissionsElevated(sel);
        if(jfxProps.getSigningEnabled() && jfxProps.getSigningType() == JFXProjectProperties.SigningType.NOSIGN) {
            jfxProps.setSigningType(JFXProjectProperties.SigningType.SELF);
        }
        refreshSigningLabel();
    }//GEN-LAST:event_checkBoxUnrestrictedAccActionPerformed

    private void buttonSigningActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSigningActionPerformed
        JFXSigningPanel panel = new JFXSigningPanel(jfxProps);
        DialogDescriptor dialogDesc = new DialogDescriptor(panel, NbBundle.getMessage(JFXSigningPanel.class, "TITLE_JFXSigningPanel"), true, null); // NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDesc);
        dialog.setVisible(true);
        if (dialogDesc.getValue() == DialogDescriptor.OK_OPTION) {
            panel.store();
            refreshSigningLabel();
        }
    }//GEN-LAST:event_buttonSigningActionPerformed

    private void checkBoxBundleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBundleActionPerformed
        boolean sel = checkBoxBundle.isSelected();
        comboBoxBundle.setEnabled(sel);
        jfxProps.setNativeBundlingEnabled(sel);
        if(jfxProps.getNativeBundlingEnabled() && jfxProps.getNativeBundlingType() == JFXProjectProperties.BundlingType.NONE) {
            jfxProps.setNativeBundlingType(JFXProjectProperties.BundlingType.ALL);
            comboBoxBundle.setSelectedItem(JFXProjectProperties.BundlingType.ALL.getString());
        }
    }//GEN-LAST:event_checkBoxBundleActionPerformed

    private void comboBoxBundleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxBundleActionPerformed
        if(!comboBoxNativeBundlingActionRunning) {
            comboBoxNativeBundlingActionRunning = true;
            String sel = (String)comboBoxBundle.getSelectedItem();
            jfxProps.setNativeBundlingType(sel);
            comboBoxNativeBundlingActionRunning = false;
        }
    }//GEN-LAST:event_comboBoxBundleActionPerformed

    private void buttonIconsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonIconsActionPerformed
        JFXIconsPanel panel = new JFXIconsPanel(jfxProps, lastImageFolder);
        panel.registerDocumentListeners();
        DialogDescriptor dialogDesc = new DialogDescriptor(panel, NbBundle.getMessage(JFXIconsPanel.class, "TITLE_JFXIconsPanel"), true, null); // NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDesc);
        dialog.setVisible(true);
        if (dialogDesc.getValue() == DialogDescriptor.OK_OPTION) {
            panel.store();
            refreshIconsLabel();
        }
        panel.unregisterDocumentListeners();
    }//GEN-LAST:event_buttonIconsActionPerformed

    private void refreshCustomJSLabel() {
        int jsDefs = 0;
        for (Map.Entry<String,String> entry : jfxProps.getJSCallbacks().entrySet()) {
            if(entry.getValue() != null && !entry.getValue().isEmpty()) {
                jsDefs++;
            }
        }
        if(jsDefs == 0) {
            labelCustomJSMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_CallbacksDefinedNone")); // NOI18N
        } else {
            labelCustomJSMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_CallbacksDefined", jsDefs)); // NOI18N
        }
    }

    private void refreshSigningLabel() {
        if(!jfxProps.getSigningEnabled() || jfxProps.getSigningType() == JFXProjectProperties.SigningType.NOSIGN) {
            labelSigningMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_SigningUnsigned")); // NOI18N
        } else {
            if(jfxProps.getSigningType() == JFXProjectProperties.SigningType.KEY) {
                labelSigningMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_SigningKey", jfxProps.getSigningKeyAlias())); // NOI18N
            } else {
                labelSigningMessage.setText(NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_SigningGenerated")); // NOI18N
            }
        }
    }

    private void refreshIconsLabel() {
        String msg = ""; // NOI18N
        if(jfxProps.getWSIconPath() != null && !jfxProps.getWSIconPath().isEmpty()) {
            msg = NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsJNLPDefined"); // NOI18N
        }
        if(jfxProps.getSplashImagePath() != null && !jfxProps.getSplashImagePath().isEmpty()) {
            msg = msg.isEmpty() ? NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsSplashDefined") : //NOI18N
                    msg + ", " + NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsSplashDefined"); // NOI18N
        }
        if(jfxProps.getNativeIconPath() != null && !jfxProps.getNativeIconPath().isEmpty()) {
            msg = msg.isEmpty() ? NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsNativeDefined") : // NOI18N
                    msg + ", " + NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsNativeDefined"); // NOI18N
        }
        if(msg.isEmpty()) {
            msg = NbBundle.getMessage(JFXDeploymentPanel.class, "MSG_IconsUndefined"); // NOI18N
        }
        labelIconsMessage.setText(msg);
    }

    private void setupNativeBundlingCombo() {
        comboBoxNativeBundlingActionRunning = true;
        comboBoxBundle.removeAllItems ();
        for (BundlingType bundleType : BundlingType.values()) {
            if(bundleType != BundlingType.NONE) {
                comboBoxBundle.addItem(bundleType.getString());
            }
        }
        BundlingType bundleType = jfxProps.getNativeBundlingType();
        boolean sel = jfxProps.getNativeBundlingEnabled();
        comboBoxBundle.setSelectedItem(bundleType.getString());
        comboBoxBundle.setEnabled(sel && bundleType != BundlingType.NONE);
        checkBoxBundle.setSelected(sel && bundleType != BundlingType.NONE);
        comboBoxNativeBundlingActionRunning = false;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCustomJSMessage;
    private javax.swing.JButton buttonDownloadMode;
    private javax.swing.JButton buttonIcons;
    private javax.swing.JButton buttonSigning;
    private javax.swing.JCheckBox checkBoxBundle;
    private javax.swing.JCheckBox checkBoxDeskShortcut;
    private javax.swing.JCheckBox checkBoxDisableProxy;
    private javax.swing.JCheckBox checkBoxInstallPerm;
    private javax.swing.JCheckBox checkBoxMenuShortcut;
    private javax.swing.JCheckBox checkBoxNoInternet;
    private javax.swing.JCheckBox checkBoxUnrestrictedAcc;
    private javax.swing.JCheckBox checkBoxUpgradeBackground;
    private javax.swing.JComboBox comboBoxBundle;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel labelCustomJS;
    private javax.swing.JLabel labelCustomJSMessage;
    private javax.swing.JLabel labelDownloadMode;
    private javax.swing.JLabel labelDownloadModeMessage;
    private javax.swing.JLabel labelIcons;
    private javax.swing.JLabel labelIconsMessage;
    private javax.swing.JLabel labelInitialRemark;
    private javax.swing.JLabel labelInitialRemarkSwing;
    private javax.swing.JLabel labelProperties;
    private javax.swing.JLabel labelPropertiesSwing;
    private javax.swing.JLabel labelSigning;
    private javax.swing.JLabel labelSigningMessage;
    private javax.swing.JPanel panelBottom;
    private javax.swing.JPanel panelTopLabel;
    private javax.swing.JPanel panelWS1;
    private javax.swing.JPanel panelWS2;
    // End of variables declaration//GEN-END:variables

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(JFXDeploymentPanel.class.getName());
    }

}
