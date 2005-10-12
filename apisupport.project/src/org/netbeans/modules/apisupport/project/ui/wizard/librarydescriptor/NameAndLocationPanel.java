/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.apisupport.project.ui.wizard.librarydescriptor;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.apisupport.project.CreatedModifiedFiles;
import org.netbeans.modules.apisupport.project.ui.UIUtil;
import org.netbeans.modules.apisupport.project.ui.wizard.BasicWizardIterator;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 * Represents <em>Name and Location</em> panel in J2SE Library Descriptor Wizard.
 *
 * @author Radek Matous
 */
final class NameAndLocationPanel extends BasicWizardIterator.Panel {
    
    private NewLibraryDescriptor.DataModel data;
    
    /** Creates new NameAndLocationPanel */
    public NameAndLocationPanel(final WizardDescriptor setting, final NewLibraryDescriptor.DataModel data) {
        super(setting);
        this.data = data;
        initComponents();
        putClientProperty("NewFileWizard_Title",// NOI18N
                NbBundle.getMessage(NameAndLocationPanel.class,"LBL_LibraryWizardTitle")); // NOI18N
        
        DocumentListener dListener = new UIUtil.DocumentAdapter() {
            public void insertUpdate(DocumentEvent e) {                
                if (checkValidity()) {
                    updateData();                    
                }
            }
        };
        libraryNameVale.getDocument().addDocumentListener(dListener);
        libraryDisplayNameValue.getDocument().addDocumentListener(dListener);
        
        if (packageNameValue.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField txt = (JTextField)packageNameValue.getEditor().getEditorComponent();
            txt.getDocument().addDocumentListener(dListener);
        }
    }
    
    protected void storeToDataModel() {
        updateData();
    }
    
    private void updateData() {
        data.setPackageName(packageNameValue.getEditor().getItem().toString());
        data.setLibraryName(libraryNameVale.getText());
        data.setLibraryDisplayName(libraryDisplayNameValue.getText());
        
        CreatedModifiedFiles files = CreatedModifiedFilesProvider.createInstance(data);
        data.setCreatedModifiedFiles(files);
        
        CreatedModifiedFilesProvider.setCreatedFiles(files, createdFilesValue);
        CreatedModifiedFilesProvider.setModifiedFiles(files, modifiedFilesValue);
        checkValidity();
    }
    
    protected void readFromDataModel() {
        libraryNameVale.setText(this.data.getLibrary().getName());
        libraryDisplayNameValue.setText(this.data.getLibrary().getDisplayName());
        if (data.getPackageName() != null) {
            packageNameValue.setSelectedItem(data.getPackageName());
        }
        checkValidity();
//        updateData();
    }
    
    protected String getPanelName() {
        return NbBundle.getMessage(NameAndLocationPanel.class,"LBL_NameAndLocation_Title"); // NOI18N
    }

    
    private boolean checkValidity() {
        String lName = libraryNameVale.getText();
        String lDisplayName = libraryDisplayNameValue.getText();
        boolean pValid = true;
        if (lName.trim().length() == 0) {
            setErrorMessage(NbBundle.getMessage(NameAndLocationPanel.class,"ERR_EmptyName")); // NOI18N
            pValid = false;
        }
        
        if (pValid && lDisplayName.trim().length() == 0) {
            setErrorMessage(NbBundle.getMessage(NameAndLocationPanel.class,"ERR_EmptyDescName")); // NOI18N
            pValid = false;
        }
        
        String packageName = packageNameValue.getEditor().getItem().toString().trim();
        if (packageName.length() == 0 || !UIUtil.isValidPackageName(packageName)) { //NOI18N
            setErrorMessage(NbBundle.getMessage(NameAndLocationPanel.class,"ERR_Package_Invalid")); // NOI18N
            pValid = false;            
        }
                        
        CreatedModifiedFiles cmf = data.getCreatedModifiedFiles();
        if (pValid && cmf != null) {
            String[] modifiedFiles = cmf.getModifiedPaths();
            pValid = (modifiedFiles != null && modifiedFiles.length == 2);
            if (!pValid) {
                setErrorMessage(NbBundle.getMessage(NameAndLocationPanel.class,
                        "ERR_LibraryExists", data.getLibraryName()));
            }
        }
        
        
        if (pValid) {
            setErrorMessage(null);
        }                
        return pValid;
    }
    
