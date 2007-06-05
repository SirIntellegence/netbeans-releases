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

package org.netbeans.modules.mobility.project.ui.customizer;
import java.nio.charset.Charset;
import javax.swing.JPanel;
import org.netbeans.api.mobility.project.ui.customizer.ProjectProperties;
import org.netbeans.modules.mobility.project.DefaultPropertiesDescriptor;
import org.netbeans.spi.mobility.project.ui.customizer.CustomizerPanel;
import org.netbeans.spi.mobility.project.ui.customizer.VisualPropertyGroup;
import org.netbeans.spi.mobility.project.ui.customizer.support.VisualPropertySupport;
import org.openide.util.NbBundle;

/**
 *
 * @author  phrebejk, Adam Sotona
 */
public class CustomizerCompile extends JPanel implements CustomizerPanel, VisualPropertyGroup {
    
    public static final String[] DEBUG_LEVELS = {"debug", "info", "warn", "error", "fatal"}; //NOI18N
    public static final String[] ENCODINGS = Charset.availableCharsets().keySet().toArray(new String[0]);
    
    private static final String[] PROPERTY_GROUP = new String[] {DefaultPropertiesDescriptor.JAVAC_DEPRECATION,
    DefaultPropertiesDescriptor.JAVAC_DEBUG,
    DefaultPropertiesDescriptor.JAVAC_OPTIMIZATION,
    DefaultPropertiesDescriptor.JAVAC_ENCODING,
    DefaultPropertiesDescriptor.DEBUG_LEVEL};
    
    private VisualPropertySupport vps;
    
    /** Creates new form CustomizerCompile */
    public CustomizerCompile() {
        initComponents();
        initAccessibility();
    }
    
    public void initValues(ProjectProperties props, String configuration) {
        vps = VisualPropertySupport.getDefault(props);
        vps.register(defaultCheck, configuration, this);
        
    }
    
    public void initGroupValues(final boolean useDefault) {
        vps.register( deprecateCheck, DefaultPropertiesDescriptor.JAVAC_DEPRECATION, useDefault );
        vps.register( debugCheck, DefaultPropertiesDescriptor.JAVAC_DEBUG, useDefault );
        vps.register( optimizeCheck, DefaultPropertiesDescriptor.JAVAC_OPTIMIZATION, useDefault );
        vps.register( jComboBoxEncoding, ENCODINGS, DefaultPropertiesDescriptor.JAVAC_ENCODING, useDefault);
        vps.register( jComboDebugLevel, DEBUG_LEVELS, DefaultPropertiesDescriptor.DEBUG_LEVEL, useDefault );
        javacLabel.setEnabled(!useDefault);
        jLabelEncoding.setEnabled(!useDefault);
        jLabelDebugLevel.setEnabled(!useDefault);
    }
    
    public String[] getGroupPropertyNames() {
        return PROPERTY_GROUP;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        defaultCheck = new javax.swing.JCheckBox();
        javacLabel = new javax.swing.JLabel();
        debugCheck = new javax.swing.JCheckBox();
        optimizeCheck = new javax.swing.JCheckBox();
        deprecateCheck = new javax.swing.JCheckBox();
        jLabelEncoding = new javax.swing.JLabel();
        jComboBoxEncoding = new javax.swing.JComboBox();
        jLabelDebugLevel = new javax.swing.JLabel();
        jComboDebugLevel = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        defaultCheck.setMnemonic(org.openide.util.NbBundle.getBundle(CustomizerCompile.class).getString("MNM_Use_Default").charAt(0));
        defaultCheck.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_Use_Default")); // NOI18N
        defaultCheck.setMargin(new java.awt.Insets(0, 0, 0, 2));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(defaultCheck, gridBagConstraints);
        defaultCheck.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerCompile.class, "ACSD_CustCompile_UseDefault")); // NOI18N

        javacLabel.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_JavacOptions")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(javacLabel, gridBagConstraints);

        debugCheck.setMnemonic(org.openide.util.NbBundle.getBundle(CustomizerCompile.class).getString("MNM_CustCompile_DebuggingInfo").charAt(0));
        debugCheck.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_DebuggingInfo")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(debugCheck, gridBagConstraints);
        debugCheck.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerCompile.class, "ACSD_CustCompile_GenDebug")); // NOI18N

        optimizeCheck.setMnemonic(org.openide.util.NbBundle.getBundle(CustomizerCompile.class).getString("MNM_CustCompile_Optimization").charAt(0));
        optimizeCheck.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_Optimization")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(optimizeCheck, gridBagConstraints);
        optimizeCheck.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerCompile.class, "ACSD_CustCompile_Optimalization")); // NOI18N

        deprecateCheck.setMnemonic(org.openide.util.NbBundle.getBundle(CustomizerCompile.class).getString("MNM_CustCompile_ReportDeprecated").charAt(0));
        deprecateCheck.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_ReportDeprecated")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(deprecateCheck, gridBagConstraints);
        deprecateCheck.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerCompile.class, "ACSD_CustCompile_Deprecated")); // NOI18N

        jLabelEncoding.setDisplayedMnemonic(NbBundle.getMessage(CustomizerCompile.class, "MNM_CustCompile_Encoding").charAt(0));
        jLabelEncoding.setLabelFor(jComboBoxEncoding);
        jLabelEncoding.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_Encoding")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 0, 0);
        add(jLabelEncoding, gridBagConstraints);

        jComboBoxEncoding.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 5, 0, 0);
        add(jComboBoxEncoding, gridBagConstraints);

        jLabelDebugLevel.setDisplayedMnemonic(NbBundle.getMessage(CustomizerCompile.class, "MNM_CustCompile_DebugLevel").charAt(0));
        jLabelDebugLevel.setLabelFor(jComboDebugLevel);
        jLabelDebugLevel.setText(NbBundle.getMessage(CustomizerCompile.class, "LBL_CustCompile_DebugLevel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 0, 0);
        add(jLabelDebugLevel, gridBagConstraints);

        jComboDebugLevel.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(11, 5, 0, 0);
        add(jComboDebugLevel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    
    private void initAccessibility() {
        getAccessibleContext().setAccessibleName(NbBundle.getMessage(CustomizerCompile.class, "ACSN_CustCompile"));
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerCompile.class, "ACSD_CustCompile"));
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox debugCheck;
    private javax.swing.JCheckBox defaultCheck;
    private javax.swing.JCheckBox deprecateCheck;
    private javax.swing.JComboBox jComboBoxEncoding;
    private javax.swing.JComboBox jComboDebugLevel;
    private javax.swing.JLabel jLabelDebugLevel;
    private javax.swing.JLabel jLabelEncoding;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel javacLabel;
    private javax.swing.JCheckBox optimizeCheck;
    // End of variables declaration//GEN-END:variables
    
    
    
}
