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

package org.netbeans.modules.form;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.jdesktop.layout.GroupLayout;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.form.FormUtils.TypeHelper;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Binding customizer.
 *
 * @author Jan Stola, Tomas Pavek
 */
public class BindingCustomizer extends JPanel {
    private BindingDesignSupport designSupport;
    /** Target component e.g. component to bound. */
    private RADComponent bindingComponent;
    /** Descriptor of the property to bound. */
    private BindingDescriptor bindingDescriptor;
    /** Binding to customize. */
    private MetaBinding binding;

    /** Selected source component. */
    private RADComponent selectedComponent;
    /** List of all source components. */
    private List<RADComponent> allComponents;

    /** Nested properties. */
    private FormProperty nullValueProperty;
    private FormProperty incompletePathValueProperty;
    private FormProperty converterProperty;
    private FormProperty validatorProperty;
    private FormProperty nameProperty;
    /** Original values of nested properties. */
    private FormProperty.ValueWithEditor oldNullValue;
    private FormProperty.ValueWithEditor oldIncompletePathValue;
    private FormProperty.ValueWithEditor oldConverter;
    private FormProperty.ValueWithEditor oldValidator;
    private FormProperty.ValueWithEditor oldName;

    /** Expression combo box. */
    private ComboBoxWithTree expressionCombo;
    /** Display expression combo box. */
    private ComboBoxWithTree displayExpressionCombo;
    /** Column selector for JTable.elements. */
    private ListSelector columnSelector;
    /** Tree model for the expression combo box. */
    private DefaultTreeModel expressionModel = new DefaultTreeModel(new DefaultMutableTreeNode(null, false));
    /** Tree model for the display expression combo box. */
    private DefaultTreeModel displayExpressionModel = new DefaultTreeModel(new DefaultMutableTreeNode(null, false));
    /** Dialog wrapper around the customizer */
    private Dialog dialog;
    /**
     * Listener invoked when the customization of the binding is finished
     * e.g. OK button of the dialog is pressed.
     */
    private ActionListener dialogListener;
    /** OK button of the dialog. */
    private JButton okButton;
    /** Cancel button of the dialog. */
    private JButton cancelButton;

    /** Property change support for selected type changes. */
    private PropertyChangeSupport typeChangeSupport = new PropertyChangeSupport(this);

    /**
     * Creates new <code>BindingCustomizer</code>.
     *
     * @param property property to customize.
     */
    public BindingCustomizer(BindingProperty property) {
        bindingComponent = property.getBindingComponent();
        bindingDescriptor = property.getBindingDescriptor();
        designSupport = FormEditor.getBindingSupport(bindingComponent.getFormModel());

        initExpressionCombo();
        initComponents();
        
        expressionLabel.setLabelFor(expressionCombo);
        displayExpressionLabel.setLabelFor(displayExpressionCombo);
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateMode1")); // NOI18N
        model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateMode2")); // NOI18N
        model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateMode3")); // NOI18N
        updateModeCombo.setModel(model);

        boolean showUpdateWhen = showUpdateWhen();
        updateWhenLabel.setVisible(showUpdateWhen);
        updateWhenCombo.setVisible(showUpdateWhen);
        if (showUpdateWhen) {
            model = new DefaultComboBoxModel();
            model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateWhen1")); // NOI18N
            model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateWhen2")); // NOI18N
            model.addElement(FormUtils.getBundleString("MSG_BindingCustomizer_UpdateWhen3")); // NOI18N
            updateWhenCombo.setModel(model);
        }

        importDataButton.setVisible(showImportData());

        if (showColumnSelector()) {
            columnSelector = new ListSelector();
            ((GroupLayout)bindingPanel.getLayout()).replace(detailPanel, columnSelector);
        }
        
        boolean showDisplayExpression = showDisplayExpression();
        displayExpressionLabel.setVisible(showDisplayExpression);
        displayExpressionCombo.setVisible(showDisplayExpression);

        nullValueProperty = property.getNullValueProperty();
        if (nullValueProperty != null) {
            nullValuePanel.setProperty(nullValueProperty);
        }
        incompletePathValueProperty = property.getIncompleteValueProperty();
        if (incompletePathValueProperty != null) {
            incompletePathValuePanel.setProperty(incompletePathValueProperty);
        }
        converterProperty = property.getConverterProperty();
        if (converterProperty != null) {
            converterPanel.setProperty(converterProperty);
        }
        validatorProperty = property.getValidatorProperty();
        if (validatorProperty != null) {
            validatorPanel.setProperty(validatorProperty);
        }
        nameProperty = property.getNameProperty();
        if (nameProperty != null) {
            namePanel.setProperty(nameProperty);
        }

        // Hack - make HTML labels non-resizable
        updatePropertiesLabel.setMinimumSize(updatePropertiesLabel.getPreferredSize());
        conversionLabel.setMinimumSize(conversionLabel.getPreferredSize());
        validatorLabel.setMinimumSize(validatorLabel.getPreferredSize());
        specialValuesLabel.setMinimumSize(specialValuesLabel.getPreferredSize());
        updatePropertiesLabel.setMaximumSize(updatePropertiesLabel.getPreferredSize());
        conversionLabel.setMaximumSize(conversionLabel.getPreferredSize());
        validatorLabel.setMaximumSize(validatorLabel.getPreferredSize());
        specialValuesLabel.setMaximumSize(specialValuesLabel.getPreferredSize());
    }

    /**
     * Determines whether Update When section should be shown. 
     * 
     * @return <code>true</code> if Update When section should be shown,
     * returns <code>false</code> otherwise.
     */
    private boolean showUpdateWhen() {
        String path = bindingDescriptor.getPath();
        return "text".equals(path) && JTextComponent.class.isAssignableFrom(bindingComponent.getBeanClass()); // NOI18N
    }

