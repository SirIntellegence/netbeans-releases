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
package org.netbeans.modules.j2ee.sun.share.configbean.customizers.other;

import java.awt.Dimension;
import java.util.ResourceBundle;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.netbeans.modules.j2ee.sun.dd.api.ASDDVersion;
import org.netbeans.modules.j2ee.sun.dd.api.CommonDDBean;
import org.netbeans.modules.j2ee.sun.dd.api.VersionNotSupportedException;
import org.netbeans.modules.j2ee.sun.dd.api.client.JavaWebStartAccess;
import org.netbeans.modules.j2ee.sun.dd.api.client.SunApplicationClient;
import org.netbeans.modules.j2ee.sun.ddloaders.SunDescriptorDataObject;
import org.netbeans.modules.j2ee.sun.ddloaders.Utils;
import org.netbeans.modules.j2ee.sun.ddloaders.multiview.DDTextFieldEditorModel;
import org.netbeans.modules.j2ee.sun.ddloaders.multiview.TextItemEditorModel;
import org.netbeans.modules.xml.multiview.ItemCheckBoxHelper;
import org.netbeans.modules.xml.multiview.ItemEditorHelper;
import org.netbeans.modules.xml.multiview.XmlMultiViewDataSynchronizer;
import org.netbeans.modules.xml.multiview.ui.SectionNodeInnerPanel;
import org.netbeans.modules.xml.multiview.ui.SectionNodeView;
import org.openide.ErrorManager;
import org.openide.util.Exceptions;


/**
 *
 * @author Peter Williams
 */
public class AppClientJWSPanel extends SectionNodeInnerPanel {
	
    private static final ResourceBundle otherBundle = ResourceBundle.getBundle(
        "org.netbeans.modules.j2ee.sun.share.configbean.customizers.other.Bundle"); // NOI18N

    private SunDescriptorDataObject dataObject;
    private SunApplicationClient sunAppClient;
    private ASDDVersion version;

    // true if AS 9.0+ fields are visible.
    private boolean as90FeaturesVisible;
    
	public AppClientJWSPanel(SectionNodeView sectionNodeView, final SunApplicationClient sunAppClient, final ASDDVersion version) {
        super(sectionNodeView);
        this.dataObject = (SunDescriptorDataObject) sectionNodeView.getDataObject();
        this.sunAppClient = sunAppClient;
        this.version = version;
        this.as90FeaturesVisible = true;
        
        initComponents();
        initUserComponents();
    }

