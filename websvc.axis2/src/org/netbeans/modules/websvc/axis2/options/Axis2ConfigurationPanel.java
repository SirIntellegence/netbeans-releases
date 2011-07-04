/*
 * ConfigurationPanel.java
 *
 * Created on December 12, 2007, 1:22 PM
 */

package org.netbeans.modules.websvc.axis2.options;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.websvc.axis2.AxisUtils;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author  mkuchtiak
 */
public class Axis2ConfigurationPanel extends javax.swing.JPanel {
    
    private String previousDeployDirectory;
    private boolean modelChanged;
    
    public Axis2ConfigurationPanel() {
        
    }
    
    /** Creates new form Axis2ConfigurationPanel */
    public Axis2ConfigurationPanel(String axisDeploy) {
        previousDeployDirectory = axisDeploy;
        initComponents();
        axisDeployTf.setText(axisDeploy);
        Preferences preferences = AxisUtils.getPreferences();
        String axisUrl = preferences.get("AXIS_URL",""); // NOI18N
        axisUrlTf.setText(axisUrl.length() == 0 ? "http://localhost:8080/axis2" : axisUrl); //NOI18N
        String tomcatUser = preferences.get("TOMCAT_MANAGER_USER",""); // NOI18N
        if (tomcatUser.length() > 0) {
            tfTomcatUser.setEditable(true);
            tfTomcatPassword.setEditable(true);
            cbTomcatManager.setSelected(true);
            tfTomcatUser.setText(tomcatUser);
        }
        String tomcatPassword = preferences.get("TOMCAT_MANAGER_PASSWORD",""); // NOI18N
        if (tomcatPassword.length() > 0) tfTomcatPassword.setText(tomcatPassword);
        cbTomcatManager.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (cbTomcatManager.isSelected()) {
                    tfTomcatUser.setEditable(true);
                    tfTomcatPassword.setEditable(true);
                } else {
                    tfTomcatUser.setEditable(false);
                    tfTomcatPassword.setEditable(false);                    
                }
                modelChanged = true; 
            }
            
        });
        DocumentListener docListener = new Axis2ConfigDocumentListener();
        axisUrlTf.getDocument().addDocumentListener(docListener);
        axisDeployTf.getDocument().addDocumentListener(docListener);
        tfTomcatUser.getDocument().addDocumentListener(docListener);
        tfTomcatPassword.getDocument().addDocumentListener(docListener);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        deploymentPanel = new javax.swing.JPanel();
        axisDeployTf = new javax.swing.JTextField();
        axisDeployLabel = new javax.swing.JLabel();
        browseButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        axisUrlTf = new javax.swing.JTextField();
        cbTomcatManager = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        tfTomcatUser = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        tfTomcatPassword = new javax.swing.JPasswordField();
        jLabel6 = new javax.swing.JLabel();

        axisDeployLabel.setLabelFor(axisDeployTf);
        org.openide.awt.Mnemonics.setLocalizedText(axisDeployLabel, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.axisDeployLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton1, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.browseButton1.text")); // NOI18N
        browseButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButton1ActionPerformed(evt);
            }
        });

        jLabel2.setText(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.jLabel2.text")); // NOI18N

        jLabel3.setLabelFor(axisUrlTf);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbTomcatManager, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.cbTomcatManager.text")); // NOI18N

        jLabel4.setLabelFor(tfTomcatUser);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.jLabel4.text")); // NOI18N

        tfTomcatUser.setEditable(false);

        jLabel5.setLabelFor(tfTomcatPassword);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.jLabel5.text")); // NOI18N

        tfTomcatPassword.setEditable(false);

        jLabel6.setText(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.jLabel6.text")); // NOI18N

        javax.swing.GroupLayout deploymentPanelLayout = new javax.swing.GroupLayout(deploymentPanel);
        deploymentPanel.setLayout(deploymentPanelLayout);
        deploymentPanelLayout.setHorizontalGroup(
            deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deploymentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbTomcatManager)
                    .addGroup(deploymentPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(deploymentPanelLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfTomcatPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(deploymentPanelLayout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfTomcatUser, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(deploymentPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(axisUrlTf, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(deploymentPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, deploymentPanelLayout.createSequentialGroup()
                            .addComponent(axisDeployTf, javax.swing.GroupLayout.DEFAULT_SIZE, 581, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(browseButton1))
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(axisDeployLabel))
                    .addContainerGap()))
        );
        deploymentPanelLayout.setVerticalGroup(
            deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deploymentPanelLayout.createSequentialGroup()
                .addGap(121, 121, 121)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47)
                .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(axisUrlTf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(cbTomcatManager)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(tfTomcatUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfTomcatPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap(82, Short.MAX_VALUE))
            .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(deploymentPanelLayout.createSequentialGroup()
                    .addGap(30, 30, 30)
                    .addComponent(axisDeployLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(deploymentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(browseButton1)
                        .addComponent(axisDeployTf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(304, Short.MAX_VALUE)))
        );

        axisDeployTf.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.axisDeployTf.AccessibleContext.accessibleDescription")); // NOI18N
        browseButton1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.browseButton1.AccessibleContext.accessibleDescription")); // NOI18N
        axisUrlTf.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.axisUrlTf.AccessibleContext.accessibleDescription")); // NOI18N
        cbTomcatManager.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.cbTomcatManager.AccessibleContext.accessibleDescription")); // NOI18N
        tfTomcatUser.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.tfTomcatUser.AccessibleContext.accessibleDescription")); // NOI18N
        tfTomcatPassword.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.tfTomcatPassword.AccessibleContext.accessibleDescription")); // NOI18N

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.deploymentPanel.TabConstraints.tabTitle"), deploymentPanel); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
        );

        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "Axis2ConfigurationPanel.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void browseButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButton1ActionPerformed
// TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(previousDeployDirectory);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileFilter fileFilter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".war"); //NOI18N
            }

            @Override
            public String getDescription() {
                return org.openide.util.NbBundle.getMessage(Axis2ConfigurationPanel.class, "DESC_FileFilter");
            }
            
        };
        chooser.setFileFilter(fileFilter);
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File axisDir = chooser.getSelectedFile();
            axisDeployTf.setText(axisDir.getAbsolutePath());
            previousDeployDirectory = axisDir.getPath();
        } 
}//GEN-LAST:event_browseButton1ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel axisDeployLabel;
    private javax.swing.JTextField axisDeployTf;
    private javax.swing.JTextField axisUrlTf;
    private javax.swing.JButton browseButton1;
    private javax.swing.JCheckBox cbTomcatManager;
    private javax.swing.JPanel deploymentPanel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPasswordField tfTomcatPassword;
    private javax.swing.JTextField tfTomcatUser;
    // End of variables declaration//GEN-END:variables
    
    public String getAxisDeploy() {
        return axisDeployTf.getText().trim();
    }
    
    public String getAxisUrl() {
        return axisUrlTf.getText().trim();
    }
    
    public String getTomcatManagerUsername() {
        return (cbTomcatManager.isSelected()?tfTomcatUser.getText():null);
    }
    public String getTomcatManagerPassword() {
        return (cbTomcatManager.isSelected()?new String(tfTomcatPassword.getPassword()):null);
    }
    
    boolean isChanged() {
        return modelChanged;
    }
    void setChanged(boolean changed) {
        modelChanged = changed;
    }
    
    private class Axis2ConfigDocumentListener implements DocumentListener {

        public void insertUpdate(DocumentEvent e) {
            modelChanged = true;
        }

        public void removeUpdate(DocumentEvent e) {
            modelChanged = true;
        }

        public void changedUpdate(DocumentEvent e) {
            modelChanged = true;
        }
        
    }
    
}
