package org.netbeans.modules.j2ee.ejbcore.ejb.wizard.jpa.dao;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.j2ee.common.J2eeProjectCapabilities;
import org.netbeans.modules.j2ee.common.Util;
import org.netbeans.modules.j2ee.core.api.support.SourceGroups;
import org.netbeans.modules.j2ee.ejbcore.ejb.wizard.session.SessionEJBWizardPanel;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.SourceGroupUISupport;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

public final class EjbFacadeVisualPanel2 extends JPanel implements DocumentListener {
    
    private final String CLASSNAME_LOCAL = NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_GeneratedFacadeLocal");
    private final String CLASSNAME_REMOTE = NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_GeneratedFacadeRemote");
    
    private WizardDescriptor wizard;
    private Project project;
    private JTextComponent packageComboBoxEditor;
    private ChangeSupport changeSupport = new ChangeSupport(this);
    private ComboBoxModel projectsList;

    public EjbFacadeVisualPanel2(Project project, WizardDescriptor wizard) {
        this.wizard = wizard;
        this.project = project;
        initComponents();
        packageComboBoxEditor = ((JTextComponent)packageComboBox.getEditor().getEditorComponent());
        Document packageComboBoxDocument = packageComboBoxEditor.getDocument();
        // TODO: add package listener
        packageComboBoxDocument.addDocumentListener(this);
        handleCheckboxes();
        
        J2eeProjectCapabilities projectCap = J2eeProjectCapabilities.forProject(project);
        if (projectCap.isEjb31LiteSupported()){
            boolean serverSupportsEJB31 = Util.getSupportedProfiles(project).contains(Profile.JAVA_EE_6_FULL);
            if (!projectCap.isEjb31Supported() && !serverSupportsEJB31){
                remoteCheckBox.setVisible(false);
                remoteCheckBox.setEnabled(false);
            }
        } else {
            localCheckBox.setSelected(true);
        }

        updateInProjectCombo(false);
    }

