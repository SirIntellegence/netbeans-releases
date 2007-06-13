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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ant.AntArtifact;
import org.netbeans.api.project.ant.AntArtifactQuery;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.modules.web.project.ui.FoldersListSettings;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * Accessory component used in the ProjectChooser for choosing project
 * artifacts.
 * @author Petr Hrebejk
 */
public class AntArtifactChooser extends JPanel implements PropertyChangeListener {
    
    // XXX to become an array later
    private String artifactType;
    
    /** Creates new form JarArtifactChooser */
    public AntArtifactChooser( String artifactType, JFileChooser chooser ) {
        this.artifactType = artifactType;
        
        initComponents();
        jListArtifacts.setModel( new DefaultListModel() );
        chooser.addPropertyChangeListener( this );
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabelName = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jLabelJarFiles = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListArtifacts = new javax.swing.JList();

        setLayout(new java.awt.GridBagLayout());

        jLabelName.setLabelFor(jTextFieldName);
        org.openide.awt.Mnemonics.setLocalizedText(jLabelName, org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectName_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 2, 0);
        add(jLabelName, gridBagConstraints);

        jTextFieldName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 6, 0);
        add(jTextFieldName, gridBagConstraints);
        jTextFieldName.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectName_JLabel"));

        jLabelJarFiles.setLabelFor(jListArtifacts);
        org.openide.awt.Mnemonics.setLocalizedText(jLabelJarFiles, org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectJarFiles_JLabel"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 2, 0);
        add(jLabelJarFiles, gridBagConstraints);

        jScrollPane1.setViewportView(jListArtifacts);
        jListArtifacts.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectJarFiles_JLabel"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        add(jScrollPane1, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents
    
    
    public void propertyChange(PropertyChangeEvent e) {
        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName())) {
            // We have to update the Accessory
            JFileChooser chooser = (JFileChooser) e.getSource();
            File dir = chooser.getSelectedFile(); // may be null (#46744)
            Project project = getProject(dir); // may be null
            populateAccessory(project);
        }
    }
    
    private Project getProject( File projectDir ) {
        
        if (projectDir == null) { // #46744
            return null;
        }
        
        try {            
            File normProjectDir = FileUtil.normalizeFile(projectDir);
            FileObject fo = FileUtil.toFileObject(normProjectDir);
            if (fo != null) {
                return ProjectManager.getDefault().findProject(fo);
            }
        } catch (IOException e) {
            Logger.getLogger("global").log(Level.INFO, null, e);
            // Return null
        }
        
        return null;
    }    
    
    /**
     * Set up GUI fields according to the requested project.
     * @param project a subproject, or null
     */
    private void populateAccessory( Project project ) {
        
        DefaultListModel model = (DefaultListModel)jListArtifacts.getModel();
        model.clear();
        jTextFieldName.setText(project == null ? "" : ProjectUtils.getInformation(project).getDisplayName()); //NOI18N
        
        if ( project != null ) {
                        
            AntArtifact artifacts[] = AntArtifactQuery.findArtifactsByType( project, artifactType );
        
            for( int i = 0; i < artifacts.length; i++ ) {
                URI uris[] = artifacts[i].getArtifactLocations();
                for( int y = 0; y < uris.length; y++ ) {
                    model.addElement( new ArtifactItem(artifacts[i], uris[y]));
                }
            }
            jListArtifacts.setSelectionInterval(0, model.size());
        }
        
    }
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelJarFiles;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JList jListArtifacts;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextFieldName;
    // End of variables declaration//GEN-END:variables

    
    /** Shows dialog with the artifact chooser 
     * @return null if canceled selected jars if some jars selected
     */
    public static ArtifactItem[] showDialog( String artifactType, Project master, Component parent ) {
        
        JFileChooser chooser = ProjectChooser.projectChooser();
        chooser.setDialogTitle( NbBundle.getMessage( AntArtifactChooser.class, "LBL_AACH_Title" ) ); // NOI18N
        chooser.setApproveButtonText( NbBundle.getMessage( AntArtifactChooser.class, "LBL_AACH_SelectProject" ) ); // NOI18N
        
        AntArtifactChooser accessory = new AntArtifactChooser( artifactType, chooser );
        chooser.setAccessory( accessory );
        chooser.setCurrentDirectory (FoldersListSettings.getDefault().getLastUsedArtifactFolder());

        int option = chooser.showOpenDialog( parent ); // Show the chooser
              
        if ( option == JFileChooser.APPROVE_OPTION ) {

            File dir = chooser.getSelectedFile();
            dir = FileUtil.normalizeFile (dir);
            Project selectedProject = accessory.getProject( dir );

            if ( selectedProject == null ) {
                return null;
            }
            
            if ( selectedProject.getProjectDirectory().equals( master.getProjectDirectory() ) ) {
                DialogDisplayer.getDefault().notify( new NotifyDescriptor.Message( 
                    NbBundle.getMessage( AntArtifactChooser.class, "MSG_AACH_RefToItself" ),
                    NotifyDescriptor.INFORMATION_MESSAGE ) );
                return null;
            }
            
            if ( ProjectUtils.hasSubprojectCycles( master, selectedProject ) ) {
                DialogDisplayer.getDefault().notify( new NotifyDescriptor.Message( 
                    NbBundle.getMessage( AntArtifactChooser.class, "MSG_AACH_Cycles" ),
                    NotifyDescriptor.INFORMATION_MESSAGE ) );
                return null;
            }
            
            FoldersListSettings.getDefault().setLastUsedArtifactFolder (FileUtil.normalizeFile(chooser.getCurrentDirectory()));
            
            Object[] tmp = new Object[accessory.jListArtifacts.getModel().getSize()];
            int count = 0;
            for(int i = 0; i < tmp.length; i++) {
                if (accessory.jListArtifacts.isSelectedIndex(i)) {
                    tmp[count] = accessory.jListArtifacts.getModel().getElementAt(i);
                    count++;
                }
            }
            ArtifactItem artifactItems[] = new ArtifactItem[count];
            System.arraycopy(tmp, 0, artifactItems, 0, count);
            return artifactItems;
        }
        else {
            return null; 
        }
                
    }
       
    /**
     * Pair of AntArtifact and one of jars it produces.
     */
    public static class ArtifactItem {
        
        private AntArtifact artifact;
        private URI artifactURI;
        
        public ArtifactItem(AntArtifact artifact, URI artifactURI) {
            this.artifact = artifact;
            this.artifactURI = artifactURI;
        }
        
        public AntArtifact getArtifact() {
            return artifact;
        }
        
        public URI getArtifactURI() {
            return artifactURI;
        }
        
        public String toString() {
            return artifactURI.toString();
        }
        
    }
}
