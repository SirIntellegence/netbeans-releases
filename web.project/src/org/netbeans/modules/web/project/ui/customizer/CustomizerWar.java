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

package org.netbeans.modules.web.project.ui.customizer;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Customizer for WAR packaging.
 */
public class CustomizerWar extends JPanel implements HelpCtx.Provider {

    WebProjectProperties uiProperties;

    /** Creates new form CustomizerCompile */
    public CustomizerWar(WebProjectProperties uiProperties) {
        this.uiProperties = uiProperties;
        initComponents();
        this.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CustomizerWar.class, "ACS_CustomizeWAR_A11YDesc")); //NOI18N

        jTextFieldFileName.setDocument( uiProperties.WAR_NAME_MODEL );
        jTextFieldExContent.setDocument( uiProperties.BUILD_CLASSES_EXCLUDES_MODEL );
        uiProperties.WAR_COMPRESS_MODEL.setMnemonic( jCheckBoxCompress.getMnemonic() );
        jCheckBoxCompress.setModel( uiProperties.WAR_COMPRESS_MODEL ); 
        uiProperties.WAR_PACKAGE_MODEL.setMnemonic( jCheckBoxCreateWAR.getMnemonic() );
        jCheckBoxCreateWAR.setModel( uiProperties.WAR_PACKAGE_MODEL ); 
 
        initTableVisualProperties(jTableAddContent);
        
        WarIncludesUi.EditMediator.register( uiProperties.getProject(),
                                             jTableAddContent,
                                             jButtonAddJar.getModel(), 
                                             jButtonAddLib.getModel(), 
                                             jButtonAddProject.getModel(), 
                                             jButtonRemove.getModel());
    }
    
    private void initTableVisualProperties(JTable table) {
        WarIncludesUiSupport.ClasspathTableModel model = uiProperties.WAR_CONTENT_ADDITIONAL_MODEL;
        table.setModel(model);
        
        table.getColumnModel().getColumn(0).setHeaderValue(NbBundle.getMessage(CustomizerWar.class, "TXT_WAR_Item"));
        table.getColumnModel().getColumn(1).setHeaderValue(NbBundle.getMessage(CustomizerWar.class, "TXT_WAR_PathInWAR"));
        table.getColumnModel().getColumn(0).setCellRenderer(new WarIncludesUi.ClassPathCellRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value != null) {
                    setToolTipText(value.toString());
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        table.setRowHeight(jTableAddContent.getRowHeight() + 4);        
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);        
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));        
        // set the color of the table's JViewport
        table.getParent().setBackground(table.getBackground());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabelFileName = new javax.swing.JLabel();
        jTextFieldFileName = new javax.swing.JTextField();
        jLabelExContent = new javax.swing.JLabel();
        jTextFieldExContent = new javax.swing.JTextField();
        excludeMessage = new javax.swing.JLabel();
        jCheckBoxCompress = new javax.swing.JCheckBox();
        jCheckBoxCreateWAR = new javax.swing.JCheckBox();
        jLabelAddContent = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableAddContent = new javax.swing.JTable();
        jButtonAddJar = new javax.swing.JButton();
        jButtonAddLib = new javax.swing.JButton();
        jButtonAddProject = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabelFileName.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_FileName_LabelMnemonic").charAt(0));
        jLabelFileName.setLabelFor(jTextFieldFileName);
        jLabelFileName.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_FileName_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 11);
        jPanel1.add(jLabelFileName, gridBagConstraints);

        jTextFieldFileName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        jPanel1.add(jTextFieldFileName, gridBagConstraints);
        jTextFieldFileName.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_FileName_A11YDesc"));

        jLabelExContent.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_Content_LabelMnemonic").charAt(0));
        jLabelExContent.setLabelFor(jTextFieldExContent);
        jLabelExContent.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Content_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 11);
        jPanel1.add(jLabelExContent, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jTextFieldExContent, gridBagConstraints);
        jTextFieldExContent.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "ACS_CustomizeWAR_Content_A11YDesc"));

        excludeMessage.setLabelFor(jTextFieldExContent);
        excludeMessage.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizerWAR_ExcludeMessage"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(excludeMessage, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(jPanel1, gridBagConstraints);

        jCheckBoxCompress.setMnemonic(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Commpres_LabelMnemonic").charAt(0));
        jCheckBoxCompress.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Commpres_JCheckBox"));
        jCheckBoxCompress.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(0, 0, 0, 0)));
        jCheckBoxCompress.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(jCheckBoxCompress, gridBagConstraints);
        jCheckBoxCompress.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_Commpres_A11YDesc"));

        jCheckBoxCreateWAR.setMnemonic(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_CreateWAR_LabelMnemonic").charAt(0));
        jCheckBoxCreateWAR.setSelected(true);
        jCheckBoxCreateWAR.setText(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_CreateWAR_JCheckBox"));
        jCheckBoxCreateWAR.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(0, 0, 0, 0)));
        jCheckBoxCreateWAR.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(jCheckBoxCreateWAR, gridBagConstraints);
        jCheckBoxCreateWAR.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerWar.class, "ACS_CustomizeWAR_CreateWAR_A11YDesc"));

        jLabelAddContent.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddContent_LabelMnemonic").charAt(0));
        jLabelAddContent.setLabelFor(jTableAddContent);
        jLabelAddContent.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddContent_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 2, 0);
        add(jLabelAddContent, gridBagConstraints);

        jTableAddContent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(jTableAddContent);
        jTableAddContent.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddContent_A11YDesc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 11);
        add(jScrollPane2, gridBagConstraints);

        jButtonAddJar.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddJar_LabelMnemonic").charAt(0));
        jButtonAddJar.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddJar_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(jButtonAddJar, gridBagConstraints);
        jButtonAddJar.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddJar_A11YDesc"));

        jButtonAddLib.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddLib_LabelMnemonic").charAt(0));
        jButtonAddLib.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddLib_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(jButtonAddLib, gridBagConstraints);
        jButtonAddLib.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddLib_A11YDesc"));

        jButtonAddProject.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AddProject_LabelMnemonic").charAt(0));
        jButtonAddProject.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_AddProject_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(jButtonAddProject, gridBagConstraints);
        jButtonAddProject.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AddProject_A11YDesc"));

        jButtonRemove.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("LBL_CustomizeWAR_AdditionalRemove_LabelMnemonic").charAt(0));
        jButtonRemove.setText(NbBundle.getMessage(CustomizerWar.class, "LBL_CustomizeWAR_Remove_JButton"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        add(jButtonRemove, gridBagConstraints);
        jButtonRemove.getAccessibleContext().setAccessibleDescription(java.util.ResourceBundle.getBundle("org/netbeans/modules/web/project/ui/customizer/Bundle").getString("ACS_CustomizeWAR_AdditionalRemove_A11YDesc"));

    }
    // </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel excludeMessage;
    private javax.swing.JButton jButtonAddJar;
    private javax.swing.JButton jButtonAddLib;
    private javax.swing.JButton jButtonAddProject;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JCheckBox jCheckBoxCompress;
    private javax.swing.JCheckBox jCheckBoxCreateWAR;
    private javax.swing.JLabel jLabelAddContent;
    private javax.swing.JLabel jLabelExContent;
    private javax.swing.JLabel jLabelFileName;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTableAddContent;
    private javax.swing.JTextField jTextFieldExContent;
    private javax.swing.JTextField jTextFieldFileName;
    // End of variables declaration//GEN-END:variables
        
    /** Help context where to find more about the paste type action.
     * @return the help context for this action
     */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(CustomizerWar.class);
    }

}
