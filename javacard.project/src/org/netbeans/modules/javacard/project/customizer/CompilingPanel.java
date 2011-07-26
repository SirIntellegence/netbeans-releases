/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.javacard.project.customizer;

import org.netbeans.modules.javacard.project.JCProjectProperties;
import org.openide.awt.Mnemonics;

import javax.swing.*;
import java.awt.*;
import org.openide.util.HelpCtx;

/**
 *
 * @author Tim Boudreau
 */
public class CompilingPanel extends javax.swing.JPanel {

    /** Creates new form CompilingPanel */
    public CompilingPanel(JCProjectProperties props) {
        initComponents();
        for (Component c : getComponents()) {
            if (c instanceof AbstractButton) {
                Mnemonics.setLocalizedText((AbstractButton) c, ((AbstractButton) c).getText());
            } else if (c instanceof JLabel) {
                Mnemonics.setLocalizedText((JLabel) c, ((JLabel) c).getText());
            }
        }
        compileOnSaveBox.setModel (props.COMPILE_ON_SAVE_BUTTON_MODEL);
        additionalOptions.setDocument(props.ADDITIONAL_COMPILER_OPTIONS_DOCUMENT);
        generateDebugInfoBox.setModel (props.GENERATE_DEBUG_INFO_BUTTON_MODEL);
        deprecationBox.setModel (props.ENABLE_DEPRECATION_BUTTON_MODEL);
        compileOnSaveBox.setEnabled(false); //XXX not yet supported
        HelpCtx.setHelpIDString(this, "org.netbeans.modules.javacard.BuildCompilingPanel"); //NOI18N
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        compileOnSaveBox = new javax.swing.JCheckBox();
        generateDebugInfoBox = new javax.swing.JCheckBox();
        instructions = new javax.swing.JLabel();
        optionsLabel = new javax.swing.JLabel();
        additionalOptions = new javax.swing.JTextField();
        additionalInstructions = new javax.swing.JLabel();
        deprecationBox = new javax.swing.JCheckBox();

        compileOnSaveBox.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.compileOnSaveBox.text")); // NOI18N
        compileOnSaveBox.setEnabled(false);

        generateDebugInfoBox.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.generateDebugInfoBox.text")); // NOI18N

        instructions.setLabelFor(compileOnSaveBox);
        instructions.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.instructions.text")); // NOI18N
        instructions.setEnabled(false);

        optionsLabel.setLabelFor(additionalOptions);
        optionsLabel.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.optionsLabel.text", new Object[] {})); // NOI18N

        additionalOptions.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.additionalOptions.text")); // NOI18N

        additionalInstructions.setLabelFor(additionalOptions);
        additionalInstructions.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.additionalInstructions.text")); // NOI18N

        deprecationBox.setText(org.openide.util.NbBundle.getMessage(CompilingPanel.class, "CompilingPanel.deprecationBox.text", new Object[] {})); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(compileOnSaveBox)
                        .addGap(42, 42, 42))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(instructions, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(generateDebugInfoBox))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(deprecationBox))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(optionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(additionalInstructions, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                            .addComponent(additionalOptions, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(compileOnSaveBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(instructions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generateDebugInfoBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deprecationBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(optionsLabel)
                    .addComponent(additionalOptions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(additionalInstructions)
                .addContainerGap(43, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel additionalInstructions;
    private javax.swing.JTextField additionalOptions;
    private javax.swing.JCheckBox compileOnSaveBox;
    private javax.swing.JCheckBox deprecationBox;
    private javax.swing.JCheckBox generateDebugInfoBox;
    private javax.swing.JLabel instructions;
    private javax.swing.JLabel optionsLabel;
    // End of variables declaration//GEN-END:variables
}
