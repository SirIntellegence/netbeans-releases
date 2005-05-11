/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 2004-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.jmx.configwizard;

import java.awt.Component;
import java.util.ResourceBundle;
import javax.swing.event.*;

import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.loaders.TemplateWizard;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.awt.Mnemonics;

import org.netbeans.modules.jmx.WizardConstants;
import org.netbeans.modules.jmx.GenericWizardPanel;
import org.netbeans.modules.jmx.WizardHelpers;
import java.awt.Container;
import java.io.File;
import javax.swing.JTextField;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;

/**
 *
 * Class handling the graphical part of the standard Agent wizard panel
 *
 */
public class ConfigPanel extends javax.swing.JPanel
{
    private ConfigWizardPanel wiz;
    private ResourceBundle bundle;
    
    //=====================================================================
    // Create the wizard panel component and set up some basic properties.
    //=====================================================================
    public ConfigPanel (ConfigWizardPanel wiz) 
    {
        this.wiz = wiz;
        bundle = NbBundle.getBundle(ConfigPanel.class);
        initComponents ();
        Mnemonics.setLocalizedText(rmiAccessFileJLabel,
                     bundle.getString("LBL_RMI_Access_File"));//NOI18N
        Mnemonics.setLocalizedText(rmiPasswordFileJLabel,
                     bundle.getString("LBL_RMI_Password_File"));//NOI18N
        
        // Provide a name in the title bar.
        setName(NbBundle.getMessage(ConfigPanel.class, 
                                    "LBL_Config_Panel"));        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        northPanel = new javax.swing.JPanel();
        northWestCenterPanel = new javax.swing.JPanel();
        rmiAccessFileJLabel = new javax.swing.JLabel();
        rmiPasswordFileJLabel = new javax.swing.JLabel();
        rmiAccessFileJTextField = new javax.swing.JTextField();
        rmiPasswordFileJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.BorderLayout());

        northPanel.setLayout(new java.awt.BorderLayout());

        northWestCenterPanel.setLayout(new java.awt.GridBagLayout());

        rmiAccessFileJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rmiAccessFileJLabel.setLabelFor(rmiAccessFileJTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        northWestCenterPanel.add(rmiAccessFileJLabel, gridBagConstraints);

        rmiPasswordFileJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        rmiPasswordFileJLabel.setLabelFor(rmiPasswordFileJTextField);
        rmiPasswordFileJLabel.setAlignmentX(0.5F);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 12);
        northWestCenterPanel.add(rmiPasswordFileJLabel, gridBagConstraints);

        rmiAccessFileJTextField.setEditable(false);
        rmiAccessFileJTextField.setName("rmiAccessFileJTextField");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        northWestCenterPanel.add(rmiAccessFileJTextField, gridBagConstraints);

        rmiPasswordFileJTextField.setEditable(false);
        rmiPasswordFileJTextField.setName("rmiPasswordFileJTextField");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        northWestCenterPanel.add(rmiPasswordFileJTextField, gridBagConstraints);

        northPanel.add(northWestCenterPanel, java.awt.BorderLayout.CENTER);

