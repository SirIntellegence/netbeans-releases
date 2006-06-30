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
/*
 * EjbRefCustomizer.java
 *
 * Created on September 4, 2003, 5:28 PM
 */

package org.netbeans.modules.j2ee.sun.share.configbean.customizers;

import java.util.ResourceBundle;

import java.beans.Customizer;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.netbeans.modules.j2ee.sun.share.configbean.EjbRef;
import org.netbeans.modules.j2ee.sun.share.configbean.ErrorMessageDB;
import org.netbeans.modules.j2ee.sun.share.configbean.customizers.common.CustomizerErrorPanel;
import org.netbeans.modules.j2ee.sun.share.configbean.customizers.common.CustomizerTitlePanel;
import org.netbeans.modules.j2ee.sun.share.configbean.customizers.common.BaseCustomizer;


/**
 *
 * @author Peter Williams
 */
public class EjbRefCustomizer extends BaseCustomizer implements PropertyChangeListener {
	
	private static final ResourceBundle customizerBundle = ResourceBundle.getBundle(
		"org.netbeans.modules.j2ee.sun.share.configbean.customizers.Bundle");	// NOI18N

	private EjbRef theBean;
	
	/** Creates new form EjbRefCustomizer */
	public EjbRefCustomizer() {
		initComponents();
		initUserComponents();
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLblName = new javax.swing.JLabel();
        jTxtName = new javax.swing.JTextField();
        jLblJndiName = new javax.swing.JLabel();
        jTxtJndiName = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLblName.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLblName.setLabelFor(jTxtName);
        jLblName.setText(customizerBundle.getString("LBL_EJBReferenceName_1"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLblName, gridBagConstraints);

        jTxtName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jTxtName, gridBagConstraints);
        jTxtName.getAccessibleContext().setAccessibleName(customizerBundle.getString("ACSN_EJBReferenceName"));
        jTxtName.getAccessibleContext().setAccessibleDescription(customizerBundle.getString("ACSD_EJBReferenceName"));

        jLblJndiName.setLabelFor(jTxtJndiName);
        jLblJndiName.setText(customizerBundle.getString("LBL_JNDIName_1"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLblJndiName, gridBagConstraints);

        jTxtJndiName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTxtJndiNameKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jTxtJndiName, gridBagConstraints);
        jTxtJndiName.getAccessibleContext().setAccessibleName(customizerBundle.getString("ACSN_JNDIName"));
        jTxtJndiName.getAccessibleContext().setAccessibleDescription(customizerBundle.getString("ACSD_JNDIName"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        add(jPanel1, gridBagConstraints);

    }//GEN-END:initComponents

	private void jTxtJndiNameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTxtJndiNameKeyReleased
		// Add your handling code here:
		if(theBean != null) {
			try {
				theBean.setJndiName(jTxtJndiName.getText());
				validateField(EjbRef.FIELD_JNDI_NAME);
			} catch(PropertyVetoException ex) {
				jTxtJndiName.setText(theBean.getJndiName());
			}
		}
	}//GEN-LAST:event_jTxtJndiNameKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLblJndiName;
    private javax.swing.JLabel jLblName;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTxtJndiName;
    private javax.swing.JTextField jTxtName;
    // End of variables declaration//GEN-END:variables

	private void initUserComponents() {
		// Add title panel
		addTitlePanel(customizerBundle.getString("TITLE_EJBReference"));	// NOI18N
		getAccessibleContext().setAccessibleName(customizerBundle.getString("ACSN_EJBReference"));	// NOI18N
		getAccessibleContext().setAccessibleDescription(customizerBundle.getString("ACSD_EJBReference"));	// NOI18N
		
		// Add error panel
		addErrorPanel();
	}	
	
	protected void initFields() {
		jTxtName.setText(theBean.getEjbRefName());
		jTxtJndiName.setText(theBean.getJndiName());
	}
	
	public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
		String eventName = propertyChangeEvent.getPropertyName();
		
		if(EjbRef.EJB_REF_NAME.equals(eventName)) {
			jTxtName.setText(theBean.getEjbRefName());
		}
	}
	
	protected void addListeners() {
		super.addListeners();
		theBean.addPropertyChangeListener(this);
	}
	
	protected void removeListeners() {
		super.removeListeners();
		theBean.removePropertyChangeListener(this);
	}
	
	protected boolean setBean(Object bean) {
		boolean result = super.setBean(bean);
		
		if(bean instanceof EjbRef) {
			theBean = (EjbRef) bean;
			result = true;
		} else {
			// if bean is not an EjbRef, then it shouldn't have passed Base either.
			assert (result == false) : 
				"EjbRefCustomizer was passed wrong bean type in setBean(Object bean)";	// NOI18N
				
			theBean = null;
			result = false;
		}
		
		return result;
	}

	public String getHelpId() {
		return "AS_CFG_EjbRef";	// NOI18N
	}
}
