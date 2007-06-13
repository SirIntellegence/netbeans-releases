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

package org.netbeans.modules.exceptions;

import java.awt.Cursor;
import java.net.MalformedURLException;
import org.openide.awt.HtmlBrowser;

/**
 *
 * @author  Jindrich Sedek
 */
public class ReportPanel extends javax.swing.JPanel {
    private final ExceptionsSettings exSettings = new ExceptionsSettings();
    
    /** Creates new form ReportPanel */
    public ReportPanel() {
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel6.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jLabel1 = new javax.swing.JLabel();
        loginField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel9 = new javax.swing.JLabel();
        summaryField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        commentArea = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();

        setPreferredSize(new java.awt.Dimension(640, 640));

        jLabel3.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel3.text")); // NOI18N

        jLabel6.setForeground(new java.awt.Color(0, 0, 255));
        jLabel6.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel6.text")); // NOI18N
        jLabel6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                registerClicked(evt);
            }
        });

        jLabel1.setLabelFor(loginField);
        jLabel1.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel1.text")); // NOI18N

        loginField.setText(exSettings.getUserName());

        jLabel9.setLabelFor(summaryField);
        jLabel9.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel9.text")); // NOI18N

        summaryField.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.summaryField.text")); // NOI18N

        jLabel4.setLabelFor(commentArea);
        jLabel4.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel4.text")); // NOI18N

        commentArea.setColumns(20);
        commentArea.setRows(5);
        jScrollPane1.setViewportView(commentArea);
        commentArea.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.commentArea.AccessibleContext.accessibleName")); // NOI18N
        commentArea.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.commentArea.AccessibleContext.accessibleDescription")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel2.text")); // NOI18N

        jLabel7.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel7.text_1")); // NOI18N

        jLabel8.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel8.text_1")); // NOI18N

        jLabel5.setText(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.jLabel5.text_1")); // NOI18N

        jPasswordField1.setText(exSettings.getPasswd());

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                    .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loginField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(jLabel9)
                        .add(28, 28, 28)
                        .add(summaryField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE))
                    .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPasswordField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 543, Short.MAX_VALUE))
                    .add(jLabel8))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(jLabel7)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(loginField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(jPasswordField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(1, 1, 1)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(summaryField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(13, 13, 13)
                .add(jLabel4)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 431, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel8))
        );

        loginField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.loginField.AccessibleContext.accessibleName")); // NOI18N
        loginField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.loginField.AccessibleContext.accessibleDescription")); // NOI18N
        summaryField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.summaryField.AccessibleContext.accessibleName")); // NOI18N
        summaryField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.summaryField.AccessibleContext.accessibleDescription")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ReportPanel.class, "ReportPanel.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
private void registerClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_registerClicked
    try {
        HtmlBrowser.URLDisplayer.getDefault().showURL(new java.net.URL(org.openide.util.NbBundle.getMessage(ReportPanel.class, "REGISTRATION_URL")));
    } catch (MalformedURLException ex) {                              
        java.util.logging.Logger.getLogger(ReportPanel.class.getName()).log(java.util.logging.Level.INFO, ex.getMessage(), ex);
    }//GEN-LAST:event_registerClicked
}                                

public void saveUserName(){
    String login = loginField.getText();
    if ((login!=null)&&(login.length()!=0)) exSettings.setUserName(login);
    String passwd = new String(jPasswordField1.getPassword());
    if ((passwd!=null)&&(passwd.length()!= 0)) exSettings.setPasswd(passwd);
}

public String getSummary(){
    return summaryField.getText();
}

public String getComment(){
    return commentArea.getText();
}

public void setSummary(String str){
    summaryField.setText(str);
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea commentArea;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField loginField;
    private javax.swing.JTextField summaryField;
    // End of variables declaration//GEN-END:variables
}
