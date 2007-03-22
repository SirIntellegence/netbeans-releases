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

package org.netbeans.modules.websvc.design.view.actions;

import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.netbeans.modules.websvc.design.schema2java.OperationGeneratorHelper;
import org.netbeans.modules.websvc.design.util.Util;
import org.netbeans.modules.xml.schema.model.GlobalElement;
import org.netbeans.modules.xml.schema.model.GlobalSimpleType;
import org.netbeans.modules.xml.schema.model.Import;
import org.netbeans.modules.xml.schema.model.Schema;
import org.netbeans.modules.xml.schema.model.SchemaModel;
import org.netbeans.modules.xml.schema.model.SchemaModelFactory;
import org.netbeans.modules.xml.wsdl.model.Definitions;
import org.netbeans.modules.xml.wsdl.model.Types;
import org.netbeans.modules.xml.wsdl.model.WSDLModel;
import org.netbeans.modules.xml.xam.locator.CatalogModelException;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 *
 * @author  mkuchtiak
 */
public class AddOperationFromSchemaPanel extends javax.swing.JPanel {
    private File wsdlFile;
    private List<URL> schemaFiles;
    private String parameterType, returnType, faultType;
    private Map<String, Import> map = new HashMap<String, Import>();
    private Map<String, Schema> map1 = new HashMap<String, Schema>();
    
