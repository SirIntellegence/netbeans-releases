/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.

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
package org.netbeans.modules.bpel.properties.editors.controls;

import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import org.netbeans.modules.bpel.properties.Util;
import org.netbeans.modules.bpel.properties.editors.controls.valid.DefaultValidStateManager;
import org.netbeans.modules.bpel.properties.editors.controls.valid.ValidStateManager;
import org.openide.util.Lookup;

/**
 *
 * @author nk160297
 */
public class TreeNodeChooser extends JPanel implements CustomNodeChooser {
    
    static final long serialVersionUID = 1L;
    
    private AbstractTreeChooserPanel myTreePanel;
    
    private DefaultValidStateManager fastValidationState;
    private PropertyChangeListener selectionChangeListener;
    
    public TreeNodeChooser(AbstractTreeChooserPanel treePanel) {
        myTreePanel = treePanel;
        //
        createContent();
        // initControls();
        //
        Util.attachDefaultDblClickAction(myTreePanel, myTreePanel);
        Util.activateInlineMnemonics(this);
    }
    
    public AbstractTreeChooserPanel getTreePanel() {
        return myTreePanel;
    }
    
    public void createContent() {
        initComponents();
        fastValidationState = new DefaultValidStateManager();
        //
        myTreePanel.createContent();
    }
    
    public boolean initControls() {
        myTreePanel.initControls();
        myTreePanel.subscribeListeners();
        myTreePanel.getValidator().revalidate(true);
        return false;
    }
    
    public void setSelectedValue(Object newValue) {
        myTreePanel.setSelectedValue(newValue);
    }
    
    public Object getSelectedValue() {
        return myTreePanel.getSelectedNode();
    }
    
    public ValidStateManager getValidStateManager() {
        return fastValidationState;
    }
    
    public boolean unsubscribeListeners() {
        return true;
    }
    
    public boolean subscribeListeners() {
        return true;
    }
    
    public boolean afterClose() {
        return true;
    }
    
    public Lookup getLookup() {
        return myTreePanel.getLookup();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        pnlTreePanel = getTreePanel();

        org.jdesktop.layout.GroupLayout pnlTreePanelLayout = new org.jdesktop.layout.GroupLayout(pnlTreePanel);
        pnlTreePanel.setLayout(pnlTreePanelLayout);
        pnlTreePanelLayout.setHorizontalGroup(
            pnlTreePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 376, Short.MAX_VALUE)
        );
        pnlTreePanelLayout.setVerticalGroup(
            pnlTreePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 287, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(pnlTreePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(pnlTreePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnlTreePanel;
    // End of variables declaration//GEN-END:variables
    
}
