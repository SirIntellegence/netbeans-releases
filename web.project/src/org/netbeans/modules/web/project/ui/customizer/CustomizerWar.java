/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2004 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.web.project.ui.customizer;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import javax.swing.DefaultListModel;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Customizer for WAR packaging.
 */
public class CustomizerWar extends JPanel implements WebCustomizer.Panel, ListSelectionListener, HelpCtx.Provider {
        
    private Dialog dialog;
    private final AddFilter filterDlg = new AddFilter();
    private DefaultListModel dlm = new DefaultListModel();
    WebProjectProperties webProperties;
    private VisualPropertySupport vps;
    private VisualWarIncludesSupport vws;
    private ActionListener actionListener;

    /** Creates new form CustomizerCompile */
    public CustomizerWar(WebProjectProperties webProperties) {
        initComponents();
        this.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerGeneral.class, "ACS_CustomizeWAR_A11YDesc")); //NOI18N

        this.webProperties = webProperties;        
        vps = new VisualPropertySupport(webProperties);
        vws = new VisualWarIncludesSupport( webProperties.getProject(),
                                            (String) webProperties.get(WebProjectProperties.J2EE_PLATFORM),
                                            jListAddContent,
                                            jButtonAddJar,
                                            jButtonAddLib,
                                            jButtonAddProject,
                                            jButtonRemove);

        jListExContent.setModel(dlm);
    
        actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (event.getSource() == DialogDescriptor.OK_OPTION) {
                    dlm.addElement(filterDlg.getExpression());
                    setExcludeProperty();
                    closeDialog();
                }
            }
        };
        
        jListExContent.getSelectionModel().addListSelectionListener(this);
    }
    
    public void initValues() {
        vps.register(jTextFieldFileName, WebProjectProperties.WAR_NAME);
        vps.register(jCheckBoxCommpress, WebProjectProperties.WAR_COMPRESS);
        vps.register(vws, WebProjectProperties.WAR_CONTENT_ADDITIONAL);

        dlm.removeAllElements();
        String exclude = (String) webProperties.get(WebProjectProperties.BUILD_CLASSES_EXCLUDES);
        if (exclude != null) {
            StringTokenizer excludeTokenizer = new StringTokenizer(exclude, ","); //NOI18N
            while (excludeTokenizer.hasMoreElements())
                dlm.addElement(excludeTokenizer.nextToken());
        } else
                dlm.addElement("**/*.java"); //NOI18N
        
        // Set the initial state of the buttons
        valueChanged(null);
    } 
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabelFileName = new javax.swing.JLabel();
        jTextFieldFileName = new javax.swing.JTextField();
        jCheckBoxCommpress = new javax.swing.JCheckBox();
        jLabelExContent = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListExContent = new javax.swing.JList();
        jButtonAddFilter = new javax.swing.JButton();
        jButtonRemoveFilter = new javax.swing.JButton();
        jLabelAddContent = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListAddContent = new javax.swing.JList();
        jButtonAddJar = new javax.swing.JButton();
        jButtonAddLib = new javax.swing.JButton();
        jButtonAddProject = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setBorder(new javax.swing.border.EtchedBorder());
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabelFileName.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_FileName_LabelMnemonic").charAt(0));
        jLabelFileName.setLabelFor(jTextFieldFileName);
        jLabelFileName.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_FileName_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 11);
        jPanel1.add(jLabelFileName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jTextFieldFileName, gridBagConstraints);
        jTextFieldFileName.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_FileName_A11YDesc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 11, 11);
        add(jPanel1, gridBagConstraints);

        jCheckBoxCommpress.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Commpres_JCheckBox"));
        jCheckBoxCommpress.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_Commpres_LabelMnemonic").charAt(0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 11, 11);
        add(jCheckBoxCommpress, gridBagConstraints);
        jCheckBoxCommpress.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_Commpres_A11YDesc"));

        jLabelExContent.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_Content_LabelMnemonic").charAt(0));
        jLabelExContent.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Content_JLabel"));
        jLabelExContent.setLabelFor(jListExContent);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 2, 11);
        add(jLabelExContent, gridBagConstraints);

        jScrollPane1.setViewportView(jListExContent);
        jListExContent.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_Content_A11YDesc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 11);
        add(jScrollPane1, gridBagConstraints);

        jButtonAddFilter.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddFilter_LabelMnemonic").charAt(0));
        jButtonAddFilter.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddFilter_JButton"));
        jButtonAddFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddFilterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 11);
        add(jButtonAddFilter, gridBagConstraints);
        jButtonAddFilter.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddFilter_A11YDesc"));

        jButtonRemoveFilter.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Remove_JButton"));
        jButtonRemoveFilter.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_Remove_LabelMnemonic").charAt(0));
        jButtonRemoveFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveFilterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 11);
        add(jButtonRemoveFilter, gridBagConstraints);
        jButtonRemoveFilter.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_Remove_A11YDesc"));

        jLabelAddContent.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddContent_LabelMnemonic").charAt(0));
        jLabelAddContent.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddContent_JLabel"));
        jLabelAddContent.setLabelFor(jListAddContent);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(11, 12, 2, 11);
        add(jLabelAddContent, gridBagConstraints);

        jScrollPane2.setViewportView(jListAddContent);
        jListAddContent.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddContent_A11YDesc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 11, 11);
        add(jScrollPane2, gridBagConstraints);

        jButtonAddJar.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddJar_JButton"));
        jButtonAddJar.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddJar_LabelMnemonic").charAt(0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 11);
        add(jButtonAddJar, gridBagConstraints);
        jButtonAddJar.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddJar_A11YDesc"));

        jButtonAddLib.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddLib_JButton"));
        jButtonAddLib.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddLib_LabelMnemonic").charAt(0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 11);
        add(jButtonAddLib, gridBagConstraints);
        jButtonAddLib.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddLib_A11YDesc"));

        jButtonAddProject.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddProject_LabelMnemonic").charAt(0));
        jButtonAddProject.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddProject_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 11);
        add(jButtonAddProject, gridBagConstraints);
        jButtonAddProject.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddProject_A11YDesc"));

        jButtonRemove.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AdditionalRemove_LabelMnemonic").charAt(0));
        jButtonRemove.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Remove_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 11);
        add(jButtonRemove, gridBagConstraints);
        jButtonRemove.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AdditionalRemove_A11YDesc"));

    }//GEN-END:initComponents

    private void jButtonRemoveFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRemoveFilterActionPerformed
        Object[] items = jListExContent.getSelectedValues();
        for (int i = 0; i < items.length; i++)
            dlm.removeElement(items[i]);   
        
        setExcludeProperty();
    }//GEN-LAST:event_jButtonRemoveFilterActionPerformed

    private void jButtonAddFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddFilterActionPerformed
        DialogDescriptor descriptor = new DialogDescriptor(filterDlg, NbBundle.getMessage(CustomizerWar.class, "LBL_AddFilter_Title"), true, actionListener); //NOI18N
        Object [] closingOptions = {DialogDescriptor.CANCEL_OPTION};
        descriptor.setClosingOptions(closingOptions);
        dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.show();
    }//GEN-LAST:event_jButtonAddFilterActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddFilter;
    private javax.swing.JButton jButtonAddJar;
    private javax.swing.JButton jButtonAddLib;
    private javax.swing.JButton jButtonAddProject;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JButton jButtonRemoveFilter;
    private javax.swing.JCheckBox jCheckBoxCommpress;
    private javax.swing.JLabel jLabelAddContent;
    private javax.swing.JLabel jLabelExContent;
    private javax.swing.JLabel jLabelFileName;
    private javax.swing.JList jListAddContent;
    private javax.swing.JList jListExContent;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextFieldFileName;
    // End of variables declaration//GEN-END:variables
        
    private void closeDialog() {
        if (dialog != null)
            dialog.dispose();
    }

    public void valueChanged(ListSelectionEvent e) {
        jButtonRemoveFilter.setEnabled(!(jListExContent.isSelectionEmpty()));        
    }
    
    private void setExcludeProperty() {
        String exclude = dlm.toString();
        exclude = exclude.replaceAll(" ", ""); //NOI18N
        exclude = exclude.substring(1, exclude.length() -1);
        webProperties.put(WebProjectProperties.BUILD_CLASSES_EXCLUDES, exclude);
    }

    /** Help context where to find more about the paste type action.
     * @return the help context for this action
     */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(CustomizerWar.class);
    }

}