    private boolean showImportData() {
        String path = bindingDescriptor.getPath();
        Class clazz = bindingComponent.getBeanClass();
        return "elements".equals(path) // NOI18N
            && (JList.class.isAssignableFrom(clazz)
                || JComboBox.class.isAssignableFrom(clazz)
                || JTable.class.isAssignableFrom(clazz))
            && (Lookup.getDefault().lookup(DataImporter.class) != null);
    }

    private boolean showDisplayExpression() {
        String path = bindingDescriptor.getPath();
        Class clazz = bindingComponent.getBeanClass();
        return "elements".equals(path) // NOI18N
            && (JList.class.isAssignableFrom(clazz)
                || JComboBox.class.isAssignableFrom(clazz));        
    }

    private boolean showColumnSelector() {
        return "elements".equals(bindingDescriptor.getPath()) // NOI18N
            && JTable.class.isAssignableFrom(bindingComponent.getBeanClass());
    }

    /**
     * Returns customized binding.
     *
     * @return customized binding.
     */
    public MetaBinding getBinding() {
        return binding;
    }

    /**
     * Sets binding to customize.
     *
     * @param binding binding to customize.
     */
    public void setBinding(MetaBinding binding) {
        this.binding = binding;
        String info = MessageFormat.format(infoLabel.getText(),
            new Object[] { bindingComponent.getName(), bindingDescriptor.getPath(), bindingDescriptor.getValueType().getName()});
        infoLabel.setText(info);
        setBindingToUI();
    }