    protected HelpCtx getHelp() {
        return new HelpCtx(NameAndLocationPanel.class);
    }
    
    public void addNotify() {
        super.addNotify();
        checkValidity();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        libraryName = new javax.swing.JLabel();
        libraryNameVale = new javax.swing.JTextField();
        libraryDisplayName = new javax.swing.JLabel();
        libraryDisplayNameValue = new javax.swing.JTextField();
        projectName = new javax.swing.JLabel();
        projectNameValue = new JTextField(ProjectUtils.getInformation(this.data.getProject()).getDisplayName());
        packageName = new javax.swing.JLabel();
        packageNameValue = UIUtil.createPackageComboBox(data.getSourceRootGroup());
        createdFiles = new javax.swing.JLabel();
        createdFilesValue = new javax.swing.JTextArea();
        modifiedFiles = new javax.swing.JLabel();
        modifiedFilesValue = new javax.swing.JTextArea();
        filler = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        libraryName.setLabelFor(libraryNameVale);
        org.openide.awt.Mnemonics.setLocalizedText(libraryName, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_LibraryName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 6, 12);
        add(libraryName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 6, 0);
        add(libraryNameVale, gridBagConstraints);

        libraryDisplayName.setLabelFor(libraryDisplayNameValue);
        org.openide.awt.Mnemonics.setLocalizedText(libraryDisplayName, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_LibraryDisplayName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        add(libraryDisplayName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(libraryDisplayNameValue, gridBagConstraints);

        projectName.setLabelFor(projectNameValue);
        org.openide.awt.Mnemonics.setLocalizedText(projectName, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_ProjectName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 6, 12);
        add(projectName, gridBagConstraints);

        projectNameValue.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 6, 0);
        add(projectNameValue, gridBagConstraints);

        packageName.setLabelFor(packageNameValue);
        org.openide.awt.Mnemonics.setLocalizedText(packageName, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_PackageName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        add(packageName, gridBagConstraints);

        packageNameValue.setEditable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(packageNameValue, gridBagConstraints);

        createdFiles.setLabelFor(createdFilesValue);
        org.openide.awt.Mnemonics.setLocalizedText(createdFiles, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_CreatedFiles"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(36, 0, 6, 12);
        add(createdFiles, gridBagConstraints);

        createdFilesValue.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        createdFilesValue.setColumns(20);
        createdFilesValue.setEditable(false);
        createdFilesValue.setRows(5);
        createdFilesValue.setBorder(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(36, 0, 6, 0);
        add(createdFilesValue, gridBagConstraints);

        modifiedFiles.setLabelFor(modifiedFilesValue);
        org.openide.awt.Mnemonics.setLocalizedText(modifiedFiles, java.util.ResourceBundle.getBundle("org/netbeans/modules/apisupport/project/ui/wizard/librarydescriptor/Bundle").getString("LBL_ModifiedFiles"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        add(modifiedFiles, gridBagConstraints);

        modifiedFilesValue.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        modifiedFilesValue.setColumns(20);
        modifiedFilesValue.setEditable(false);
        modifiedFilesValue.setRows(5);
        modifiedFilesValue.setToolTipText("modifiedFilesValue");
        modifiedFilesValue.setBorder(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(modifiedFilesValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(filler, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel createdFiles;
    private javax.swing.JTextArea createdFilesValue;
    private javax.swing.JLabel filler;
    private javax.swing.JLabel libraryDisplayName;
    private javax.swing.JTextField libraryDisplayNameValue;
    private javax.swing.JLabel libraryName;
    private javax.swing.JTextField libraryNameVale;
    private javax.swing.JLabel modifiedFiles;
    private javax.swing.JTextArea modifiedFilesValue;
    private javax.swing.JLabel packageName;
    private javax.swing.JComboBox packageNameValue;
    private javax.swing.JLabel projectName;
    private javax.swing.JTextField projectNameValue;
    // End of variables declaration//GEN-END:variables
    
}
