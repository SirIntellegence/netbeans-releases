/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javafx2.project.ui;

import javax.swing.JPanel;
import org.netbeans.modules.javafx2.project.JFXProjectProperties;
import org.openide.util.NbBundle;

public final class JFXPreloaderChooserVisualPanel1 extends JPanel {

    /** Creates new form JFXPreloaderChooserVisualPanel1 */
    public JFXPreloaderChooserVisualPanel1() {
        initComponents();
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(JFXPreloaderChooserVisualPanel1.class, "JFXPreloaderChooserVisualPanel1.name"); // NOI18N
    }

    public JFXProjectProperties.PreloaderSourceType getSelectedType() {
        if(radioButtonProject.isSelected()) {
            return JFXProjectProperties.PreloaderSourceType.PROJECT;
        }
        return JFXProjectProperties.PreloaderSourceType.JAR;
    }

    public void setSelectedType(JFXProjectProperties.PreloaderSourceType selectedType) {
        if(selectedType == JFXProjectProperties.PreloaderSourceType.PROJECT) {
            radioButtonProject.setSelected(true);
        } else {
            radioButtonJAR.setSelected(true);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonSourceType = new javax.swing.ButtonGroup();
        radioButtonProject = new javax.swing.JRadioButton();
        radioButtonJAR = new javax.swing.JRadioButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));

        setLayout(new java.awt.GridBagLayout());

        buttonSourceType.add(radioButtonProject);
        radioButtonProject.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(radioButtonProject, org.openide.util.NbBundle.getMessage(JFXPreloaderChooserVisualPanel1.class, "JFXPreloaderChooserVisualPanel1.radioButtonProject.text")); // NOI18N
        radioButtonProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonProjectActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.ABOVE_BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 0, 0);
        add(radioButtonProject, gridBagConstraints);

        buttonSourceType.add(radioButtonJAR);
        org.openide.awt.Mnemonics.setLocalizedText(radioButtonJAR, org.openide.util.NbBundle.getMessage(JFXPreloaderChooserVisualPanel1.class, "JFXPreloaderChooserVisualPanel1.radioButtonJAR.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.ABOVE_BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        add(radioButtonJAR, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 0.1;
        add(filler1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        add(filler2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

private void radioButtonProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonProjectActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_radioButtonProjectActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonSourceType;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JRadioButton radioButtonJAR;
    private javax.swing.JRadioButton radioButtonProject;
    // End of variables declaration//GEN-END:variables
}
