/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 *
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

package org.netbeans.modules.mercurial.ui.repository;

/**
 *
 * @author  Petr Kuzel
 */
public class RepositoryPanel extends javax.swing.JPanel {

    /** Creates new form RepositoryPanel */
    public RepositoryPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setMinimumSize(new java.awt.Dimension(480, 160));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/mercurial/ui/repository/Bundle"); // NOI18N
        setName(bundle.getString("BK2018")); // NOI18N
        setVerifyInputWhenFocusTarget(false);

        titleLabel.setText(bundle.getString("BK0001")); // NOI18N

        jLabel2.setLabelFor(urlComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "BK0002")); // NOI18N

        urlComboBox.setEditable(true);

        org.openide.awt.Mnemonics.setLocalizedText(proxySettingsButton, org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "BK0006")); // NOI18N
        proxySettingsButton.setToolTipText(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "ACSD_ProxyDialog")); // NOI18N

        tipLabel.setText(" ");
        tipLabel.setMaximumSize(new java.awt.Dimension(32767, 32767));

        removeButton.setText("Remove from History");

        userPasswordField.setMinimumSize(new java.awt.Dimension(11, 22));
        userPasswordField.setPreferredSize(new java.awt.Dimension(11, 22));

        passwordLabel.setLabelFor(userPasswordField);
        org.openide.awt.Mnemonics.setLocalizedText(passwordLabel, org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "BK0004")); // NOI18N
        passwordLabel.setToolTipText(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "TT_Password")); // NOI18N

        userLabel.setLabelFor(userTextField);
        org.openide.awt.Mnemonics.setLocalizedText(userLabel, org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "BK0003")); // NOI18N
        userLabel.setToolTipText(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "TT_UserName")); // NOI18N

        userTextField.setMinimumSize(new java.awt.Dimension(11, 22));
        userTextField.setPreferredSize(new java.awt.Dimension(11, 22));

        leaveBlankLabel.setText(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "BK0005")); // NOI18N

        tunnelLabel.setText("Use External Tunnel");

        tunnelCommandLabel.setText("Tunnel Command");

        tunnelHelpLabel.setText(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "TT_svn_xxx")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(titleLabel)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(userLabel)
                    .add(passwordLabel)
                    .add(tunnelCommandLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, userPasswordField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                            .add(userTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tipLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                            .add(layout.createSequentialGroup()
                                .add(23, 23, 23)
                                .add(leaveBlankLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())))
                    .add(layout.createSequentialGroup()
                        .add(tunnelCommandTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(urlComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 382, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(removeButton)
                        .addContainerGap())))
            .add(layout.createSequentialGroup()
                .add(proxySettingsButton)
                .addContainerGap())
            .add(layout.createSequentialGroup()
                .add(tunnelLabel)
                .addContainerGap())
            .add(tunnelHelpLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 651, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(titleLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(removeButton)
                    .add(urlComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tipLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(userLabel)
                    .add(leaveBlankLabel)
                    .add(userTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(passwordLabel)
                    .add(userPasswordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(proxySettingsButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tunnelLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tunnelCommandLabel)
                    .add(tunnelCommandTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tunnelHelpLabel)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titleLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "ACSD_RepositoryPanel_Title")); // NOI18N
        titleLabel.getAccessibleContext().setAccessibleParent(this);
        jLabel2.getAccessibleContext().setAccessibleParent(this);
        urlComboBox.getAccessibleContext().setAccessibleParent(this);
        proxySettingsButton.getAccessibleContext().setAccessibleParent(this);
        userPasswordField.getAccessibleContext().setAccessibleParent(this);
        passwordLabel.getAccessibleContext().setAccessibleParent(this);
        userLabel.getAccessibleContext().setAccessibleParent(this);
        userTextField.getAccessibleContext().setAccessibleParent(this);
        leaveBlankLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "ACSD_InfoLabel")); // NOI18N
        leaveBlankLabel.getAccessibleContext().setAccessibleParent(this);

        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryPanel.class, "ACSD_RepositoryPanel")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
    final javax.swing.JLabel leaveBlankLabel = new javax.swing.JLabel();
    final javax.swing.JLabel passwordLabel = new javax.swing.JLabel();
    final javax.swing.JButton proxySettingsButton = new javax.swing.JButton();
    final javax.swing.JButton removeButton = new javax.swing.JButton();
    final javax.swing.JLabel tipLabel = new javax.swing.JLabel();
    final javax.swing.JLabel titleLabel = new javax.swing.JLabel();
    final javax.swing.JLabel tunnelCommandLabel = new javax.swing.JLabel();
    final javax.swing.JTextField tunnelCommandTextField = new javax.swing.JTextField();
    final javax.swing.JLabel tunnelHelpLabel = new javax.swing.JLabel();
    final javax.swing.JLabel tunnelLabel = new javax.swing.JLabel();
    final javax.swing.JComboBox urlComboBox = new javax.swing.JComboBox();
    final javax.swing.JLabel userLabel = new javax.swing.JLabel();
    final javax.swing.JPasswordField userPasswordField = new javax.swing.JPasswordField();
    final javax.swing.JTextField userTextField = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables
        
}
