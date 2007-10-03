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

package org.netbeans.modules.versioning.system.cvss.ui.actions.project;

/**
 *
 * @author  Petr Kuzel
 */
public class ImportPanel extends javax.swing.JPanel {

    /** Creates new form ImportPanel */
    public ImportPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        setLayout(new java.awt.GridBagLayout());

        setName(org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK0014"));
        getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("ACSD_ImportPanel"));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK1012"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
        add(jLabel4, gridBagConstraints);

        jLabel3.setLabelFor(folderTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK1104"));
        jLabel3.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_FolderToImport"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 3);
        add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        add(folderTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(folderButton, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK1020"));
        folderButton.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_BrowseFolder"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 0);
        add(folderButton, gridBagConstraints);

        jLabel1.setLabelFor(commentTextArea);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK0001"));
        jLabel1.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_ImportMessage"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 3);
        add(jLabel1, gridBagConstraints);

        commentTextArea.setColumns(40);
        commentTextArea.setRows(8);
        commentScrollPane.setViewportView(commentTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 0);
        add(commentScrollPane, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK0013"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        add(jLabel5, gridBagConstraints);

        jLabel2.setLabelFor(moduleTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(ImportPanel.class, "Bk0002"));
        jLabel2.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_RepositoryFolder"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 3);
        add(jLabel2, gridBagConstraints);

        moduleTextField.setColumns(40);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        add(moduleTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(moduleButton, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK1016"));
        moduleButton.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_BrowseRepository"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 0);
        add(moduleButton, gridBagConstraints);

        checkoutCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(checkoutCheckBox, org.openide.util.NbBundle.getMessage(ImportPanel.class, "BK0011"));
        checkoutCheckBox.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/versioning/system/cvss/ui/actions/project/Bundle").getString("TT_Checkout"));
        checkoutCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        checkoutCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        add(checkoutCheckBox, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JCheckBox checkoutCheckBox = new javax.swing.JCheckBox();
    final javax.swing.JScrollPane commentScrollPane = new javax.swing.JScrollPane();
    final org.netbeans.modules.versioning.system.cvss.ui.components.KTextArea commentTextArea = new org.netbeans.modules.versioning.system.cvss.ui.components.KTextArea();
    final javax.swing.JButton folderButton = new javax.swing.JButton();
    final javax.swing.JTextField folderTextField = new javax.swing.JTextField();
    final javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
    final javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
    final javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
    final javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
    final javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
    final javax.swing.JButton moduleButton = new javax.swing.JButton();
    final javax.swing.JTextField moduleTextField = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables
    
}
