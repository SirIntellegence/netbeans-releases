/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.git.ui.rebase;

/**
 *
 * @author Ondrej Vrabec
 */
public class SelectDestPanel extends javax.swing.JPanel {

    /**
     * Creates new form RebaseSimple
     */
    public SelectDestPanel () {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/rebase-destination.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.jLabel1.text")); // NOI18N

        tfBaseRevision.setEditable(false);
        tfBaseRevision.setText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.tfBaseRevision.text")); // NOI18N

        tfDestinationRevision.setEditable(false);
        tfDestinationRevision.setText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.tfDestinationRevision.text")); // NOI18N

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/base.png"))); // NOI18N
        jLabel2.setLabelFor(tfBaseRevision);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.lblBase.text")); // NOI18N
        jLabel2.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.lblBaseRevision.TTtext")); // NOI18N

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/destinations.png"))); // NOI18N
        jLabel3.setLabelFor(tfDestinationRevision);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.lblDest.text")); // NOI18N
        jLabel3.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.lblDest.TTtext")); // NOI18N

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/base.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblBaseRevision.text")); // NOI18N
        jLabel4.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblBaseRevision.TTtext")); // NOI18N

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/destinations.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblDestRevision.text")); // NOI18N
        jLabel5.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblDestRevision.TTtext")); // NOI18N

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/resources/icons/tracked.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblTracked.text")); // NOI18N
        jLabel6.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.legend.lblTracked.TTtext")); // NOI18N

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "PreviewPanel.wcParentHelp.text")); // NOI18N
        jLabel7.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(btnBrowse, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.btnBrowse.text")); // NOI18N
        btnBrowse.setToolTipText(org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "SelectDestPanel.btnBrowse.TTtext")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(SelectDestPanel.class, "RebasePanel.lblPreview.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfBaseRevision)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tfDestinationRevision)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowse))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)))
                    .addComponent(jLabel8))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(tfBaseRevision, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(tfDestinationRevision, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowse))
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel6)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JButton btnBrowse = new javax.swing.JButton();
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    final javax.swing.JTextField tfBaseRevision = new javax.swing.JTextField();
    final javax.swing.JTextField tfDestinationRevision = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables
}
