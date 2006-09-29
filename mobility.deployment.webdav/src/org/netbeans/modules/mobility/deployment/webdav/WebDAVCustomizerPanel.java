/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

/*
 * WebDAVCustomizerPanel.java
 *
 * Created on 8. prosinec 2004, 17:28
 */
package org.netbeans.modules.mobility.deployment.webdav;

import java.awt.Font;
import javax.swing.JSpinner;
import org.openide.util.NbBundle;

/**
 *
 * @author  Adam Sotona
 */
public class WebDAVCustomizerPanel extends javax.swing.JPanel {
    
    /** Creates new form WebDAVCustomizerPanel */
    public WebDAVCustomizerPanel() {
        initComponents();
        jSpinnerPort.setEditor(new JSpinner.NumberEditor(jSpinnerPort, "#0")); //NOI18N
        jTextArea1.setFont(jTextArea1.getFont().deriveFont(Font.ITALIC));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jTextFieldServer = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jSpinnerPort = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldUser = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jPasswordField = new javax.swing.JPasswordField();
        jTextArea1 = new javax.swing.JTextArea();

        setLayout(new java.awt.GridBagLayout());

        jLabel1.setDisplayedMnemonic(NbBundle.getMessage(WebDAVCustomizerPanel.class, "MNM_WebDAV_Server").charAt(0));
        jLabel1.setLabelFor(jTextFieldServer);
        jLabel1.setText(NbBundle.getMessage(WebDAVCustomizerPanel.class, "LBL_WebDAV_Server")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel1, gridBagConstraints);

        jTextFieldServer.setName(WebDAVDeploymentPlugin.PROP_SERVER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(jTextFieldServer, gridBagConstraints);
        jTextFieldServer.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WebDAVCustomizerPanel.class, "ACSD_WebDAV_Server")); // NOI18N

        jLabel2.setDisplayedMnemonic(NbBundle.getMessage(WebDAVCustomizerPanel.class, "MNM_WebDAV_Port").charAt(0));
        jLabel2.setLabelFor(jSpinnerPort);
        jLabel2.setText(NbBundle.getMessage(WebDAVCustomizerPanel.class, "LBL_WebDAV_Port")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 11, 0, 0);
        add(jLabel2, gridBagConstraints);
        jLabel2.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WebDAVCustomizerPanel.class, "ACSD_WebDAV_Port")); // NOI18N

        jSpinnerPort.setName(WebDAVDeploymentPlugin.PROP_PORT);
        jSpinnerPort.setPreferredSize(new java.awt.Dimension(54, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(jSpinnerPort, gridBagConstraints);

        jLabel3.setDisplayedMnemonic(NbBundle.getMessage(WebDAVCustomizerPanel.class, "MNM_WebDAV_User").charAt(0));
        jLabel3.setLabelFor(jTextFieldUser);
        jLabel3.setText(NbBundle.getMessage(WebDAVCustomizerPanel.class, "LBL_WebDAV_User")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(jLabel3, gridBagConstraints);

        jTextFieldUser.setName(WebDAVDeploymentPlugin.PROP_USERID);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(jTextFieldUser, gridBagConstraints);
        jTextFieldUser.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WebDAVCustomizerPanel.class, "ACSD_WebDAV_User")); // NOI18N

        jLabel4.setDisplayedMnemonic(NbBundle.getMessage(WebDAVCustomizerPanel.class, "MNM_WebDAV_Password").charAt(0));
        jLabel4.setLabelFor(jPasswordField);
        jLabel4.setText(NbBundle.getMessage(WebDAVCustomizerPanel.class, "LBL_WebDAV_Password")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(jLabel4, gridBagConstraints);

        jPasswordField.setName(WebDAVDeploymentPlugin.PROP_PASSWORD);
        jPasswordField.setPreferredSize(new java.awt.Dimension(6, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(jPasswordField, gridBagConstraints);
        jPasswordField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WebDAVCustomizerPanel.class, "ACSD_WebDAV_Password")); // NOI18N

        jTextArea1.setBackground(getBackground());
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText(NbBundle.getMessage(WebDAVCustomizerPanel.class, "LBL_WebDAV_Notice")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 0, 0);
        add(jTextArea1, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JLabel jLabel1;
    javax.swing.JLabel jLabel2;
    javax.swing.JLabel jLabel3;
    javax.swing.JLabel jLabel4;
    javax.swing.JPasswordField jPasswordField;
    javax.swing.JSpinner jSpinnerPort;
    javax.swing.JTextArea jTextArea1;
    javax.swing.JTextField jTextFieldServer;
    javax.swing.JTextField jTextFieldUser;
    // End of variables declaration//GEN-END:variables
    
}
