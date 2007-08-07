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
 * PlatformInstallPanel.java
 *
 * Created on April 8, 2004, 1:39 PM
 */
package org.netbeans.modules.mobility.project.ui.wizard;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.PlatformsCustomizer;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.j2me.cdc.platform.CDCPlatform;
import org.netbeans.modules.mobility.cldcplatform.J2MEPlatform;
import org.openide.loaders.TemplateWizard;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author  David Kaspar
 */
public class PlatformInstallPanel extends javax.swing.JPanel {
    
    ActionListener listener = null;
    final private String platformType;
    /** Creates new form PlatformInstallPanel */
    public PlatformInstallPanel(String pt) {
        platformType=pt;
        initComponents();
        initAccessibility();
    }
    
    public void setActionListener(final ActionListener listener) {
        this.listener = listener;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        bInstall = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();

        setName(org.openide.util.NbBundle.getMessage(PlatformInstallPanel.class, "TITLE_Platform")); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(PlatformInstallPanel.class, "LBL_Platform_Info1")); // NOI18N
        jLabel1.setMaximumSize(new java.awt.Dimension(500, 60));
        jLabel1.setMinimumSize(new java.awt.Dimension(500, 30));
        jLabel1.setPreferredSize(new java.awt.Dimension(500, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        add(jLabel1, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(PlatformInstallPanel.class, "LBL_Platform_Info2",platformType.toUpperCase())); // NOI18N
        jLabel2.setMaximumSize(new java.awt.Dimension(500, 60));
        jLabel2.setMinimumSize(new java.awt.Dimension(500, 30));
        jLabel2.setPreferredSize(new java.awt.Dimension(500, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 6, 0);
        add(jLabel2, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(PlatformInstallPanel.class, "LBL_Platform_Info3")); // NOI18N
        jLabel3.setMaximumSize(new java.awt.Dimension(500, 60));
        jLabel3.setMinimumSize(new java.awt.Dimension(500, 30));
        jLabel3.setPreferredSize(new java.awt.Dimension(500, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 6, 0);
        add(jLabel3, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(bInstall, org.openide.util.NbBundle.getMessage(PlatformInstallPanel.class, "LBL_Platform_Install")); // NOI18N
        bInstall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInstallActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 6, 0);
        add(bInstall, gridBagConstraints);
        bInstall.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(PlatformInstallPanel.class, "ACSD_PlatInst_Install")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    
    private void initAccessibility() {
        getAccessibleContext().setAccessibleName(NbBundle.getMessage(PlatformInstallPanel.class, "ACSN_PlatformInstall"));
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(PlatformInstallPanel.class, "ACSD_PlatformInstall"));
    }
    
    private void bInstallActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bInstallActionPerformed
        openPlatformInstallWizard();
        if (listener != null)
            listener.actionPerformed(null);
    }//GEN-LAST:event_bInstallActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bInstall;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    
    public static void openPlatformInstallWizard() {
        PlatformsCustomizer.showCustomizer(null);
    }
    
    public static boolean isPlatformInstalled(String subType,String platformType) {
        JavaPlatform platforms[]=JavaPlatformManager.getDefault().getPlatforms(null, new Specification(platformType, null));
        if (subType==null || !(platformType.equals(CDCPlatform.PLATFORM_CDC) || platformType.equals(J2MEPlatform.SPECIFICATION_NAME)))
            return platforms.length > 0;
        else
            for (JavaPlatform platform : platforms)
            {
                if (platform instanceof J2MEPlatform)
                    if (subType.equals(((J2MEPlatform)platform).getType())) return true;
                if (platform instanceof CDCPlatform)
                    if (subType.equals(((CDCPlatform)platform).getType())) return true;
            }
        return false;    
    }
    
    public static boolean isPlatformInstalled(String platformType) {
        return JavaPlatformManager.getDefault().getPlatforms(null, new Specification(platformType, null)).length != 0;
    }
    
    public static class WizardPanel implements TemplateWizard.FinishablePanel, ActionListener {
        
        PlatformInstallPanel component;
        TemplateWizard wizard;
        Collection<ChangeListener> listeners = new ArrayList<ChangeListener>();
        boolean valid = false;
        final String platformType;
        final String platformSubType;
        
        public WizardPanel(String platType)
        {
            platformType=platType;
            platformSubType=null;
        }
        
        public WizardPanel(String name,String platType)
        {
            platformType=platType;
            platformSubType=name;
        }
        
        public void addChangeListener(final javax.swing.event.ChangeListener changeListener) {
            listeners.add(changeListener);
        }
        
        public void removeChangeListener(final javax.swing.event.ChangeListener changeListener) {
            listeners.remove(changeListener);
        }
        
        public java.awt.Component getComponent() {
            if (component == null) {
                component = new PlatformInstallPanel(platformType);
                component.setActionListener(this);
                checkValid();
            }
            return component;
        }
        
        public org.openide.util.HelpCtx getHelp() {
            return new HelpCtx(PlatformInstallPanel.class);
        }
        
        public boolean isFinishPanel() {
            return false;
        }
        
        public void showError(final String message) {
            if (wizard != null)
                wizard.putProperty("WizardPanel_errorMessage", message); // NOI18N
        }
        
        public boolean isValid() {
            final boolean valid = PlatformInstallPanel.isPlatformInstalled(platformSubType,platformType);
            if (! valid)
                showError(NbBundle.getMessage(PlatformInstallPanel.class, "ERR_Platform_NoPlatform",platformType.toUpperCase())); // NOI18N
            else
                showError(null);
            return valid;
        }
        
        public void readSettings(final Object obj) {
            wizard = (TemplateWizard) obj;
        }
        
        public void storeSettings(final Object obj) {
            wizard = (TemplateWizard) obj;
        }
        
        void fireStateChange() {
            ChangeListener[] ll;
            synchronized (this) {
                if (listeners.isEmpty())
                    return;
                ll = listeners.toArray(new ChangeListener[listeners.size()]);
            }
            final ChangeEvent ev = new ChangeEvent(this);
            for (int i = 0; i < ll.length; i++)
                ll[i].stateChanged(ev);
        }
        
        void checkValid() {
            if (isValid() != valid) {
                valid ^= true;
                fireStateChange();
            }
        }
        
        public void actionPerformed(@SuppressWarnings("unused")
		final java.awt.event.ActionEvent e) {
            checkValid();
        }
        
    }
    
}