        add(northPanel, java.awt.BorderLayout.NORTH);

    }
    // </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel northPanel;
    private javax.swing.JPanel northWestCenterPanel;
    private javax.swing.JLabel rmiAccessFileJLabel;
    private javax.swing.JTextField rmiAccessFileJTextField;
    private javax.swing.JLabel rmiPasswordFileJLabel;
    private javax.swing.JTextField rmiPasswordFileJTextField;
    // End of variables declaration//GEN-END:variables
    
    /**
     *
     * Class handling the standard Agent wizard panel
     *
     */
    public static class ConfigWizardPanel extends GenericWizardPanel 
            implements org.openide.WizardDescriptor.FinishablePanel
    {    
        private ConfigPanel panel = null;
        private String projectLocation   = null;
        private TemplateWizard templateWiz = null;
        private WizardDescriptor.Panel targetWiz = null;
        private JTextField createdFileTextField = null;
        private JTextField projectTextField = null;
        private JTextField nameTextField = null;
        private transient ResourceBundle bundle;
        
        public Component getComponent () { return getPanel(); }
        
        public String getProjectLocation() { return projectLocation; }
        
        private ConfigPanel getPanel() 
        {
            if (panel == null) {
                panel = new ConfigPanel(this);
            }
            return panel;
        }
        
        //implementation of the FinishablePanel Interface
        //provides the Finish Button to be always enabled 
        public boolean isFinishPanel() { return false;}

        public boolean isValid ()
        {
            if (WizardHelpers.fileExists(
                    getPanel().rmiAccessFileJTextField.getText())) {
                setErrorMsg("The file " + 
                        WizardHelpers.getFileName(
                            getPanel().rmiAccessFileJTextField.getText()) +
                        " already exists.");
                return false;
            } else if (WizardHelpers.fileExists(
                    getPanel().rmiPasswordFileJTextField.getText())) {
                setErrorMsg("The file " + 
                        WizardHelpers.getFileName(
                            getPanel().rmiPasswordFileJTextField.getText()) +
                        " already exists.");
                return false;
            } 
            setErrorMsg("");
            return true;
        }
        
        public void fireEvent() {
            fireChangeEvent();
        }
        
        private String getProjectDisplayedName() {
            Project project = Templates.getProject(templateWiz);
            return ProjectUtils.getInformation(project).getDisplayName();
        }
        
        private void initTargetComponentDef(Component root) {
            if (root.getClass().equals(JTextField.class)) {
                JTextField rootTextField = (JTextField) root;
                if (rootTextField.isEditable()) {
                    if (nameTextField == null) {
                        nameTextField = ((JTextField) root);
                    }
                } else if (!rootTextField.getText().equals(getProjectDisplayedName())) {
                    if (projectTextField != null) {
                        createdFileTextField = ((JTextField) root);
                    }
                } else {
                    projectTextField = ((JTextField) root);
                }
            } else if ((root instanceof Container) && (root != getComponent())) {
                Component[] components = ((Container) root).getComponents();
                for (int i = 0; i < components.length; i++) {
                    initTargetComponentDef(components[i]);
                }
            }
        }
        
        /**
         * Displays the given message in the wizard's message area.
         *
         * @param  message  message to be displayed, or <code>null</code>
         *                  if the message area should be cleared
         */
        private void setErrorMsg(String message) {
            if (templateWiz != null) {
                templateWiz.putProperty(WizardConstants.WIZARD_ERROR_MESSAGE, 
                        message);    //NOI18N
            }
        }
        
        public void setListenerEnabled(
                final WizardDescriptor.Panel targetWiz,
                final WizardDescriptor.Panel optionsWiz,
                final TemplateWizard wizard) {
            bundle = NbBundle.getBundle(JMXConfigWizardIterator.class);
            templateWiz = wizard;
            this.targetWiz = targetWiz;
            initTargetComponentDef(targetWiz.getComponent());
            targetWiz.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    optionsWiz.storeSettings(wizard);
                    optionsWiz.readSettings(wizard);
                }
            });
        }

        //=====================================================================
        // Called to read information from the wizard map in order to populate
        // the GUI correctly.
        //=====================================================================
        public void readSettings (Object settings) 
        {
            templateWiz = (TemplateWizard) settings;
            String filePath = (String) 
                templateWiz.getProperty(WizardConstants.PROP_CONFIG_FILE_PATH);
            String targetName = templateWiz.getTargetName();
            String rmiAccessFileName = "";
            String rmiPasswordFileName = "";
            if ((targetName != null) && (filePath != null)) {
                rmiAccessFileName = filePath + File.separator + targetName +
                        "." + WizardConstants.ACCESS_EXT;
                rmiPasswordFileName = filePath + File.separator + targetName + 
                        "." + WizardConstants.PASSWORD_EXT;
            }
            getPanel().rmiAccessFileJTextField.setText(rmiAccessFileName);
            getPanel().rmiPasswordFileJTextField.setText(rmiPasswordFileName);
        }
        
        public void storeSettings(Object settings) {
            templateWiz = (TemplateWizard) settings;
            if (createdFileTextField != null) {
                String filePath = WizardHelpers.getFolderPath(
                        createdFileTextField.getText());
                templateWiz.putProperty(
                        WizardConstants.PROP_CONFIG_FILE_PATH,filePath);
            }
            if (nameTextField != null) {
                Templates.setTargetName(templateWiz,nameTextField.getText());
            }
        }
              
        public HelpCtx getHelp() {
           return new HelpCtx("mgt_properties");  
        }   
        
    }

}