    private void initUserComponents() {
        if(ASDDVersion.SUN_APPSERVER_9_0.compareTo(version) <= 0) {
            showAS90Fields(true);
        } else {
            showAS90Fields(false);
        }

        XmlMultiViewDataSynchronizer synchronizer = dataObject.getModelSynchronizer();
        addRefreshable(new ItemEditorHelper(jTxtContextRoot, new SunAppClientTextFieldEditorModel(synchronizer, JavaWebStartAccess.CONTEXT_ROOT)));
        addRefreshable(new ItemEditorHelper(jTxtVendor, new SunAppClientTextFieldEditorModel(synchronizer, JavaWebStartAccess.VENDOR)));
        addRefreshable(new EligibleCheckboxHelper(synchronizer, jChkEligible));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPnlJws = new javax.swing.JPanel();
        jLblContextRoot = new javax.swing.JLabel();
        jTxtContextRoot = new javax.swing.JTextField();
        jLblVendor = new javax.swing.JLabel();
        jTxtVendor = new javax.swing.JTextField();
        jLblEligible = new javax.swing.JLabel();
        jChkEligible = new javax.swing.JCheckBox();

        setOpaque(false);
        setLayout(new java.awt.GridBagLayout());

        jPnlJws.setOpaque(false);
        jPnlJws.setLayout(new java.awt.GridBagLayout());

        jLblContextRoot.setLabelFor(jTxtContextRoot);
        jLblContextRoot.setText(otherBundle.getString("LBL_ContextRoot_1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPnlJws.add(jLblContextRoot, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPnlJws.add(jTxtContextRoot, gridBagConstraints);
        jTxtContextRoot.getAccessibleContext().setAccessibleName(otherBundle.getString("ContextRoot_Acsbl_Name")); // NOI18N
        jTxtContextRoot.getAccessibleContext().setAccessibleDescription(otherBundle.getString("ContextRoot_Acsbl_Desc")); // NOI18N

        jLblVendor.setLabelFor(jTxtVendor);
        jLblVendor.setText(otherBundle.getString("LBL_Vendor_1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        jPnlJws.add(jLblVendor, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        jPnlJws.add(jTxtVendor, gridBagConstraints);
        jTxtVendor.getAccessibleContext().setAccessibleName(otherBundle.getString("ASCN_Vendor")); // NOI18N
        jTxtVendor.getAccessibleContext().setAccessibleDescription(otherBundle.getString("ASCD_Vendor")); // NOI18N

        jLblEligible.setLabelFor(jChkEligible);
        jLblEligible.setText(otherBundle.getString("LBL_Eligible_1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        jPnlJws.add(jLblEligible, gridBagConstraints);

        jChkEligible.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        jPnlJws.add(jChkEligible, gridBagConstraints);
        jChkEligible.getAccessibleContext().setAccessibleName(otherBundle.getString("ASCN_Eligible")); // NOI18N
        jChkEligible.getAccessibleContext().setAccessibleDescription(otherBundle.getString("ASCD_Eligible")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 5, 5);
        add(jPnlJws, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
		
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jChkEligible;
    private javax.swing.JLabel jLblContextRoot;
    private javax.swing.JLabel jLblEligible;
    private javax.swing.JLabel jLblVendor;
    private javax.swing.JPanel jPnlJws;
    private javax.swing.JTextField jTxtContextRoot;
    private javax.swing.JTextField jTxtVendor;
    // End of variables declaration//GEN-END:variables

    private void showAS90Fields(boolean show) {
        if(as90FeaturesVisible != show) {
            as90FeaturesVisible = show;
            jPnlJws.setVisible(show);
        }
    }
    
    public String getHelpId() {
        return "AS_CFG_AppClient";
    }

    public void setValue(JComponent source, Object value) {
    }

    public void linkButtonPressed(Object ddBean, String ddProperty) {
    }

    public JComponent getErrorComponent(String errorId) {
        return null;
    }
    
    /** Return correct preferred size.  The multiline JLabels in this panel cause
     *  the default preferred size behavior to be incorrect (too wide).
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getMinimumSize().width, super.getPreferredSize().height);
    }
    
    // Model class for handling updates to the text fields
    private class SunAppClientTextFieldEditorModel extends TextItemEditorModel {

        private String propertyName;
        
        public SunAppClientTextFieldEditorModel(XmlMultiViewDataSynchronizer synchronizer, String propertyName) {
            super(synchronizer, true, true);
            
            this.propertyName = propertyName;
        }
        
        protected String getValue() {
            String result = null;
            
            try {
                JavaWebStartAccess jws = sunAppClient.getJavaWebStartAccess();
                result = (jws != null) ? (String) jws.getValue(propertyName) : null;
            } catch (VersionNotSupportedException ex) {
//                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
            
            return result;
        }
        
        protected void setValue(String value) {
            try {
                JavaWebStartAccess jws = sunAppClient.getJavaWebStartAccess();
                if(jws == null) {
                    jws = sunAppClient.newJavaWebStartAccess();
                    sunAppClient.setJavaWebStartAccess(jws);
                }
                
                jws.setValue(propertyName, value);

                if(isEmpty(jws)) {
                    sunAppClient.setJavaWebStartAccess(null);
                }
            } catch (VersionNotSupportedException ex) {
//                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        }
    }
    
    private class EligibleCheckboxHelper extends ItemCheckBoxHelper {

        public EligibleCheckboxHelper(XmlMultiViewDataSynchronizer synchronizer, JCheckBox component) {
            super(synchronizer, component);
        }

        public boolean getItemValue() {
            try {
                JavaWebStartAccess jws = sunAppClient.getJavaWebStartAccess();
                return (jws != null) ? Utils.booleanValueOf(jws.getEligible()) : false;
            } catch(VersionNotSupportedException ex) {
//                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
            return false;
        }

        public void setItemValue(boolean value) {
            try {
                JavaWebStartAccess jws = sunAppClient.getJavaWebStartAccess();
                if(jws == null) {
                    jws = sunAppClient.newJavaWebStartAccess();
                    sunAppClient.setJavaWebStartAccess(jws);
                }
                
                jws.setEligible(Boolean.toString(value));

                if(isEmpty(jws)) {
                    sunAppClient.setJavaWebStartAccess(null);
                }
            } catch(VersionNotSupportedException ex) {
//                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        }
    }

    private static boolean isEmpty(JavaWebStartAccess jws) {
        String eligible = jws.getEligible();
        return Utils.strEmpty(jws.getContextRoot()) && 
                Utils.strEmpty(jws.getVendor()) && 
                (Utils.strEmpty(eligible) || !Utils.booleanValueOf(eligible));
    }
}
