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
package org.netbeans.modules.j2ee.websphere6.dd.loaders.ui;

import java.awt.event.ItemListener;
import org.netbeans.modules.j2ee.websphere6.dd.beans.*;
import org.netbeans.modules.j2ee.websphere6.dd.loaders.WSMultiViewDataObject;
import org.netbeans.modules.j2ee.websphere6.dd.loaders.webbnd.WSWebBndDataObject;
import org.netbeans.modules.xml.multiview.*;
import org.netbeans.modules.xml.multiview.ui.*;
import org.netbeans.modules.xml.multiview.Error;
/**
 *
 * @author  dlm198383
 */
public class WSReferenceBindingsPanel extends SectionInnerPanel {
    CommonRef reference;
    WSMultiViewDataObject dObj;
     /**
     * Creates new form WSReferenceBindingsPanel
     */
    public WSReferenceBindingsPanel(SectionView view, WSMultiViewDataObject dObj,  CommonRef reference) {
        super(view);
        this.dObj=dObj;
        this.reference=reference;        
        initComponents();
        referenceField.setText(reference.getXmiId());
        jndiNameField.setText(reference.getJndiName());
        hrefField.setText(reference.getHref());
        addModifier(hrefField);
        addModifier(jndiNameField);
        addModifier(referenceField);
        getSectionView().getErrorPanel().clearError();
    }
    
    public void setValue(javax.swing.JComponent source, Object value) {
        if (source==referenceField) {
            reference.setXmiId((String)value);
        } else if(source==jndiNameField) {
            reference.setJndiName((String)value);
        } else if(source==hrefField) {            
            reference.setHref((String)value);
        }
    }
    
    public void documentChanged(javax.swing.text.JTextComponent comp, String value) {
        if (comp==referenceField) {
            String val = (String)value;
            if (val.length()==0) {
                getSectionView().getErrorPanel().setError(new Error(Error.MISSING_VALUE_MESSAGE, "id", comp));
                return;
            }
            getSectionView().getErrorPanel().clearError();
        }
        if(comp==jndiNameField) {
            String val = (String)value;
            if (val.length()==0) {
                getSectionView().getErrorPanel().setError(new Error(Error.MISSING_VALUE_MESSAGE, "jndi", comp));
                return;
            }
            getSectionView().getErrorPanel().clearError();
        }
        if(comp==hrefField) {
            String val = (String)value;
            if (val.length()==0) {
                getSectionView().getErrorPanel().setError(new Error(Error.MISSING_VALUE_MESSAGE, "webhref", comp));
                return;
            }
            getSectionView().getErrorPanel().clearError();
        }
    }
    
    public void rollbackValue(javax.swing.text.JTextComponent source) {
        if (referenceField==source) {
            referenceField.setText(reference.getXmiId());
        }
        if (jndiNameField==source) {
            jndiNameField.setText(reference.getJndiName());
        }
        if (hrefField==source) {
            hrefField.setText(reference.getHref());
        }
    }
    public void linkButtonPressed(Object ddBean, String ddProperty) {
    }
    public javax.swing.JComponent getErrorComponent(String errorId) {
        if ("id".equals(errorId)) return referenceField;
        if ("jndi".equals(errorId)) return jndiNameField;
        if ("webhref".equals(errorId)) return hrefField;
        return null;
    }
     public void itemStateChanged(java.awt.event.ItemEvent evt) {                                            
        // TODO add your handling code here:
            dObj.modelUpdatedFromUI();
            //dObj.setChangedFromUI(true);            
            dObj.setChangedFromUI(false);        
    }
    /** This will be called before model is changed from this panel
     */
    protected void startUIChange() {
        dObj.setChangedFromUI(true);
    }
    
    /** This will be called after model is changed from this panel
     */
    protected void endUIChange() {
        dObj.modelUpdatedFromUI();
        dObj.setChangedFromUI(false);
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        referenceField = new javax.swing.JTextField();
        jndiNameField = new javax.swing.JTextField();
        hrefField = new javax.swing.JTextField();

        jLabel1.setText("Name:");

        jLabel2.setText("JNDI Name:");

        jLabel3.setText("Name in Web.xml :");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel2)
                    .add(jLabel3))
                .add(16, 16, 16)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(referenceField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                    .add(hrefField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                    .add(jndiNameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(referenceField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(jndiNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(hrefField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField hrefField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jndiNameField;
    private javax.swing.JTextField referenceField;
    // End of variables declaration//GEN-END:variables
    
}