    /**
     * Returns dialog wrapper around the customizer.
     *
     * @param listener listener notified when the customization of the binding is finished.
     * @return dialog wrapper around the customizer.
     */
    public Dialog getDialog(ActionListener listener) {
        if (dialog == null) {
            ResourceBundle bundle = NbBundle.getBundle(BindingCustomizer.class);
            initButtons(bundle);
            String pattern = bundle.getString("MSG_BindingCustomizer_Binding"); // NOI18N
            String title = MessageFormat.format(pattern, bindingComponent.getName(), bindingDescriptor.getPath());
            DialogDescriptor dd = new DialogDescriptor(
                this, title, true,
                new JButton[] { okButton, cancelButton },
                okButton,
                DialogDescriptor.DEFAULT_ALIGN, HelpCtx.DEFAULT_HELP,
                null
            );
            dd.setClosingOptions(new JButton[] { okButton, cancelButton });
            dialog = DialogDisplayer.getDefault().createDialog(dd);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    restore(nullValueProperty, oldNullValue);
                    restore(incompletePathValueProperty, oldIncompletePathValue);
                    restore(converterProperty, oldConverter);
                    restore(validatorProperty, oldValidator);
                    restore(nameProperty, oldName);
                }
                private void restore(FormProperty property, Object value) {
                    // the original values are cleared when okButton is pressed
                    if (value != null) {
                        try {
                            property.setValue(value);
                        } catch (IllegalAccessException iaex) {
                            iaex.printStackTrace();
                        } catch (InvocationTargetException itex) {
                            itex.printStackTrace();
                        }
                    }                    
                }
            });
        }
        dialogListener = listener;
        updatePropertyPanels();
        return dialog;
    }

    public JComponent getBindingPanel() {
        infoLabel.setVisible(false);
        if (columnSelector.getParent() != null) {
            detailPanel.setVisible(false);
            ((GroupLayout)bindingPanel.getLayout()).replace(columnSelector, detailPanel);
        }
        return bindingPanel;
    }

    public ComboBoxWithTree getSubExpressionCombo() {
        return displayExpressionCombo;
    }

    public TreeModel getSubExpressionModel() {
        return displayExpressionModel;
    }

    /**
     * Initializes buttons on the dialog.
     *
     * @param bundle localized messages to be included on the buttons.
     */
    private void initButtons(ResourceBundle bundle) {
        okButton = new JButton(bundle.getString("MSG_BindingCustomizer_OK")); // NOI18N
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (getBindingFromUI()) {
                    oldNullValue = null;
                    oldIncompletePathValue = null;
                    oldConverter = null;
                    oldValidator = null;
                    oldName = null;
                    if (dialogListener != null) {
                        dialogListener.actionPerformed(ev);
                    }
                }
            }
        });
        cancelButton = new JButton(bundle.getString("MSG_BindingCustomizer_Cancel")); // NOI18N
    }

    /**
     * Returns form model.
     *
     * @return form model.
     */
    private FormModel getFormModel() {
        return bindingComponent.getFormModel();
    }

    /**
     * Updates UI according to value of <code>binding</code> property.
     */
    private void setBindingToUI() {
        fillSourceComponentsCombo();
        if (binding != null) {
            // source component
            int index = allComponents.indexOf(binding.getSource());
            if (index >= 0) {
                // +1 because the first item is <select>
                sourceCombo.setSelectedIndex(index+1);
                setSelectedComponent(binding.getSource());

                String sourcePath = binding.getSourcePath();
                expressionCombo.setSelectedItem(sourcePath == null ? "null" : sourcePath); // NOI18N
            }
            updateModeCombo.setSelectedIndex(binding.getUpdateStratedy());
            if (showUpdateWhen()) {
                int updateWhen = 0;
                String value = binding.getParameter(MetaBinding.TEXT_CHANGE_STRATEGY);
                if (MetaBinding.TEXT_CHANGE_ON_ACTION_OR_FOCUS_LOST.equals(value)) {
                    updateWhen = 0;
                } else if (MetaBinding.TEXT_CHANGE_ON_FOCUS_LOST.equals(value)) {
                    updateWhen = 1;
                } else if (MetaBinding.TEXT_CHANGE_ON_TYPE.equals(value)) {
                    updateWhen = 2;
                }
                updateWhenCombo.setSelectedIndex(updateWhen);
            }
            if (showDisplayExpression()) {
                String subPath = null;
                if (binding.hasSubBindings()) {
                    MetaBinding subBinding = binding.getSubBindings().iterator().next();
                    subPath = subBinding.getSourcePath();
                }
                displayExpressionCombo.setSelectedItem(subPath == null ? "null" : subPath); // NOI18N
            }
            updateColumnSelector();
            if (columnSelector != null) {
                if (binding.hasSubBindings()) {
                    SortedMap<Integer,String> columns = new TreeMap<Integer,String>();
                    for (MetaBinding subBinding : binding.getSubBindings()) {
                        Object value = subBinding.getParameter(MetaBinding.TABLE_COLUMN_PARAMETER);
                        if (value != null) {
                            columns.put(new Integer(value.toString()), subBinding.getSourcePath());
                        }
                    }
                    List<String> available = new LinkedList<String>(columnSelector.getSelectedItems());
                    List<String> selected = new LinkedList<String>();
                    Iterator iter = columns.values().iterator();
                    while (iter.hasNext()) {
                        String column = iter.next().toString();
                        column = BindingDesignSupport.unwrapSimpleExpression(column);
                        if (available.contains(column)) {
                            selected.add(column);
                            available.remove(column);
                        }
                    }
                    columnSelector.setItems(available, selected);
                }
            }
            nullValueCheckBox.setSelected(binding.isNullValueSpecified());
            incompletePathValueCheckBox.setSelected(binding.isIncompletePathValueSpecified());
            if (binding.isNullValueSpecified()) {
                oldNullValue = propertyValue(nullValueProperty);
            }
            if (binding.isIncompletePathValueSpecified()) {
                oldIncompletePathValue = propertyValue(incompletePathValueProperty);
            }
            if (binding.isConverterSpecified()) {
                oldConverter = propertyValue(converterProperty);
            }
            if (binding.isValidatorSpecified()) {
                oldValidator = propertyValue(validatorProperty);
            }
            if (binding.isNameSpecified()) {
                oldName = propertyValue(nameProperty);
            }
        } else {
            sourceCombo.setSelectedIndex(0);
            updateModeCombo.setSelectedIndex(0);
            if (showUpdateWhen()) {
                updateWhenCombo.setSelectedIndex(0);
            }
            nullValueCheckBox.setSelected(false);
            incompletePathValueCheckBox.setSelected(false);
            oldNullValue = null;
            oldIncompletePathValue = null;
            oldConverter = null;
            oldValidator = null;
            oldName = null;
        }
        updatePropertyPanels();
    }

    private void updatePropertyPanels() {
        if (binding != null) {
            nullValuePanel.setEnabled(nullValueCheckBox.isSelected());
            incompletePathValuePanel.setEnabled(incompletePathValueCheckBox.isSelected());
        } else {
            nullValuePanel.setEnabled(false);
            incompletePathValuePanel.setEnabled(false);
        }
    }

    private FormProperty.ValueWithEditor propertyValue(FormProperty property) {
        FormProperty.ValueWithEditor value = null;
        try {
            value = new FormProperty.ValueWithEditor(property.getValue(), property.getCurrentEditor());
        } catch (IllegalAccessException iaex) {
            iaex.printStackTrace();
        } catch (InvocationTargetException itex) {
            itex.printStackTrace();
        }
        return value;
    }

   /**
    * Fills combo box with source components.
    */
    private void fillSourceComponentsCombo() {
        FormModel formModel = getFormModel();
        List<RADComponent> nonvisualList = formModel.getNonVisualComponents();
        List<RADComponent> visualList = formModel.getVisualComponents();
        RADComponent topcomp = formModel.getTopRADComponent();
        nonvisualList.remove(topcomp);
        visualList.remove(topcomp);

        Comparator<RADComponent> c = new RADComponentComparator();
        Collections.sort(nonvisualList, c);
        Collections.sort(visualList, c);

        allComponents = new ArrayList<RADComponent>(nonvisualList.size() + visualList.size() + 1);
        allComponents.addAll(nonvisualList);
        allComponents.addAll(visualList);
        allComponents.add(topcomp);

        sourceCombo.removeAllItems();
        String select = NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_None"); // NOI18N
        sourceCombo.addItem(select);
        for (RADComponent metacomp : allComponents) {
            sourceCombo.addItem(metacomp.getName());
        }
    }

    /**
     * Sets a selected component.
     *
     * @param metacomp selected component.
     */
    private void setSelectedComponent(RADComponent metacomp) {
        if (metacomp != selectedComponent) {
            selectedComponent = metacomp;
        }
    }

    public boolean getBindingFromUI() {
        if (selectedComponent != null) {
            String sourcePath = expressionCombo.getSelectedItem().toString();
            if ("null".equals(sourcePath)) { // NOI18N
                sourcePath = null;
            }
            binding = new MetaBinding(selectedComponent, sourcePath, bindingComponent, bindingDescriptor.getPath());
            binding.setNullValueSpecified(nullValueCheckBox.isSelected());
            binding.setIncompletePathValueSpecified(incompletePathValueCheckBox.isSelected());
            binding.setUpdateStrategy(updateModeCombo.getSelectedIndex());
            if (showUpdateWhen()) {
                int index = updateWhenCombo.getSelectedIndex();
                String updateWhen = null;
                switch (index) {
                    case 0: updateWhen = null; /*MetaBinding.TEXT_CHANGE_ON_ACTION_OR_FOCUS_LOST*/ break; // default value
                    case 1: updateWhen = MetaBinding.TEXT_CHANGE_ON_FOCUS_LOST; break;
                    case 2: updateWhen = MetaBinding.TEXT_CHANGE_ON_TYPE; break;
                    default: assert false;
                }
                binding.setParameter(MetaBinding.TEXT_CHANGE_STRATEGY, updateWhen);
            }
            if (showDisplayExpression()) {
                String displayExpression = displayExpressionCombo.getSelectedItem().toString();
                if ("null".equals(displayExpression)) { // NOI18N
                    displayExpression = null;
                }
                if (displayExpression != null) {
                    binding.addSubBinding(displayExpression, null);
                }
            }
            if (columnSelector != null) {
                binding.setBindImmediately(true);
                if (columnSelector.isVisible()) {
                    List items = columnSelector.getSelectedItems();
                    for (int i=0; i<items.size(); i++) {
                        String item = items.get(i).toString();
                        MetaBinding subBinding = binding.addSubBinding(BindingDesignSupport.elWrap(item), null);
                        subBinding.setParameter(MetaBinding.TABLE_COLUMN_PARAMETER, i+""); // NOI18N
                        String columnType = columnToType.get(item);
                        if ((columnType != null) && (!columnType.equals("java.lang.Object"))) { // NOI18N
                            String clazz = FormUtils.autobox(columnType);
                            if (clazz.startsWith("java.lang.")) { // NOI18N
                                clazz = clazz.substring(10);
                            }
                            clazz += ".class"; // NOI18N
                            subBinding.setParameter(MetaBinding.TABLE_COLUMN_CLASS_PARAMETER, clazz);
                        }
                    }
                }
            }
        } else {
            binding = null;
        }
        return true;
    }

    private void initExpressionCombo() {
        TreeCellRenderer renderer = new TreeComboRenderer(FormUtils.getBundleString("MSG_BindingCustomizer_NullExpression")); // NOI18N
        expressionCombo = new ComboBoxWithTree(expressionModel, renderer, new Converter(expressionModel));
        expressionCombo.setSelectedItem("null"); // NOI18N

        renderer = new TreeComboRenderer(FormUtils.getBundleString("MSG_BindingCustomizer_NullDisplayExpression")); // NOI18N
        displayExpressionCombo = new ComboBoxWithTree(displayExpressionModel, renderer, new Converter(displayExpressionModel));
        displayExpressionCombo.setSelectedItem("null"); // NOI18N
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabbedPane = new javax.swing.JTabbedPane();
        bindingPanel = new javax.swing.JPanel();
        infoLabel = new javax.swing.JLabel();
        sourceLabel = new javax.swing.JLabel();
        sourceCombo = new javax.swing.JComboBox();
        expressionLabel = new javax.swing.JLabel();
        treeCombo = expressionCombo ;
        importDataButton = new javax.swing.JButton();
        displayExpressionLabel = new javax.swing.JLabel();
        treeCombo2 = displayExpressionCombo ;
        detailPanel = new javax.swing.JPanel();
        advancedPanel = new javax.swing.JPanel();
        updatePropertiesLabel = new javax.swing.JLabel();
        updateModeLabel = new javax.swing.JLabel();
        updateModeCombo = new javax.swing.JComboBox();
        updateWhenLabel = new javax.swing.JLabel();
        updateWhenCombo = new javax.swing.JComboBox();
        nullValuePanel = new org.openide.explorer.propertysheet.PropertyPanel();
        incompletePathValuePanel = new org.openide.explorer.propertysheet.PropertyPanel();
        nullValueCheckBox = new javax.swing.JCheckBox();
        displayValuesLabel = new javax.swing.JLabel();
        incompletePathValueCheckBox = new javax.swing.JCheckBox();
        conversionLabel = new javax.swing.JLabel();
        validationLabel = new javax.swing.JLabel();
        validatorLabel = new javax.swing.JLabel();
        converterLabel = new javax.swing.JLabel();
        converterPanel = new org.openide.explorer.propertysheet.PropertyPanel();
        validatorPanel = new org.openide.explorer.propertysheet.PropertyPanel();
        specialValuesLabel = new javax.swing.JLabel();
        updateLabel = new javax.swing.JLabel();
        converterMessage = new javax.swing.JLabel();
        validatorMessage = new javax.swing.JLabel();
        identificationLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        namePanel = new org.openide.explorer.propertysheet.PropertyPanel();

        FormListener formListener = new FormListener();

        org.openide.awt.Mnemonics.setLocalizedText(infoLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Bind")); // NOI18N

        sourceLabel.setLabelFor(sourceCombo);
        org.openide.awt.Mnemonics.setLocalizedText(sourceLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Source")); // NOI18N

        sourceCombo.addActionListener(formListener);

        org.openide.awt.Mnemonics.setLocalizedText(expressionLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Expression")); // NOI18N

        treeCombo.addActionListener(formListener);

        org.openide.awt.Mnemonics.setLocalizedText(importDataButton, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_ImportData")); // NOI18N
        importDataButton.addActionListener(formListener);

        org.openide.awt.Mnemonics.setLocalizedText(displayExpressionLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_DisplayExpression")); // NOI18N

        org.jdesktop.layout.GroupLayout detailPanelLayout = new org.jdesktop.layout.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 452, Short.MAX_VALUE)
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 286, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout bindingPanelLayout = new org.jdesktop.layout.GroupLayout(bindingPanel);
        bindingPanel.setLayout(bindingPanelLayout);
        bindingPanelLayout.setHorizontalGroup(
            bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(bindingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(infoLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
                    .add(bindingPanelLayout.createSequentialGroup()
                        .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(displayExpressionLabel)
                            .add(expressionLabel)
                            .add(sourceLabel))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(bindingPanelLayout.createSequentialGroup()
                                .add(sourceCombo, 0, 204, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(importDataButton))
                            .add(treeCombo, 0, 355, Short.MAX_VALUE)
                            .add(treeCombo2, 0, 355, Short.MAX_VALUE)))
                    .add(detailPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(10, 10, 10))
        );
        bindingPanelLayout.setVerticalGroup(
            bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(bindingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(infoLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sourceLabel)
                    .add(sourceCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(importDataButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(expressionLabel)
                    .add(treeCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bindingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(displayExpressionLabel)
                    .add(treeCombo2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        sourceCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Source_ACSD")); // NOI18N
        treeCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Expression_ACSD")); // NOI18N
        importDataButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_ImportData_ACSD")); // NOI18N
        treeCombo2.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_DisplayExpression_ACSD")); // NOI18N

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_BindingTab"), bindingPanel); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(updatePropertiesLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_UpdateProperties")); // NOI18N

        updateModeLabel.setLabelFor(updateModeCombo);
        org.openide.awt.Mnemonics.setLocalizedText(updateModeLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_UpdateMode")); // NOI18N

        updateWhenLabel.setLabelFor(updateWhenCombo);
        org.openide.awt.Mnemonics.setLocalizedText(updateWhenLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_UpdateWhen")); // NOI18N

        nullValuePanel.setEnabled(false);
        nullValuePanel.setPreferences(PropertyPanel.PREF_TABLEUI);

        org.jdesktop.layout.GroupLayout nullValuePanelLayout = new org.jdesktop.layout.GroupLayout(nullValuePanel);
        nullValuePanel.setLayout(nullValuePanelLayout);
        nullValuePanelLayout.setHorizontalGroup(
            nullValuePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 317, Short.MAX_VALUE)
        );
        nullValuePanelLayout.setVerticalGroup(
            nullValuePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 24, Short.MAX_VALUE)
        );

        incompletePathValuePanel.setEnabled(false);
        incompletePathValuePanel.setPreferences(PropertyPanel.PREF_TABLEUI);

        org.jdesktop.layout.GroupLayout incompletePathValuePanelLayout = new org.jdesktop.layout.GroupLayout(incompletePathValuePanel);
        incompletePathValuePanel.setLayout(incompletePathValuePanelLayout);
        incompletePathValuePanelLayout.setHorizontalGroup(
            incompletePathValuePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 317, Short.MAX_VALUE)
        );
        incompletePathValuePanelLayout.setVerticalGroup(
            incompletePathValuePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 24, Short.MAX_VALUE)
        );

        org.openide.awt.Mnemonics.setLocalizedText(nullValueCheckBox, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_NullValue")); // NOI18N
        nullValueCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        nullValueCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        nullValueCheckBox.addActionListener(formListener);

        org.openide.awt.Mnemonics.setLocalizedText(displayValuesLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_DisplayValues")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(incompletePathValueCheckBox, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_IncompletePathValue")); // NOI18N
        incompletePathValueCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        incompletePathValueCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        incompletePathValueCheckBox.addActionListener(formListener);

        org.openide.awt.Mnemonics.setLocalizedText(conversionLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Conversion")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(validationLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Validation")); // NOI18N

        validatorLabel.setLabelFor(validatorPanel);
        org.openide.awt.Mnemonics.setLocalizedText(validatorLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Validator")); // NOI18N

        converterLabel.setLabelFor(converterPanel);
        org.openide.awt.Mnemonics.setLocalizedText(converterLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Converter")); // NOI18N

        if (false) {
            converterPanel.setEnabled(false);
        }
        converterPanel.setPreferences(PropertyPanel.PREF_TABLEUI);

        org.jdesktop.layout.GroupLayout converterPanelLayout = new org.jdesktop.layout.GroupLayout(converterPanel);
        converterPanel.setLayout(converterPanelLayout);
        converterPanelLayout.setHorizontalGroup(
            converterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 395, Short.MAX_VALUE)
        );
        converterPanelLayout.setVerticalGroup(
            converterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 24, Short.MAX_VALUE)
        );

        if (false) {
            validatorPanel.setEnabled(false);
        }
        validatorPanel.setPreferences(PropertyPanel.PREF_TABLEUI);

        org.jdesktop.layout.GroupLayout validatorPanelLayout = new org.jdesktop.layout.GroupLayout(validatorPanel);
        validatorPanel.setLayout(validatorPanelLayout);
        validatorPanelLayout.setHorizontalGroup(
            validatorPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 395, Short.MAX_VALUE)
        );
        validatorPanelLayout.setVerticalGroup(
            validatorPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 24, Short.MAX_VALUE)
        );

        org.openide.awt.Mnemonics.setLocalizedText(specialValuesLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_SpecialValues")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(updateLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Update")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(converterMessage, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_ConversionTxt")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(validatorMessage, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_ValidationTxt")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(identificationLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Identification")); // NOI18N

        nameLabel.setLabelFor(namePanel);
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Name")); // NOI18N

        if (false) {
            namePanel.setEnabled(false);
        }
        namePanel.setPreferences(PropertyPanel.PREF_TABLEUI);

        org.jdesktop.layout.GroupLayout namePanelLayout = new org.jdesktop.layout.GroupLayout(namePanel);
        namePanel.setLayout(namePanelLayout);
        namePanelLayout.setHorizontalGroup(
            namePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 395, Short.MAX_VALUE)
        );
        namePanelLayout.setVerticalGroup(
            namePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 24, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout advancedPanelLayout = new org.jdesktop.layout.GroupLayout(advancedPanel);
        advancedPanel.setLayout(advancedPanelLayout);
        advancedPanelLayout.setHorizontalGroup(
            advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(converterLabel)
                    .add(validatorLabel)
                    .add(nameLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(namePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(converterPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(validatorPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(identificationLabel)
                .addContainerGap(386, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(updateLabel)
                .addContainerGap(234, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(specialValuesLabel)
                .addContainerGap(368, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(updateModeLabel)
                    .add(updateWhenLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(updateWhenCombo, 0, 378, Short.MAX_VALUE)
                    .add(updateModeCombo, 0, 378, Short.MAX_VALUE))
                .add(10, 10, 10))
            .add(advancedPanelLayout.createSequentialGroup()
                .add(10, 10, 10)
                .add(updatePropertiesLabel)
                .addContainerGap(359, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(nullValueCheckBox)
                    .add(incompletePathValueCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(incompletePathValuePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(nullValuePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(displayValuesLabel)
                .addContainerGap(155, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(conversionLabel)
                .addContainerGap(368, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(converterMessage)
                .addContainerGap(266, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(validatorMessage)
                .addContainerGap(349, Short.MAX_VALUE))
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(validationLabel)
                .addContainerGap(406, Short.MAX_VALUE))
        );
        advancedPanelLayout.setVerticalGroup(
            advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(identificationLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(namePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(nameLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(updatePropertiesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(updateLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(updateModeLabel)
                    .add(updateModeCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(updateWhenLabel)
                    .add(updateWhenCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(conversionLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(converterMessage)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(converterLabel)
                    .add(converterPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(validationLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(validatorMessage)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(validatorLabel)
                    .add(validatorPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(specialValuesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(displayValuesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.CENTER, nullValueCheckBox)
                    .add(org.jdesktop.layout.GroupLayout.CENTER, nullValuePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(7, 7, 7)
                .add(advancedPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.CENTER, incompletePathValueCheckBox)
                    .add(org.jdesktop.layout.GroupLayout.CENTER, incompletePathValuePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        updateModeCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_UpdateMode_ACSD")); // NOI18N
        updateWhenCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_UpdateWhen_ACSD")); // NOI18N
        nullValuePanel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_NullValue_ACSD")); // NOI18N
        incompletePathValuePanel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_IncompletePathValue_ACSD")); // NOI18N
        nullValueCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_NullValue_ACSD")); // NOI18N
        incompletePathValueCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_IncompletePathValue_ACSD")); // NOI18N
        converterPanel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Converter_ACSD")); // NOI18N
        validatorPanel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Validator_ACSD")); // NOI18N
        namePanel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_Name_ACSD")); // NOI18N

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_AdvancedTab"), advancedPanel); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabbedPane)
        );

        tabbedPane.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(BindingCustomizer.class, "MSG_BindingCustomizer_ACSD")); // NOI18N
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == sourceCombo) {
                BindingCustomizer.this.sourceComboActionPerformed(evt);
            }
            else if (evt.getSource() == treeCombo) {
                BindingCustomizer.this.treeComboActionPerformed(evt);
            }
            else if (evt.getSource() == importDataButton) {
                BindingCustomizer.this.importDataButtonActionPerformed(evt);
            }
            else if (evt.getSource() == nullValueCheckBox) {
                BindingCustomizer.this.nullValueCheckBoxActionPerformed(evt);
            }
            else if (evt.getSource() == incompletePathValueCheckBox) {
                BindingCustomizer.this.incompletePathValueCheckBoxActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void importDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importDataButtonActionPerformed
    DataImporter importer = Lookup.getDefault().lookup(DataImporter.class);
    if (importer != null) {
        RADComponent data = importer.importData(bindingComponent.getFormModel());
        if (data != null) {
            // refresh source components combo
            fillSourceComponentsCombo();
            sourceCombo.setSelectedItem(data.getName());
        }
    }
}//GEN-LAST:event_importDataButtonActionPerformed

    private void treeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_treeComboActionPerformed
        updateColumnSelector();
        fireTypeChange();
    }//GEN-LAST:event_treeComboActionPerformed

    private void incompletePathValueCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incompletePathValueCheckBoxActionPerformed
        incompletePathValuePanel.setEnabled(incompletePathValueCheckBox.isSelected());
    }//GEN-LAST:event_incompletePathValueCheckBoxActionPerformed

    private void nullValueCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nullValueCheckBoxActionPerformed
        nullValuePanel.setEnabled(nullValueCheckBox.isSelected());
    }//GEN-LAST:event_nullValueCheckBoxActionPerformed

    /**
     * Handles change of selected source component.
     *
     * @param evt event describing the change that occured.
     */
    private void sourceComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceComboActionPerformed
        int index = sourceCombo.getSelectedIndex();
        if (index >= 0) {
            setSelectedComponent(index > 0 ? allComponents.get(index-1) : null);
            if (index > 0) {
                expressionModel.setRoot(new ExpressionNode(allComponents.get(index-1)));
            } else {
                expressionCombo.setSelectedItem("null"); // NOI18N
                expressionModel.setRoot(new DefaultMutableTreeNode(null, false));                
            }
            if (tabbedPane.getTabCount() > 1) {
                tabbedPane.setEnabledAt(1, index > 0);
            }
        }
        expressionCombo.setEnabled(index > 0);
        displayExpressionCombo.setEnabled(index > 0);
        updateColumnSelector();
        fireTypeChange();
    }//GEN-LAST:event_sourceComboActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JPanel advancedPanel;
    javax.swing.JPanel bindingPanel;
    javax.swing.JLabel conversionLabel;
    javax.swing.JLabel converterLabel;
    javax.swing.JLabel converterMessage;
    org.openide.explorer.propertysheet.PropertyPanel converterPanel;
    javax.swing.JPanel detailPanel;
    javax.swing.JLabel displayExpressionLabel;
    javax.swing.JLabel displayValuesLabel;
    javax.swing.JLabel expressionLabel;
    javax.swing.JLabel identificationLabel;
    javax.swing.JButton importDataButton;
    javax.swing.JCheckBox incompletePathValueCheckBox;
    org.openide.explorer.propertysheet.PropertyPanel incompletePathValuePanel;
    javax.swing.JLabel infoLabel;
    javax.swing.JLabel nameLabel;
    org.openide.explorer.propertysheet.PropertyPanel namePanel;
    javax.swing.JCheckBox nullValueCheckBox;
    org.openide.explorer.propertysheet.PropertyPanel nullValuePanel;
    javax.swing.JComboBox sourceCombo;
    javax.swing.JLabel sourceLabel;
    javax.swing.JLabel specialValuesLabel;
    javax.swing.JTabbedPane tabbedPane;
    javax.swing.JComboBox treeCombo;
    javax.swing.JComboBox treeCombo2;
    javax.swing.JLabel updateLabel;
    javax.swing.JComboBox updateModeCombo;
    javax.swing.JLabel updateModeLabel;
    javax.swing.JLabel updatePropertiesLabel;
    javax.swing.JComboBox updateWhenCombo;
    javax.swing.JLabel updateWhenLabel;
    javax.swing.JLabel validationLabel;
    javax.swing.JLabel validatorLabel;
    javax.swing.JLabel validatorMessage;
    org.openide.explorer.propertysheet.PropertyPanel validatorPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Comparator of <code>RADComponent</code>s.
     */
    private static class RADComponentComparator implements Comparator<RADComponent> {
        public int compare(RADComponent o1, RADComponent o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();
            return name1.compareToIgnoreCase(name2);
        }

    }

    public TypeHelper getSelectedType() {
        TypeHelper type = null;
        if (selectedComponent != null) {
            Object value = expressionCombo.getSelectedItem();
            if ((value == null) || ("null".equals(value))) { // NOI18N
                type = BindingDesignSupport.determineType(selectedComponent);
            } else {
                String path = value.toString();
                if (BindingDesignSupport.isSimpleExpression(path)) {
                    type = designSupport.determineType(selectedComponent, BindingDesignSupport.unwrapSimpleExpression(path));
                } else {
                    type = new TypeHelper(String.class);
                }
            }
        }
        return type;
    }

    private Map<String,String> columnToType;
    
    // Updates also displayExpressionCombo
    private void updateColumnSelector() {
        boolean showDisplayExpression = showDisplayExpression();
        if (!showDisplayExpression && (columnSelector == null)) return;
        TypeHelper type = getSelectedType();
        if ((type != null) && Collection.class.isAssignableFrom(FormUtils.typeToClass(type))) {
            TypeHelper elemType = BindingDesignSupport.typeOfElement(type);
            if (columnSelector != null) {
                List<BindingDescriptor> descriptors = designSupport.getAllBindingDescriptors(elemType);
                columnSelector.setVisible(descriptors.size() > 0);
                List<String> available = new LinkedList<String>();
                columnToType = new HashMap<String,String>();
                for (BindingDescriptor desc : descriptors) {
                    TypeHelper t = desc.getGenericValueType();
                    String className = t.getName();
                    if (className == null) {
                        Class clazz = desc.getValueType();
                        className = clazz.getName();
                    }
                    columnToType.put(desc.getPath(), className);
                    available.add(desc.getPath());
                }
                columnSelector.setItems(Collections.EMPTY_LIST, available);
            }
            displayExpressionModel.setRoot(new ExpressionNode(elemType));
        } else {
            if (columnSelector != null) {
                columnSelector.setVisible(false);
            } else {
                displayExpressionCombo.setSelectedItem("null"); // NOI18N
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(null, true);
            node.add(new DefaultMutableTreeNode(null, false));
            displayExpressionModel.setRoot(node);
        }
    }

    public void addTypeChangeListener(PropertyChangeListener listener) {
        typeChangeSupport.addPropertyChangeListener(listener);
    }

    public void removeTypeChangeListener(PropertyChangeListener listener) {
        typeChangeSupport.removePropertyChangeListener(listener);
    }

    private void fireTypeChange() {
        typeChangeSupport.firePropertyChange(null, null, null);
    }

   public class ExpressionNode extends JTree.DynamicUtilTreeNode {
       private BindingDescriptor descriptor;
       private RADComponent comp;
       private int category;
       private TypeHelper type;
       
       ExpressionNode(RADComponent comp) {
           this(BindingDesignSupport.determineType(comp));
           this.comp = comp;
       }
       
       ExpressionNode(TypeHelper type) {
           super("root", designSupport.getBindingDescriptors(type)); // NOI18N
           this.type = type;
           setAllowsChildren(true);
       }
       
       private ExpressionNode(BindingDescriptor descriptor) {
           super(descriptor.getPath(), designSupport.getBindingDescriptors(descriptor.getGenericValueType()));
           this.descriptor = descriptor;
           this.type = descriptor.getGenericValueType();
           updateLeafStatus();
       }
       
       private ExpressionNode() {
           super(null, null);
       }
       
       private void updateLeafStatus() {
           boolean leaf = true;
           if (childValue instanceof List[]) {
               List[] lists = (List[])childValue;
               for (int i=0; i<lists.length; i++) {
                   if (lists[i].size() > 0) {
                       leaf = false;
                       break;
                   }
               }
           }
           setAllowsChildren(!leaf);
       }

       BindingDescriptor getDescriptor() {
           return descriptor;
       }

       int getCategory() {
           return category;
       }

       public String getTypeName() {
           String name = (type == null)? null : type.getName();
           if (name == null) {
               name = FormUtils.typeToClass(type).getName();
           }
           return name;
       }

       @Override
       protected void loadChildren() {
           loadedChildren = true;
           if ("root".equals(getUserObject())) { // NOI18N
               add(new ExpressionNode()); // null expression
           }
           if (childValue instanceof List[]) {
               List<BindingDescriptor>[] lists = (List<BindingDescriptor>[])childValue;
               for (int i=0; i<lists.length; i++) {
                   loadChildren(lists[i], i);
               }                       
           }
       }

       private void loadChildren(List<BindingDescriptor> descriptors, int category) {
           for (BindingDescriptor descriptor : descriptors) {
               ExpressionNode child;
               if (descriptor.isValueTypeRelative()) {
                   StringBuilder sb = new StringBuilder(descriptor.getPath());
                   ExpressionNode node = this;
                   while (node.comp == null) {
                       sb.insert(0, node.getDescriptor().getPath() + "."); // NOI18N
                       node = (ExpressionNode)getParent();
                   }
                   TypeHelper type = designSupport.determineType(node.comp, sb.toString());
                   child = new ExpressionNode(new BindingDescriptor(descriptor.getPath(), type));
               } else {
                   child = new ExpressionNode(descriptor);
               }
               child.category = category;
               add(child);
           }
       }
       
   }

   private static class Converter implements ComboBoxWithTree.Converter {
       private DefaultTreeModel treeModel;

       Converter(DefaultTreeModel treeModel) {
           this.treeModel = treeModel;
       }
       
        public String pathToString(TreePath path) {
            StringBuilder sb = new StringBuilder();
            Object[] items = path.getPath();
            for (int i=1; i<items.length; i++) {
                sb.append(items[i]).append('.');
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length()-1);
            }
            String value = sb.toString().trim();
            return "null".equals(value) ? "null" : BindingDesignSupport.elWrap(sb.toString()); // NOI18N
        }
        
        public TreePath stringToPath(String value) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)treeModel.getRoot();
            if (BindingDesignSupport.isSimpleExpression(value)) {
                value = BindingDesignSupport.unwrapSimpleExpression(value);
            } else {
                if ("null".equals(value)) { // NOI18N
                    return new TreePath(new Object[] {node, node.getChildAt(0)});
                }
                return null;
            }
            List<DefaultMutableTreeNode> path = new LinkedList<DefaultMutableTreeNode>();
            // always include root
            path.add(node);
            int index;
            while ((index = value.indexOf('.')) != -1) {
                String item = value.substring(0, index);
                node = findNode(node, item);
                if (node == null) {
                    return null;
                } else {
                    path.add(node);
                }
                value = value.substring(index+1);
            }
            // add last
            node = findNode(node, value);
            if (node != null) {
                path.add(node);
            } else {
                return null;
            }
            return new TreePath(path.toArray());
        }

        private DefaultMutableTreeNode findNode(DefaultMutableTreeNode parent, String userObject) {
            for (int i=0; i<parent.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);
                if (userObject.equals(child.getUserObject())) {
                    return child;
                }
            }
            return null;
        }

    }

    private static class TreeComboRenderer extends DefaultTreeCellRenderer {
        private String nullString;
        private int baseSize;

        TreeComboRenderer(String nullString) {
            this.nullString = nullString;
            this.baseSize = new JLabel().getFont().getSize()+1;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
               boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus); // NOI18N
            if (value instanceof ExpressionNode) {
                ExpressionNode node = (ExpressionNode)value;
                Object object = node.getUserObject();
                if (object == null) {
                    updateFont(label, baseSize);
                    label.setText(nullString);
                } else {
                    BindingDescriptor descriptor = node.getDescriptor();
                    if (descriptor != null) {
                        updateFont(label, baseSize-node.getCategory());
                        label.setText("<html><b>" + descriptor.getPath() + "</b> " + nameOfClass(descriptor.getGenericValueType())); // NOI18N
                    }
                }
            } else if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
                Object object = node.getUserObject();
                if (object == null) {
                    updateFont(label, baseSize);
                    label.setText("null"); // NOI18N
                }
            }
            return label;
        }

        private static void updateFont(JLabel label, int size) {
            if (label.getFont().getSize() != size) {
                label.setFont(label.getFont().deriveFont((float)size));
            }
        }
        
        private String nameOfClass(TypeHelper type) {
            String name = type.getName();
            if (name == null) {
                name = FormUtils.typeToClass(type).getName();
                if (name.startsWith("[")) { // NOI18N
                    StringBuilder sb = new StringBuilder();
                    while (name.startsWith("[")) { // NOI18N
                        sb.append("[]"); // NOI18N
                        name = name.substring(1);
                    }
                    if ("Z".equals(name)) { // NOI18N
                        sb.insert(0, "boolean"); // NOI18N
                    } else if ("B".equals(name)) { // NOI18N
                        sb.insert(0, "byte"); // NOI18N
                    } else if ("C".equals(name)) { // NOI18N
                        sb.insert(0, "char"); // NOI18N
                    } else if ("D".equals(name)) { // NOI18N
                        sb.insert(0, "double"); // NOI18N
                    } else if ("F".equals(name)) { // NOI18N
                        sb.insert(0, "float"); // NOI18N
                    } else if ("I".equals(name)) { // NOI18N
                        sb.insert(0, "int"); // NOI18N
                    } else if ("J".equals(name)) { // NOI18N
                        sb.insert(0, "long"); // NOI18N
                    } else if ("S".equals(name)) { // NOI18N
                        sb.insert(0, "short"); // NOI18N
                    } else {
                        sb.insert(0, name.substring(1, name.length()-1));
                    }
                    name = sb.toString();
                }
            }
            return name;
        }
    }

}
