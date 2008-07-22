/*
 * XMLContentPanel.java
 *
 * Created on January 7, 2008, 10:37 AM
 */

package org.netbeans.modules.xml.wizard;

import java.io.File;
import java.util.Iterator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.netbeans.modules.xml.lib.Util;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;


/**
 *
 * @author  Sonali
 */
public class XMLContentPanel extends AbstractPanel {
    private DefaultComboBoxModel rootModel;
    private boolean visible= false;
    SchemaParser.SchemaInfo schemaInfo;
    
    /** Creates new form XMLContentPanel */
    public XMLContentPanel() {
        initComponents(); 
        initAccessibility();
    }
        
    public XMLContentPanel(boolean value) {
        this.visible = value;
        initComponents(); 
        initAccessibility();
    }

    private void initAccessibility() {
        Util util = Util.THIS;
        getAccessibleContext().setAccessibleDescription(titleLabel.getText());
        attributes.setMnemonic(util.getChar(
                XMLContentPanel.class, "XMLContentPanel.attributes.mne"));
        elements.setMnemonic(util.getChar(
                XMLContentPanel.class, "XMLContentPanel.elements.mne"));
        
    }
     
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        attributes = new javax.swing.JCheckBox();
        attributes.setSelected(true);
        elements = new javax.swing.JCheckBox();
        elements.setSelected(true);
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        occurSpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        depthSpinner = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel7 = new javax.swing.JLabel();
        jLabel7.setVisible(visible);
        rootElementComboBox = new javax.swing.JComboBox();
        rootElementComboBox.setVisible(visible);

        setName(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "PROP_xml_content_panel_name")); // NOI18N

        titleLabel.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.titleLabel.text")); // NOI18N

        jLabel1.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel1.text")); // NOI18N

        attributes.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.attributes.text")); // NOI18N
        attributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                attributesActionPerformed(evt);
            }
        });

        elements.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.elements.text")); // NOI18N
        elements.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                elementsActionPerformed(evt);
            }
        });

        jLabel2.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel2.text")); // NOI18N

        jLabel3.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel3.text")); // NOI18N

        jLabel4.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel4.text")); // NOI18N

        jLabel5.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel5.text")); // NOI18N

        jLabel6.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.jLabel6.text")); // NOI18N

        jLabel7.setText(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "LBL_SchemaPanel_Root_Element")); // NOI18N

        rootElementComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        rootElementComboBox.setMinimumSize(new java.awt.Dimension(60, 60));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
                    .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(attributes, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                                .add(523, 523, 523))
                            .add(layout.createSequentialGroup()
                                .add(elements, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                                .add(527, 527, 527))
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(layout.createSequentialGroup()
                                        .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                                        .add(46, 46, 46))
                                    .add(layout.createSequentialGroup()
                                        .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                                        .add(30, 30, 30))
                                    .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE))
                                .add(7, 7, 7)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(layout.createSequentialGroup()
                                        .add(17, 17, 17)
                                        .add(rootElementComboBox, 0, 65, Short.MAX_VALUE)
                                        .add(500, 500, 500))
                                    .add(layout.createSequentialGroup()
                                        .add(42, 42, 42)
                                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(depthSpinner, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE)
                                            .add(occurSpinner, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE))
                                        .add(66, 66, 66)
                                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(layout.createSequentialGroup()
                                                .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                                                .add(14, 14, 14))
                                            .add(jLabel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)))))
                            .add(layout.createSequentialGroup()
                                .add(titleLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                                .add(341, 341, 341))
                            .add(layout.createSequentialGroup()
                                .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
                                .add(623, 623, 623))
                            .add(layout.createSequentialGroup()
                                .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                                .add(601, 601, 601)))
                        .add(12, 12, 12))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(7, 7, 7)
                .add(titleLabel)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(rootElementComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(14, 14, 14)
                .add(jLabel1)
                .add(9, 9, 9)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(elements, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(attributes)
                .add(26, 26, 26)
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(occurSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel5)
                        .add(depthSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jLabel6))
                .addContainerGap(72, Short.MAX_VALUE))
        );

        attributes.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.attributes.text")); // NOI18N
        attributes.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.attributes.text")); // NOI18N
        elements.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.elements.text")); // NOI18N
        elements.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(XMLContentPanel.class, "XMLContentPanel.elements.text")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void attributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_attributesActionPerformed
        boolean attr = attributes.isSelected();
}//GEN-LAST:event_attributesActionPerformed

    private void elementsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_elementsActionPerformed
       boolean elem = elements.isSelected();
    }//GEN-LAST:event_elementsActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox attributes;
    private javax.swing.JSpinner depthSpinner;
    private javax.swing.JCheckBox elements;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSpinner occurSpinner;
    private javax.swing.JComboBox rootElementComboBox;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateModel() {
       XMLContentAttributes contentAttr = new XMLContentAttributes(model.getPrefix());
       contentAttr.setOptionalAttributes(attributes.isSelected());
       contentAttr.setOptionalElements(elements.isSelected());
       
       contentAttr.setPreferredOccurences(((SpinnerNumberModel)occurSpinner.getModel()).getNumber().intValue());
       contentAttr.setDepthPreferrence(((SpinnerNumberModel)depthSpinner.getModel()).getNumber().intValue());
       
       model.setXMLContentAttributes(contentAttr);
       if(visible) {
           Object root = rootElementComboBox.getSelectedItem();
           model.setRoot(root == null ? null : root.toString());
       }
    }

    @Override
    protected void initView() {
       attributes.setSelected(true);
       elements.setSelected(true);
       
       occurencesModel = new SpinnerNumberModel(3, 0, 10, 1);
       occurSpinner.setModel(occurencesModel);
       
       depthModel = new SpinnerNumberModel(2, 0, 10, 1);
       depthSpinner.setModel(depthModel);
       
        rootModel = new DefaultComboBoxModel();
        rootElementComboBox.setModel(rootModel);       
        
        if(getSchemaInfo() == null)
            return;
        
        if(schemaInfo.roots.size() ==0){
            //TODO: should have some error message
            //String errMsg =  NbBundle.getMessage(XMLContentPanel.class, "MSG_XMLContentPanel_No_Root");
            //templateWizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, errMsg);
            return;
        }
        Iterator it = schemaInfo.roots.iterator();
            while (it.hasNext()) {
                 String next = (String) it.next();
                 rootModel.addElement(next);
            }    
    }
    
    private SchemaParser.SchemaInfo getSchemaInfo() {
        if(schemaInfo != null)
            return schemaInfo;
        
        File f = new File(model.getPrimarySchema());
        if(f == null )
            return null;
        //for http based xsd files
        //this combo box in this panel is pnly visible for files
        //on disk, so ignore http based xsd files
       if( !f.exists() ) {
           return null;
        } 
        FileObject fobj = FileUtil.toFileObject(f);
        schemaInfo = Util.getRootElements(fobj);
        return schemaInfo;
    }

    @Override
    protected void updateView() {
        
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(XMLContentPanel.class, "PROP_xml_content_panel_name");//noi18n
    }
    
    public boolean isPanelValid() {
        //valid if root selected in previous panel
        if(model.getRoot() != null)
            return true;
        
        if(getSchemaInfo() == null)
            return false;
        
        if(schemaInfo.roots.size() == 0){
            //no root elements
            return false;
        }
        
        return true;
    }    
    
    SpinnerModel occurencesModel;
    SpinnerModel depthModel;
}
