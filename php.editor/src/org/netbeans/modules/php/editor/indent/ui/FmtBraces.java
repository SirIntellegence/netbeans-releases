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

package org.netbeans.modules.php.editor.indent.ui;

import java.io.IOException;
import static org.netbeans.modules.php.editor.indent.FmtOptions.*;
import static org.netbeans.modules.php.editor.indent.FmtOptions.CategorySupport.OPTION_ID;
import org.netbeans.modules.options.editor.spi.PreferencesCustomizer;


/**
 *
 * @author  phrebejk
 */
public class FmtBraces extends javax.swing.JPanel {
    
    /** Creates new form FmtAlignmentBraces */
    public FmtBraces() {
        initComponents();
        classDeclCombo.putClientProperty(OPTION_ID, classDeclBracePlacement);
        methodDeclCombo.putClientProperty(OPTION_ID, methodDeclBracePlacement);
        otherCombo.putClientProperty(OPTION_ID, otherBracePlacement);
    }
    
    public static PreferencesCustomizer.Factory getController() {
	String preview = "";
        try {
            preview = Utils.loadPreviewText(FmtBlankLines.class.getClassLoader().getResourceAsStream("org/netbeans/modules/php/editor/indent/ui/Braces.php"));
        } catch (IOException ex) {
            // TODO log it
        }
        return new CategorySupport.Factory("braces", FmtBraces.class, //NOI18N
                preview);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bracesPlacementLabel = new javax.swing.JLabel();
        classDeclLabel = new javax.swing.JLabel();
        classDeclCombo = new javax.swing.JComboBox();
        methodDeclLabel = new javax.swing.JLabel();
        methodDeclCombo = new javax.swing.JComboBox();
        otherLabel = new javax.swing.JLabel();
        otherCombo = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();

        setName(org.openide.util.NbBundle.getMessage(FmtBraces.class, "LBL_Braces")); // NOI18N
        setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(bracesPlacementLabel, org.openide.util.NbBundle.getMessage(FmtBraces.class, "LBL_br_bracesPlacement")); // NOI18N

        classDeclLabel.setLabelFor(classDeclCombo);
        org.openide.awt.Mnemonics.setLocalizedText(classDeclLabel, org.openide.util.NbBundle.getMessage(FmtBraces.class, "LBL_bp_ClassDecl")); // NOI18N

        classDeclCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        methodDeclLabel.setLabelFor(methodDeclCombo);
        org.openide.awt.Mnemonics.setLocalizedText(methodDeclLabel, org.openide.util.NbBundle.getMessage(FmtBraces.class, "LBL_bp_MethodDecl")); // NOI18N

        methodDeclCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        methodDeclCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                methodDeclComboActionPerformed(evt);
            }
        });

        otherLabel.setLabelFor(otherCombo);
        org.openide.awt.Mnemonics.setLocalizedText(otherLabel, org.openide.util.NbBundle.getMessage(FmtBraces.class, "LBL_bp_Other")); // NOI18N

        otherCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(otherLabel)
                            .addComponent(methodDeclLabel))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(otherCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(classDeclCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(methodDeclCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(bracesPlacementLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(classDeclLabel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {classDeclCombo, methodDeclCombo, otherCombo});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bracesPlacementLabel)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(classDeclLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(classDeclCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(methodDeclLabel)
                    .addComponent(methodDeclCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(otherLabel)
                    .addComponent(otherCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void methodDeclComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_methodDeclComboActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_methodDeclComboActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bracesPlacementLabel;
    private javax.swing.JComboBox classDeclCombo;
    private javax.swing.JLabel classDeclLabel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox methodDeclCombo;
    private javax.swing.JLabel methodDeclLabel;
    private javax.swing.JComboBox otherCombo;
    private javax.swing.JLabel otherLabel;
    // End of variables declaration//GEN-END:variables
    
}