    /** Creates new form NewJPanel */
    public AddOperationFromSchemaPanel(File wsdlFile) {
        this();
        this.wsdlFile=wsdlFile;
        //jTextField2.setText(NbBundle.getMessage(AddOperationFromSchemaPanel.class, "TXT_DefaultSchmas", wsdlFile.getName()));
        //jTextField2.setEditable(false);
        browseButton.setEnabled(false);
        try{
            populate();
        }catch(CatalogModelException e){
            ErrorManager.getDefault().notify(e);
        }
        schemaCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                schemaComboChanged(evt);
            }
        });
        SchemaPanelListCellRenderer renderer = new SchemaPanelListCellRenderer();
        parmCombo.setRenderer(renderer);
        returnCombo.setRenderer(renderer);
        faultCombo.setRenderer(renderer);
    }   
        /** Creates new form NewJPanel */
        public AddOperationFromSchemaPanel() {
            initComponents();
            opNameTxt.setText(NbBundle.getMessage(AddOperationFromSchemaPanel.class, "TXT_DefaultOperationName"));
        }
        
        public File getWsdlFile() {
            return wsdlFile;
        }
        
        public String getOperationName(){
            return opNameTxt.getText();
        }
        
        public List<URL> getSchemaFiles() {
            return schemaFiles;
        }
        
        public Object getParameterType() {
            Object paramType = parmCombo.getSelectedItem();
            if (paramType instanceof String && ((String)paramType).startsWith("xsd:")) {
                return getPrimitiveType(((String)paramType).substring(4));
            } else return paramType;
            /*
            List<GlobalElement> list = new ArrayList<GlobalElement>();
            Object[] objs = parmCombo.getSelectedObjects();
            for(int i = 0; i < objs.length; i++){
                list.add((GlobalElement)objs[i]);
            }
            return list.<GlobalElement>toArray(new GlobalElement[list.size()]);
            */
        }
        
        public Object getReturnType() {
            Object returnType = returnCombo.getSelectedItem();
            if (returnType instanceof String && ((String)returnType).startsWith("xsd:")) {
                return getPrimitiveType(((String)returnType).substring(4));
            } else return returnType;
        }
        
        public Object getFaultType() {
            Object faultType = faultCombo.getSelectedItem();
            if (faultType instanceof String && ((String)faultType).startsWith("xsd:")) {
                return getPrimitiveType(((String)faultType).substring(4));
            } else return faultType;
        }
        
        private void populate()throws CatalogModelException {
            WSDLModel model = Util.getWSDLModel(FileUtil.toFileObject(wsdlFile), true);
            Definitions definitions = model.getDefinitions();
            Types types = definitions.getTypes();
            Collection<Schema> schemas = types.getSchemas();
            for(Schema schema : schemas) {
                // populate with internal schema
                Collection<GlobalElement> elements = schema.getElements();
                if (elements.size()>0) {
                    schemaCombo.addItem(schema.getTargetNamespace());
                    map1.put(schema.getTargetNamespace(), schema);
                }
                // populate with imported schemas
                Collection<Import> importedSchemas = schema.getImports();
                for(Import importedSchema : importedSchemas){
                    String schemaLocation = importedSchema.getSchemaLocation();
                    map.put(schemaLocation, importedSchema);
                    schemaCombo.addItem(schemaLocation);
                }
            }
            
            String selectedItem = (String)schemaCombo.getItemAt(0);
            if (selectedItem!=null)
                populateWithElements(selectedItem);
        }
        
        
        class SchemaPanelListCellRenderer extends JLabel implements ListCellRenderer{
            public Component getListCellRendererComponent(JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                    if (value instanceof GlobalElement) {
                        GlobalElement el = (GlobalElement)value;
                        String text = "{"+el.getModel().getEffectiveNamespace(el)+"}:"+el.getName(); //NOI18N
                        setText(text);
                    } else if (value instanceof String) {
                        setText((String)value);
                    }
                return this;
            }
        }
        
        
        /** This method is called from within the constructor to
         * initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        opNameTxt = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        parmCombo = new javax.swing.JComboBox();
        returnCombo = new javax.swing.JComboBox();
        faultCombo = new javax.swing.JComboBox();
        schemaCombo = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();

        jLabel1.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_OperationName_mnem").charAt(0));
        jLabel1.setLabelFor(opNameTxt);
        jLabel1.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_OperationName")); // NOI18N

        jLabel2.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_SchemaFiles_mnem").charAt(0));
        jLabel2.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_SchemaFiles")); // NOI18N

        jLabel3.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_ParameterTypes_mnem").charAt(0));
        jLabel3.setLabelFor(parmCombo);
        jLabel3.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_ParameterTypes")); // NOI18N

        jLabel4.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_ReturnType_mnem").charAt(0));
        jLabel4.setLabelFor(returnCombo);
        jLabel4.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_ReturnType")); // NOI18N

        jLabel5.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_FaultType_mnem").charAt(0));
        jLabel5.setLabelFor(faultCombo);
        jLabel5.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_FaultType")); // NOI18N

        browseButton.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("LBL_Browse_mnem").charAt(0));
        browseButton.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_Browse")); // NOI18N

        jLabel6.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "LBL_BindingStyle")); // NOI18N

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("RB_DOCUMENT_LITERAL_mnem").charAt(0));
        jRadioButton1.setSelected(true);
        jRadioButton1.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "RB_DOCUMENT_LITERAL")); // NOI18N
        jRadioButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/websvc/design/view/actions/Bundle").getString("RB_RPC_LITERAL_mnem").charAt(0));
        jRadioButton2.setText(org.openide.util.NbBundle.getMessage(AddOperationFromSchemaPanel.class, "RB_RPC_LITERAL")); // NOI18N
        jRadioButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, browseButton)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel4)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel5)
                            .add(jLabel6))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(jRadioButton1)
                                .add(76, 76, 76)
                                .add(jRadioButton2))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, opNameTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, parmCombo, 0, 487, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, faultCombo, 0, 487, Short.MAX_VALUE)
                            .add(returnCombo, 0, 487, Short.MAX_VALUE)
                            .add(schemaCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 459, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(opNameTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(schemaCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browseButton)
                .add(11, 11, 11)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(jRadioButton1)
                    .add(jRadioButton2))
                .add(20, 20, 20)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(parmCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(returnCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(faultCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(65, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void schemaComboChanged(java.awt.event.ItemEvent evt) {
        // TODO add your handling code here:
        parmCombo.removeAllItems();
        returnCombo.removeAllItems();
        faultCombo.removeAllItems();
        String selectedItem = (String)schemaCombo.getSelectedItem();
        populateWithElements(selectedItem);
    }


    private void populateWithElements(String selectedItem) {
        parmCombo.addItem("<no params>");populateWithPrimitives(parmCombo);
        returnCombo.addItem("void");populateWithPrimitives(returnCombo);
        faultCombo.addItem("<no exceptions>");
        Import importedSchema = map.get(selectedItem);
        if (importedSchema!=null) {
            String namespace = importedSchema.getNamespace();
            try {
                SchemaModel schemaModel = importedSchema.resolveReferencedModel();
                Collection<GlobalElement> elements = schemaModel.getSchema().getElements();
                for(GlobalElement element : elements){
                    String elementName = element.getName();
                    parmCombo.addItem(element);
                    returnCombo.addItem(element);
                    faultCombo.addItem(element);
                }
            } catch (CatalogModelException ex) {
                ex.printStackTrace();
            }
        } else {
            Schema schema = map1.get(selectedItem);
            if (schema!=null) {
                Collection<GlobalElement> elements = schema.getElements();
                for(GlobalElement element : elements){
                    String elementName = element.getName();
                    parmCombo.addItem(element);
                    returnCombo.addItem(element);
                    faultCombo.addItem(element);
                }
            }
        }
    }

    private void populateWithPrimitives(javax.swing.JComboBox combo) {
        combo.addItem("xsd:string");
        combo.addItem("xsd:int");
        combo.addItem("xsd:boolean");
        combo.addItem("xsd:decimal");
        combo.addItem("xsd:float");
        combo.addItem("xsd:double");
        combo.addItem("xsd:duration");
        combo.addItem("xsd:base64Binary");
        combo.addItem("xsd:hexBinary");
        combo.addItem("xsd:date");
        combo.addItem("xsd:time");
        combo.addItem("xsd:dateTime");
        combo.addItem("xsd:anyUri");
        combo.addItem("xsd:QName");
    }
    
    private GlobalSimpleType getPrimitiveType(String typeName){
        SchemaModel primitiveModel = SchemaModelFactory.getDefault().getPrimitiveTypesModel();
        Collection<GlobalSimpleType> primitives = primitiveModel.getSchema().getSimpleTypes();
        for(GlobalSimpleType ptype: primitives){
            if(ptype.getName().equals(typeName)){
                return ptype;
            }
        }
        return null;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox faultCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JTextField opNameTxt;
    private javax.swing.JComboBox parmCombo;
    private javax.swing.JComboBox returnCombo;
    private javax.swing.JComboBox schemaCombo;
    // End of variables declaration//GEN-END:variables
    
    }
