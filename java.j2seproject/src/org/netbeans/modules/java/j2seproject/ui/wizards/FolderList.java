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

package org.netbeans.modules.java.j2seproject.ui.wizards;


import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.text.MessageFormat;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.modules.java.j2seproject.ui.FoldersListSettings;


/**
 * List of source/test roots
 * @author tzezula
 */
public final class FolderList extends javax.swing.JPanel {

    public static final String PROP_FILES = "files";    //NOI18N
    public static final String PROP_LAST_USED_DIR = "lastUsedDir";  //NOI18N

    private String fcMessage;
    private File projectFolder;
    private File lastUsedFolder;

    /** Creates new form FolderList */
    public FolderList (String label, char mnemonic, String accessibleDesc, String fcMessage,
                       char addButtonMnemonic, String addButtonAccessibleDesc,
                       char removeButtonMnemonic,String removeButtonAccessibleDesc) {
        this.fcMessage = fcMessage;
        initComponents();
        this.jLabel1.setText(label);
        this.jLabel1.setDisplayedMnemonic(mnemonic);
        this.roots.getAccessibleContext().setAccessibleDescription(accessibleDesc);
        this.roots.setCellRenderer(new Renderer());
        this.roots.setModel (new DefaultListModel());
        this.roots.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    removeButton.setEnabled(roots.getSelectedIndices().length != 0);
                }
            }
        });
        this.addButton.getAccessibleContext().setAccessibleDescription(addButtonAccessibleDesc);
        this.addButton.setMnemonic (addButtonMnemonic);        
        this.removeButton.getAccessibleContext().setAccessibleDescription(removeButtonAccessibleDesc);
        this.removeButton.setMnemonic (removeButtonMnemonic);
        this.removeButton.setEnabled(false);
    }
    
    public void setProjectFolder (File projectFolder) {
        this.projectFolder = projectFolder;
    }

    public File[] getFiles () {
        Object[] files = ((DefaultListModel)this.roots.getModel()).toArray();
        File[] result = new File[files.length];
        System.arraycopy(files, 0, result, 0, files.length);
        return result;
    }

    public void setFiles (File[] files) {
        DefaultListModel model = ((DefaultListModel)this.roots.getModel());
        model.clear();
        for (int i=0; i<files.length; i++) {
            model.addElement (files[i]);
        }
        if (files.length>0) {
            this.roots.setSelectedIndex(0);
        }
    }
    
    public void setLastUsedDir (File lastUsedDir) {
        if (this.lastUsedFolder == null ? lastUsedDir != null : !this.lastUsedFolder.equals(lastUsedDir)) {
            File oldValue = this.lastUsedFolder;
            this.lastUsedFolder = lastUsedDir;
            this.firePropertyChange(PROP_LAST_USED_DIR, oldValue, this.lastUsedFolder);
        }
    }
    
    public File getLastUsedDir () {
        return this.lastUsedFolder;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        roots = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jLabel1.setLabelFor(roots);
        jLabel1.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        add(jLabel1, gridBagConstraints);

        jScrollPane1.setViewportView(roots);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        add(jScrollPane1, gridBagConstraints);

        addButton.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/java/j2seproject/ui/wizards/Bundle").getString("CTL_AddFolder"));
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(addButton, gridBagConstraints);

        removeButton.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/java/j2seproject/ui/wizards/Bundle").getString("CTL_RemoveFolder"));
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(removeButton, gridBagConstraints);

    }//GEN-END:initComponents

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        Object[] selection = this.roots.getSelectedValues ();
        for (int i=0; i<selection.length; i++) {
            ((DefaultListModel)this.roots.getModel()).removeElement (selection[i]);
        }
        this.firePropertyChange(PROP_FILES, null, null);
    }//GEN-LAST:event_removeButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileUtil.preventFileChooserSymlinkTraversal(chooser, null);
        chooser.setDialogTitle(this.fcMessage);
        chooser.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        if (this.lastUsedFolder != null && this.lastUsedFolder.isDirectory()) {
            chooser.setCurrentDirectory (this.lastUsedFolder);
        }        
        else if (this.projectFolder != null && this.projectFolder.isDirectory()) {
            chooser.setCurrentDirectory (this.projectFolder);            
        }                        
        if (chooser.showOpenDialog(this)== JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            int[] indecesToSelect = new int[files.length];
            DefaultListModel model = (DefaultListModel)this.roots.getModel();
            for (int i=0, index=model.size(); i<files.length; i++, index++) {
                File normalizedFile = FileUtil.normalizeFile(files[i]);
                int pos = model.indexOf (normalizedFile);
                if (pos == -1) {
                    model.addElement (normalizedFile);
                    indecesToSelect[i] = index;
                }
                else {
                    indecesToSelect[i] = pos;
                }

            }
            this.roots.setSelectedIndices(indecesToSelect);
            this.firePropertyChange(PROP_FILES, null, null);
            File cd = chooser.getCurrentDirectory();
            if (cd != null) {
                this.setLastUsedDir(FileUtil.normalizeFile(cd));
            }
        }
    }//GEN-LAST:event_addButtonActionPerformed
    
    
    private static class Renderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            File f = (File) value;
            Project p = FileOwnerQuery.getOwner(f.toURI());
            String message = f.getAbsolutePath();
            if (p != null) {
                ProjectInformation info = (ProjectInformation) p.getLookup().lookup(ProjectInformation.class);
                if (info != null) {
                    String projectName = info.getDisplayName();
                    if (projectName != null) {
                        message = MessageFormat.format (NbBundle.getMessage(FolderList.class,"TXT_RootOwnedByProject"), new Object[] {
                            message,
                            projectName});
                    }
                }
            }
            Component result = super.getListCellRendererComponent(list, message, index, isSelected, cellHasFocus);
            if (p!=null) {
                result.setForeground (new Color(164,0,0));
            }
            return result;
        }
        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton removeButton;
    private javax.swing.JList roots;
    // End of variables declaration//GEN-END:variables
    
}
