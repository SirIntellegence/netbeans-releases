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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.xml.jaxb.ui;

import java.util.List;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import org.netbeans.modules.xml.jaxb.util.JAXBWizModuleConstants;

/**
 *
 * @author  gpatil
 */
public class FileListPanel extends javax.swing.JPanel {

    /** Creates new form FileListPanel */
    public FileListPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrlPnFiles = new javax.swing.JScrollPane();
        listFiles = new javax.swing.JList();
        btnAdd = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        scrlPnFiles.setAlignmentY(0.0F);
        scrlPnFiles.setPreferredSize(new java.awt.Dimension(350, 50));
        scrlPnFiles.setRequestFocusEnabled(false);

        listFiles.setModel(getFileListModel());
        listFiles.setPreferredSize(new java.awt.Dimension(300, 75));
        scrlPnFiles.setViewportView(listFiles);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        add(scrlPnFiles, gridBagConstraints);
        scrlPnFiles.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FileListPanel.class, "ASN_Files")); // NOI18N
        scrlPnFiles.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FileListPanel.class, "ASD_Files")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnAdd, org.openide.util.NbBundle.getMessage(FileListPanel.class, "LBL_BindingFiles_Add")); // NOI18N
        btnAdd.setToolTipText(org.openide.util.NbBundle.getMessage(FileListPanel.class, "TT_BindingFilesAddBtn")); // NOI18N
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPressedEvent(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 10);
        add(btnAdd, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(btnRemove, org.openide.util.NbBundle.getMessage(FileListPanel.class, "LBL_BindinfFiles_Remove")); // NOI18N
        btnRemove.setToolTipText(org.openide.util.NbBundle.getMessage(FileListPanel.class, "TT_BindingFilesRemoveBtn")); // NOI18N
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPressedEvent(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 10);
        add(btnRemove, gridBagConstraints);

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FileListPanel.class, "ASN_FileList")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FileListPanel.class, "ASD_FileList")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void btnPressedEvent(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPressedEvent
    if (evt.getSource() == this.btnAdd){
        String fileSelected = JAXBBindingInfoPnl.selectFileFromFileSystem(this,
                JAXBWizModuleConstants.LAST_BROWSED_SCHEMA_DIR);
        DefaultListModel model = (DefaultListModel) this.listFiles.getModel();        
        if ((fileSelected != null) && (!model.contains(fileSelected))){
            model.addElement(fileSelected);
        }
    }

    if (evt.getSource() == this.btnRemove){
        DefaultListModel model = (DefaultListModel) this.listFiles.getModel();
        int[] selectedFileIndxs = this.listFiles.getSelectedIndices();
        while (selectedFileIndxs.length > 0){
            // Remove one at a time, as ArrayIndex
            model.removeElementAt(selectedFileIndxs[0]);
            selectedFileIndxs = this.listFiles.getSelectedIndices();
        }
    }
}//GEN-LAST:event_btnPressedEvent

    private ListModel getFileListModel(){
        DefaultListModel ret = new DefaultListModel();
        for (String file: origFiles){
            ret.addElement(file);
        }
        return ret;
    }

    public void setFiles(List<String> fls){
        if (fls != null){
            this.origFiles.addAll(fls);
            this.listFiles.setModel(getFileListModel());
        }
    }

    public List<String> getFiles(){
        Vector<String> files = new Vector<String>();
        ListModel lm = this.listFiles.getModel();
        int numFiles = lm.getSize();
        if (numFiles > 0){
            for (int i=0; i < numFiles; i++){
                files.add((String) lm.getElementAt(i));
            }
        }
        return files;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnRemove;
    private javax.swing.JList listFiles;
    private javax.swing.JScrollPane scrlPnFiles;
    // End of variables declaration//GEN-END:variables

    private Vector<String> origFiles = new Vector<String>();
}
