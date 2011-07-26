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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.modules.clearcase.util;

import org.netbeans.modules.clearcase.ClearcaseModuleConfig;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

import java.io.File;
import org.netbeans.modules.clearcase.Clearcase;

/**
 *
 * @author Maros Sandor
 */
public class CheckoutActionPanel extends javax.swing.JPanel {
    
    /** Creates new form CheckoutActionPanel 
     * @param file
     * @param odc*/
    public CheckoutActionPanel(File file, ClearcaseModuleConfig.OnDemandCheckout odc) {
        initComponents();
        Mnemonics.setLocalizedText(jLabel1, NbBundle.getMessage(CheckoutActionPanel.class, "CheckoutActionPanel.jLabel1.text", file.getName()));
        
        rbHijack.setSelected(odc == ClearcaseModuleConfig.OnDemandCheckout.Hijack);
        rbReserved.setSelected(odc == ClearcaseModuleConfig.OnDemandCheckout.Reserved);
        rbUnreserved.setSelected(odc == ClearcaseModuleConfig.OnDemandCheckout.Unreserved);

        rbHijack.setEnabled(Clearcase.getInstance().getTopmostSnapshotViewAncestor(file) != null);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        rbHijack = new javax.swing.JRadioButton();
        rbReserved = new javax.swing.JRadioButton();
        rbUnreserved = new javax.swing.JRadioButton();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CheckoutActionPanel.class, "CheckoutActionPanel.jLabel1.text")); // NOI18N

        buttonGroup1.add(rbHijack);
        org.openide.awt.Mnemonics.setLocalizedText(rbHijack, org.openide.util.NbBundle.getMessage(CheckoutActionPanel.class, "CheckoutActionPanel.rbHijack.text")); // NOI18N

        buttonGroup1.add(rbReserved);
        org.openide.awt.Mnemonics.setLocalizedText(rbReserved, org.openide.util.NbBundle.getMessage(CheckoutActionPanel.class, "CheckoutActionPanel.rbReserved.text")); // NOI18N

        buttonGroup1.add(rbUnreserved);
        org.openide.awt.Mnemonics.setLocalizedText(rbUnreserved, org.openide.util.NbBundle.getMessage(CheckoutActionPanel.class, "CheckoutActionPanel.rbUnreserved.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbHijack)
                            .addComponent(rbReserved)
                            .addComponent(rbUnreserved))))
                .addContainerGap(49, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbHijack)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbReserved)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbUnreserved))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.ButtonGroup buttonGroup1;
    javax.swing.JLabel jLabel1;
    javax.swing.JRadioButton rbHijack;
    javax.swing.JRadioButton rbReserved;
    javax.swing.JRadioButton rbUnreserved;
    // End of variables declaration//GEN-END:variables
    
}
