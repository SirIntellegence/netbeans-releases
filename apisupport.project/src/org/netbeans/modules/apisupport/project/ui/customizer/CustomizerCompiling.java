/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.apisupport.project.ui.customizer;

import java.beans.PropertyChangeEvent;
import org.netbeans.modules.apisupport.project.universe.NbPlatform;
import org.openide.util.NbBundle;

/**
 * Represents <em>Compiling</em> panel in Netbeans Module customizer.
 *
 * @author Martin Krauskopf
 */
final class CustomizerCompiling extends NbPropertyPanel.Single {
    
    CustomizerCompiling(final SingleModuleProperties props) {
        super(props, CustomizerCompiling.class);
        initComponents();
        initAccessibility();
        refresh();
    }

    void refresh() {
        debug.setSelected(getBooleanProperty(SingleModuleProperties.BUILD_COMPILER_DEBUG));
        deprecation.setSelected(getBooleanProperty(SingleModuleProperties.BUILD_COMPILER_DEPRECATION));
        options.setText(getProperty(SingleModuleProperties.JAVAC_COMPILERARGS));
        NbPlatform platform = ((SingleModuleProperties) props).getActivePlatform();
        options.setEnabled(platform == null || platform.getHarnessVersion() >= NbPlatform.HARNESS_VERSION_50u1); // #71631
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if (SingleModuleProperties.JAVAC_COMPILERARGS.equals(evt.getPropertyName())) {
            options.setText(getProperty(SingleModuleProperties.JAVAC_COMPILERARGS));
        }
        if (SingleModuleProperties.NB_PLATFORM_PROPERTY.equals(evt.getPropertyName())) {
            NbPlatform platform = ((SingleModuleProperties) props).getActivePlatform();
            options.setEnabled(platform == null || platform.getHarnessVersion() >= NbPlatform.HARNESS_VERSION_50u1);
        }
    }
    
    public void store() {
        setBooleanProperty(SingleModuleProperties.BUILD_COMPILER_DEBUG, debug.isSelected());
        setBooleanProperty(SingleModuleProperties.BUILD_COMPILER_DEPRECATION, deprecation.isSelected());
        setProperty(SingleModuleProperties.JAVAC_COMPILERARGS, options.getText());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        deprecation = new javax.swing.JCheckBox();
        debug = new javax.swing.JCheckBox();
        optionsLabel = new javax.swing.JLabel();
        options = new javax.swing.JTextField();

        org.openide.awt.Mnemonics.setLocalizedText(deprecation, org.openide.util.NbBundle.getMessage(CustomizerCompiling.class, "CTL_ReportDeprecation"));

        org.openide.awt.Mnemonics.setLocalizedText(debug, org.openide.util.NbBundle.getMessage(CustomizerCompiling.class, "CTL_GenerateDebugInfo"));

        optionsLabel.setLabelFor(options);
        org.openide.awt.Mnemonics.setLocalizedText(optionsLabel, NbBundle.getMessage(CustomizerCompiling.class, "LBL_additional_compiler_options"));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(debug)
                    .add(deprecation)
                    .add(layout.createSequentialGroup()
                        .add(optionsLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(options, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(debug)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deprecation)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(optionsLabel)
                    .add(options, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(233, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox debug;
    private javax.swing.JCheckBox deprecation;
    private javax.swing.JTextField options;
    private javax.swing.JLabel optionsLabel;
    // End of variables declaration//GEN-END:variables
    
    private static String getMessage(String key) {
        return NbBundle.getMessage(CustomizerCompiling.class, key);
    }
    
    private void initAccessibility() {
        debug.getAccessibleContext().setAccessibleDescription(getMessage("ACSD_Debug"));
        deprecation.getAccessibleContext().setAccessibleDescription(getMessage("ACSD_Deprecation"));
    }
    
}
