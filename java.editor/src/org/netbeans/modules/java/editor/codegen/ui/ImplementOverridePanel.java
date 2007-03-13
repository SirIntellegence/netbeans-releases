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

package org.netbeans.modules.java.editor.codegen.ui;

import java.util.List;
import javax.lang.model.element.Element;
import javax.swing.JPanel;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.java.editor.codegen.ImplementOverrideMethodGenerator;
import org.openide.util.NbBundle;

/**
 *
 * @author  Dusan Balek
 */
public class ImplementOverridePanel extends JPanel {
    
    private ElementSelectorPanel elementSelector;
    
    /** Creates new form ConstructorPanel */
    public ImplementOverridePanel(ElementNode.Description description, boolean isImplement) {
        initComponents();
        elementSelector = new ElementSelectorPanel(description, false);
        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
        add(elementSelector, gridBagConstraints);
        selectorLabel.setText(NbBundle.getMessage(ImplementOverrideMethodGenerator.class, isImplement ? "LBL_implement_method_select" : "LBL_override_method_select")); //NOI18N
        selectorLabel.setLabelFor(elementSelector);
        
        elementSelector.doInitialExpansion(isImplement ? -1 : 1);
    }
    
    public List<ElementHandle<? extends Element>> getSelectedMethods() {
        return ((ElementSelectorPanel)elementSelector).getSelectedElements();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        selectorLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 6, 12);
        add(selectorLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel selectorLabel;
    // End of variables declaration//GEN-END:variables

}
