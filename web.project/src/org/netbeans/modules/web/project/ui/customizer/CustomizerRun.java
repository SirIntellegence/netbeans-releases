/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.web.project.ui.customizer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.WizardValidationException;

import org.netbeans.modules.j2ee.deployment.devmodules.api.Deployment;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eePlatform;
import org.netbeans.modules.web.project.ProjectWebModule;
import org.netbeans.modules.web.project.Utils;

public class CustomizerRun extends JPanel implements HelpCtx.Provider {
    
    /** Creates new form CustomizerRun */
    public CustomizerRun(WebProjectProperties uiProperties) {
        initComponents();
        
        this.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerRun.class, "ACS_CustomizeRun_A11YDesc")); //NOI18N
        
        ProjectWebModule wm = (ProjectWebModule) uiProperties.getProject().getLookup().lookup(ProjectWebModule.class);
        
//        if (webProperties.get(WebProjectProperties.CONTEXT_PATH) == null)
        if (jTextFieldContextPath.getText().trim().length() == 0)
                jTextFieldContextPath.setText(wm.getContextPath());
        
        // disable editing context path if deployment descriptor does not exist
        jTextFieldContextPath.setEnabled(wm.getDeploymentDescriptor() != null);             

        jTextFieldJ2EE.setDocument( uiProperties.J2EE_PLATFORM_MODEL );
        jTextFieldContextPath.setDocument( uiProperties.CONTEXT_PATH_MODEL );
        jTextFieldRelativeURL.setDocument( uiProperties.LAUNCH_URL_RELATIVE_MODEL );
        jCheckBoxDisplayBrowser.setModel( uiProperties.DISPLAY_BROWSER_MODEL ); 
        jComboBoxServer.setModel( uiProperties.J2EE_SERVER_INSTANCE_MODEL );
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jLabelServer = new javax.swing.JLabel();
        jComboBoxServer = new javax.swing.JComboBox();
        jLabelJ2EE = new javax.swing.JLabel();
        jTextFieldJ2EE = new javax.swing.JTextField();
        jLabelContextPath = new javax.swing.JLabel();
        jTextFieldContextPath = new javax.swing.JTextField();
        jCheckBoxDisplayBrowser = new javax.swing.JCheckBox();
        jLabelContextPathDesc = new javax.swing.JLabel();
        jLabelRelativeURL = new javax.swing.JLabel();
        jTextFieldRelativeURL = new javax.swing.JTextField();
        jLabelURLExample = new javax.swing.JLabel();
        errorLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        jLabelServer.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeRun_Server_LabelMnemonic").charAt(0));
        jLabelServer.setLabelFor(jComboBoxServer);
        jLabelServer.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_Server_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(jLabelServer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 0);
        add(jComboBoxServer, gridBagConstraints);
        jComboBoxServer.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeRun_Server_A11YDesc"));

        jLabelJ2EE.setLabelFor(jTextFieldJ2EE);
        jLabelJ2EE.setText(org.openide.util.NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_J2EE_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(jLabelJ2EE, gridBagConstraints);

        jTextFieldJ2EE.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 0);
        add(jTextFieldJ2EE, gridBagConstraints);

        jLabelContextPath.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeRun_ContextPath_LabelMnemonic").charAt(0));
        jLabelContextPath.setLabelFor(jTextFieldContextPath);
        jLabelContextPath.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_ContextPath_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(jLabelContextPath, gridBagConstraints);

        jTextFieldContextPath.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextFieldContextPathKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 11, 11, 0);
        add(jTextFieldContextPath, gridBagConstraints);
        jTextFieldContextPath.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeRun_ContextPath_A11YDesc"));

        jCheckBoxDisplayBrowser.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeRun_DisplayBrowser_LabelMnemonic").charAt(0));
        jCheckBoxDisplayBrowser.setSelected(true);
        jCheckBoxDisplayBrowser.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_DisplayBrowser_JCheckBox"));
        jCheckBoxDisplayBrowser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDisplayBrowserActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(jCheckBoxDisplayBrowser, gridBagConstraints);
        jCheckBoxDisplayBrowser.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeRun_DisplayBrowser_A11YDesc"));

        jLabelContextPathDesc.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_ContextPathDesc_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 0);
        add(jLabelContextPathDesc, gridBagConstraints);

        jLabelRelativeURL.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeRun_RelativeURL_LabelMnemonic").charAt(0));
        jLabelRelativeURL.setLabelFor(jTextFieldRelativeURL);
        jLabelRelativeURL.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_CustomizeRun_RelativeURL_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 5, 0);
        add(jLabelRelativeURL, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 11, 5, 0);
        add(jTextFieldRelativeURL, gridBagConstraints);
        jTextFieldRelativeURL.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeRun_RelativeURL_A11YDesc"));

        jLabelURLExample.setText(NbBundle.getMessage(CustomizerRun.class, "LBL_RelativeURLExample"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 11, 11, 0);
        add(jLabelURLExample, gridBagConstraints);
        jLabelURLExample.getAccessibleContext().setAccessibleName(NbBundle.getMessage(CustomizerRun.class, "ACS_CustomizeRun_RelativeURLExample_A11YDesc"));

        org.openide.awt.Mnemonics.setLocalizedText(errorLabel, " ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        add(errorLabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void jTextFieldContextPathKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldContextPathKeyReleased
        checkContextPath();
    }//GEN-LAST:event_jTextFieldContextPathKeyReleased

    private void jCheckBoxDisplayBrowserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDisplayBrowserActionPerformed
        boolean editable = jCheckBoxDisplayBrowser.isSelected();
        
        jLabelContextPathDesc.setEnabled(editable);
        jLabelRelativeURL.setEnabled(editable);
        jTextFieldRelativeURL.setEditable(editable);
    }//GEN-LAST:event_jCheckBoxDisplayBrowserActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel errorLabel;
    private javax.swing.JCheckBox jCheckBoxDisplayBrowser;
    private javax.swing.JComboBox jComboBoxServer;
    private javax.swing.JLabel jLabelContextPath;
    private javax.swing.JLabel jLabelContextPathDesc;
    private javax.swing.JLabel jLabelJ2EE;
    private javax.swing.JLabel jLabelRelativeURL;
    private javax.swing.JLabel jLabelServer;
    private javax.swing.JLabel jLabelURLExample;
    private javax.swing.JTextField jTextFieldContextPath;
    private javax.swing.JTextField jTextFieldJ2EE;
    private javax.swing.JTextField jTextFieldRelativeURL;
    // End of variables declaration//GEN-END:variables

    /** Help context where to find more about the paste type action.
     * @return the help context for this action
     */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(CustomizerRun.class);
    }

    private boolean checkContextPath() {
        String message = contextPathValidation();
        errorLabel.setText(message);
        return message != null;
    }

    private String contextPathValidation() {
        String contextPath = jTextFieldContextPath.getText();
        String message = null;
        if (contextPath.length() > 0) {
            if (!contextPath.startsWith("/")) {
                message = NbBundle.getMessage (CustomizerRun.class, "MSG_INVALID_CP_DOES_NOT_START_WITH_SLASH");
            } else if (contextPath.indexOf("//") >= 0) {
                message = NbBundle.getMessage (CustomizerRun.class, "MSG_INVALID_CP_CONTAINS_DOUBLE_SLASH");
                message = "Context path should not contain \"//\"";
            } else if (contextPath.endsWith("/")) {
                message = NbBundle.getMessage (CustomizerRun.class, "MSG_INVALID_CP_ENDS_WITH_SLASH");
                message = "Context path should not end with \"/\"";
            }
        }
        return message;
    }

}