    private void updateInProjectCombo(boolean show) {
        if (show) {
            remoteCheckBox.setText(org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Remote_In_Project")); // NOI18N
        } else {
            remoteCheckBox.setText(org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Remote")); // NOI18N
        }
        inProjectCombo.setVisible(show);
        if (show && projectsList == null) {
            List<Project> projects = SessionEJBWizardPanel.getProjectsList(project);
            projectsList = new DefaultComboBoxModel(projects.toArray(new Project[projects.size()]));
            final ListCellRenderer defaultRenderer = inProjectCombo.getRenderer();
            if (!projects.isEmpty()){
                inProjectCombo.setRenderer(new ListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        String name = ProjectUtils.getInformation((Project)value).getDisplayName();
                        return defaultRenderer.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
                    }
                });
                inProjectCombo.setModel(projectsList);
                inProjectCombo.setSelectedIndex(0);
            }
        }
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_GeneratedSessionBeans");
    }
    
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }
    
    boolean valid(WizardDescriptor wizard) {
        // TODO: add package listener
        return true;
    }
    
    public SourceGroup getLocationValue() {
        return (SourceGroup)locationComboBox.getSelectedItem();
    }

    public String getPackage() {
        return packageComboBoxEditor.getText();
    }

    void read(WizardDescriptor settings) {
        FileObject targetFolder = Templates.getTargetFolder(settings);
        
        projectTextField.setText(ProjectUtils.getInformation(project).getDisplayName());

        SourceGroup[] sourceGroups = SourceGroups.getJavaSourceGroups(project);
        SourceGroupUISupport.connect(locationComboBox, sourceGroups);

        packageComboBox.setRenderer(PackageView.listRenderer());

        updateSourceGroupPackages();

        // set default source group and package cf. targetFolder
        SourceGroup targetSourceGroup = targetFolder !=null ? SourceGroups.getFolderSourceGroup(sourceGroups, targetFolder) : sourceGroups[0];
        if (targetSourceGroup != null) {
            locationComboBox.setSelectedItem(targetSourceGroup);
            if(targetFolder != null){
                String targetPackage = SourceGroups.getPackageForFolder(targetSourceGroup, targetFolder);
                if (targetPackage != null) {
                    packageComboBoxEditor.setText(targetPackage);
                }
            }
        }
        updateCheckboxes();

    }
    
    void store(WizardDescriptor settings) {
        try {
            Templates.setTargetFolder(settings, SourceGroups.getFolderForPackage(getLocationValue(), getPackage()));
        } catch (IOException ex) {
            Logger.getLogger("global").log(Level.INFO, null, ex);
        }
    }

    private void updateSourceGroupPackages() {
        SourceGroup sourceGroup = (SourceGroup)locationComboBox.getSelectedItem();
        ComboBoxModel model = PackageView.createListView(sourceGroup);
        if (model.getSelectedItem()!= null && model.getSelectedItem().toString().startsWith("META-INF")
                && model.getSize() > 1) { // NOI18N
            model.setSelectedItem(model.getElementAt(1));
        }
        packageComboBox.setModel(model);
    }
    
    public void insertUpdate(DocumentEvent e) {
    }

    public void removeUpdate(DocumentEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
    }

    boolean isRemote() {
        return remoteCheckBox.isSelected();
    }
    
    boolean isLocal() {
        return localCheckBox.isSelected();
    }
    
    private void handleCheckboxes() {
        createdFilesText.setText(NbBundle.getMessage(
            EjbFacadeVisualPanel2.class,
            "LBL_CreatedFIles",
            isLocal() ? CLASSNAME_LOCAL : "",
            isRemote() ? CLASSNAME_REMOTE : ""
        )); 
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        locationComboBox = new javax.swing.JComboBox();
        packageComboBox = new javax.swing.JComboBox();
        projectTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        remoteCheckBox = new javax.swing.JCheckBox();
        localCheckBox = new javax.swing.JCheckBox();
        createdFilesText = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        inProjectCombo = new javax.swing.JComboBox();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_SpecifyLocation")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Project")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Location")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Package")); // NOI18N

        packageComboBox.setEditable(true);

        projectTextField.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_CreateInterface")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(remoteCheckBox, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Remote_In_Project")); // NOI18N
        remoteCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteCheckBoxActionPerformed(evt);
            }
        });

        localCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(localCheckBox, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_Local")); // NOI18N
        localCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        localCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                localCheckBoxActionPerformed(evt);
            }
        });

        createdFilesText.setEditable(false);
        createdFilesText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        createdFilesText.setText(org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_CreatedFIles", new Object[] {"", ""})); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(EjbFacadeVisualPanel2.class, "LBL_CreatedFilesLabel")); // NOI18N

        inProjectCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inProjectComboActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3)
                    .add(jLabel4)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(projectTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                    .add(locationComboBox, 0, 570, Short.MAX_VALUE)
                    .add(packageComboBox, 0, 570, Short.MAX_VALUE)
                    .add(createdFilesText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)))
            .add(jLabel1)
            .add(jLabel6)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(localCheckBox)
                .addContainerGap(605, Short.MAX_VALUE))
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(remoteCheckBox)
                .add(18, 18, 18)
                .add(inProjectCombo, 0, 493, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(projectTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(locationComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(packageComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(createdFilesText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(localCheckBox)
                .add(0, 0, 0)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(remoteCheckBox)
                    .add(inProjectCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void localCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_localCheckBoxActionPerformed
        handleCheckboxes();
        changeSupport.fireChange();
    }//GEN-LAST:event_localCheckBoxActionPerformed

    private void remoteCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteCheckBoxActionPerformed
        handleCheckboxes();
        updateInProjectCombo(remoteCheckBox.isSelected());
        changeSupport.fireChange();        
    }//GEN-LAST:event_remoteCheckBoxActionPerformed

    private void inProjectComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inProjectComboActionPerformed
        changeSupport.fireChange();
    }//GEN-LAST:event_inProjectComboActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField createdFilesText;
    private javax.swing.JComboBox inProjectCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JCheckBox localCheckBox;
    private javax.swing.JComboBox locationComboBox;
    private javax.swing.JComboBox packageComboBox;
    private javax.swing.JTextField projectTextField;
    private javax.swing.JCheckBox remoteCheckBox;
    // End of variables declaration//GEN-END:variables

    private void updateCheckboxes() {
        //by default for ejb 3.1 no interfaces will be created
        localCheckBox.setSelected(!J2eeProjectCapabilities.forProject(project).isEjb31LiteSupported());
    }
    
    public Project getRemoteInterfaceProject() {
        if (projectsList == null) {
            return null;
        }
        return (Project)projectsList.getSelectedItem();
    }
}

