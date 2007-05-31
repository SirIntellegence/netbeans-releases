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
package org.netbeans.modules.form.j2ee.wizard;

import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.support.DatabaseExplorerUIs;
import org.netbeans.modules.form.j2ee.J2EEUtils;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

/**
 * Wizard panel for master information of master/detail wizard.
 *
 * @author Jan Stola
 */
public class MasterPanel implements WizardDescriptor.Panel {
    /** Determines whether we can proceed to the next panel. */
    private boolean valid;
    /** List of <code>ChangeListener</code> objects. */
    private EventListenerList listenerList;

    /**
     * Initializes GUI of this panel.
     */
    private void initGUI() {
        initComponents();
        initLists();
        DatabaseExplorerUIs.connect(connectionCombo, ConnectionManager.getDefault());
    }

    /**
     * Initializes lists of columns.
     */
    private void initLists() {
        availableList.setModel(new DefaultListModel());
        includeList.setModel(new DefaultListModel());
        ListDataListener listener = new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }
            public void contentsChanged(ListDataEvent e) {
                Object source = e.getSource();
                if (source == availableList.getModel()) {
                    addAllButton.setEnabled(availableList.getModel().getSize() != 0);
                } else if (source == includeList.getModel()) {
                    boolean empty = includeList.getModel().getSize() == 0;
                    removeAllButton.setEnabled(!empty);
                    setValid(!empty);
                }
            }
        };
        availableList.getModel().addListDataListener(listener);
        includeList.getModel().addListDataListener(listener);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        masterPanel = new javax.swing.JPanel();
        connectionCombo = new javax.swing.JComboBox();
        connectionLabel = new javax.swing.JLabel();
        tableLabel = new javax.swing.JLabel();
        tableCombo = new javax.swing.JComboBox();
        availableLabel = new javax.swing.JLabel();
        availablePane = new javax.swing.JScrollPane();
        availableList = new javax.swing.JList();
        addAllButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        removeAllButton = new javax.swing.JButton();
        includePane = new javax.swing.JScrollPane();
        includeList = new javax.swing.JList();
        upButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        includeLabel = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        masterPanel.setName(org.openide.util.NbBundle.getMessage(MasterPanel.class, "TITLE_MasterPanel")); // NOI18N

        connectionCombo.addActionListener(formListener);

        connectionLabel.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_DatabaseConnection")); // NOI18N

        tableLabel.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_DatabaseTable")); // NOI18N

        tableCombo.setEnabled(false);
        tableCombo.setRenderer(J2EEUtils.DBColumnInfo.getRenderer());
        tableCombo.addActionListener(formListener);

        availableLabel.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_AvailableColumns")); // NOI18N

        availableList.addListSelectionListener(formListener);
        availablePane.setViewportView(availableList);

        addAllButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterAddAll")); // NOI18N
        addAllButton.setEnabled(false);
        addAllButton.addActionListener(formListener);

        addButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterAdd")); // NOI18N
        addButton.setEnabled(false);
        addButton.addActionListener(formListener);

        removeButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterRemove")); // NOI18N
        removeButton.setEnabled(false);
        removeButton.addActionListener(formListener);

        removeAllButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterRemoveAll")); // NOI18N
        removeAllButton.setEnabled(false);
        removeAllButton.addActionListener(formListener);

        includeList.addListSelectionListener(formListener);
        includePane.setViewportView(includeList);

        upButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/form/j2ee/resources/up.gif")));
        upButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterUp")); // NOI18N
        upButton.setEnabled(false);
        upButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        upButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
        upButton.addActionListener(formListener);

        downButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/form/j2ee/resources/down.gif")));
        downButton.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_MasterDown")); // NOI18N
        downButton.setEnabled(false);
        downButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        downButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
        downButton.addActionListener(formListener);

        includeLabel.setText(org.openide.util.NbBundle.getMessage(MasterPanel.class, "LBL_ColumnsToInclude")); // NOI18N

        org.jdesktop.layout.GroupLayout masterPanelLayout = new org.jdesktop.layout.GroupLayout(masterPanel);
        masterPanel.setLayout(masterPanelLayout);
        masterPanelLayout.setHorizontalGroup(
            masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(masterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(masterPanelLayout.createSequentialGroup()
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(connectionLabel)
                            .add(tableLabel))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tableCombo, 0, 290, Short.MAX_VALUE)
                            .add(connectionCombo, 0, 290, Short.MAX_VALUE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, masterPanelLayout.createSequentialGroup()
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(availablePane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                            .add(availableLabel))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(addButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(addAllButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(removeButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(removeAllButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(masterPanelLayout.createSequentialGroup()
                                .add(includePane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(upButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(downButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .add(includeLabel))))
                .addContainerGap())
        );

        masterPanelLayout.linkSize(new java.awt.Component[] {addAllButton, addButton, downButton, removeAllButton, removeButton, upButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        masterPanelLayout.setVerticalGroup(
            masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(masterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(connectionLabel)
                    .add(connectionCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tableLabel)
                    .add(tableCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(10, 10, 10)
                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(availableLabel)
                    .add(includeLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(availablePane, 0, 140, Short.MAX_VALUE)
                    .add(includePane, 0, 140, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, masterPanelLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 15, Short.MAX_VALUE)
                        .add(addAllButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(addButton)
                            .add(upButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(masterPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(removeButton)
                            .add(downButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeAllButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 15, Short.MAX_VALUE)))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ListSelectionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == connectionCombo) {
                MasterPanel.this.connectionComboActionPerformed(evt);
            }
            else if (evt.getSource() == tableCombo) {
                MasterPanel.this.tableComboActionPerformed(evt);
            }
            else if (evt.getSource() == addAllButton) {
                MasterPanel.this.addAllButtonActionPerformed(evt);
            }
            else if (evt.getSource() == addButton) {
                MasterPanel.this.addButtonActionPerformed(evt);
            }
            else if (evt.getSource() == removeButton) {
                MasterPanel.this.removeButtonActionPerformed(evt);
            }
            else if (evt.getSource() == removeAllButton) {
                MasterPanel.this.removeAllButtonActionPerformed(evt);
            }
            else if (evt.getSource() == upButton) {
                MasterPanel.this.upButtonActionPerformed(evt);
            }
            else if (evt.getSource() == downButton) {
                MasterPanel.this.downButtonActionPerformed(evt);
            }
        }

        public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            if (evt.getSource() == availableList) {
                MasterPanel.this.availableListValueChanged(evt);
            }
            else if (evt.getSource() == includeList) {
                MasterPanel.this.includeListValueChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
        DefaultListModel model = (DefaultListModel)includeList.getModel();
        int index = includeList.getSelectedIndex();
        Object item = model.remove(index);
        model.add(index+1, item);
        includeList.setSelectedIndex(index+1);
    }//GEN-LAST:event_downButtonActionPerformed

    private void upButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upButtonActionPerformed
        DefaultListModel model = (DefaultListModel)includeList.getModel();
        int index = includeList.getSelectedIndex();
        Object item = model.remove(index);
        model.add(index-1, item);
        includeList.setSelectedIndex(index-1);
    }//GEN-LAST:event_upButtonActionPerformed

    private void removeAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllButtonActionPerformed
        moveListItems(includeList, availableList, false);
    }//GEN-LAST:event_removeAllButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        moveListItems(includeList, availableList, true);
    }//GEN-LAST:event_removeButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        moveListItems(availableList, includeList, true);
    }//GEN-LAST:event_addButtonActionPerformed

    private void addAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAllButtonActionPerformed
        moveListItems(availableList, includeList, false);
    }//GEN-LAST:event_addAllButtonActionPerformed

    private void includeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_includeListValueChanged
        int[] index = includeList.getSelectedIndices();
        boolean single = (index.length == 1);
        upButton.setEnabled(single && (index[0] != 0));
        downButton.setEnabled(single && (index[0] != includeList.getModel().getSize()-1));
        boolean any = (index.length > 0);
        removeButton.setEnabled(any);
        if (any) {
            availableList.clearSelection();
        }
    }//GEN-LAST:event_includeListValueChanged

    private void availableListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_availableListValueChanged
        boolean enabled = (availableList.getSelectedIndex() != -1);
        addButton.setEnabled(enabled);
        if (enabled) {
            includeList.clearSelection();
        }
    }//GEN-LAST:event_availableListValueChanged

    private void tableComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableComboActionPerformed
        J2EEUtils.DBColumnInfo table = getTable();
        DatabaseConnection connection = getConnection();
        Connection con = ((connection == null) || !table.isValid()) ? null : connection.getJDBCConnection();
        try {
            DefaultListModel model = (DefaultListModel)availableList.getModel();
            model.clear();
            model = (DefaultListModel)includeList.getModel();
            model.clear();
            if (con != null) {
                ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), connection.getSchema(), table.getName(), "%"); // NOI18N
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME"); // NOI18N
                    model.addElement(columnName);
                }
                rs.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_tableComboActionPerformed

    private void connectionComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionComboActionPerformed
        DatabaseConnection connection = getConnection();
        if (connection != null) {
            Connection con = J2EEUtils.establishConnection(connection);
            if (con == null) return; // User canceled the connection dialog
            fillTableCombo(connection);
        } else {
            fillTableCombo(null);
        }
    }//GEN-LAST:event_connectionComboActionPerformed

    /**
     * Returns selected database connection.
     *
     * @return selected database connection.
     */
    private DatabaseConnection getConnection() {
        Object selItem = connectionCombo.getSelectedItem();
        return (selItem instanceof DatabaseConnection) ? (DatabaseConnection)selItem : null;
    }

    /**
     * Returns selected database table.
     *
     * @return selected database table.
     */
    private J2EEUtils.DBColumnInfo getTable() {
        return (J2EEUtils.DBColumnInfo)tableCombo.getSelectedItem();
    }

    /**
     * Returns selected columns.
     *
     * @return selected columns.
     */
    private List getSelectedColumns() {
        DefaultListModel model = (DefaultListModel)includeList.getModel();
        return Arrays.asList(model.toArray());
    }

    /**
     * Fills the content of <code>tableCombo</code>.
     *
     * @param connection database connection.
     */
    private void fillTableCombo(DatabaseConnection connection) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        if (connection != null) {
            for (J2EEUtils.DBColumnInfo tableName : J2EEUtils.tableNamesForConnection(connection)) {
                model.addElement(tableName);
            }
        }
        tableCombo.setModel(model);
        tableCombo.setEnabled(tableCombo.getModel().getSize() != 0);
        tableCombo.setSelectedItem(tableCombo.getSelectedItem());
    }

    /**
     * Moves items of <code>fromList</code> into <code>toList</code>.
     *
     * @param fromList list to move the items from.
     * @param toList list to move the items to.
     * @param selected determines whether to move all items or just the selected ones.
     */
    private static void moveListItems(JList fromList, JList toList, boolean selected) {
        DefaultListModel fromModel = (DefaultListModel)fromList.getModel();
        DefaultListModel toModel = (DefaultListModel)toList.getModel();
        if (selected) {
            int[] index = fromList.getSelectedIndices();
            for (int i=0; i<index.length; i++) {
                Object item = fromModel.getElementAt(index[i]);
                toModel.addElement(item);
            }
            for (int i=index.length-1; i>=0; i--) {
                fromModel.removeElementAt(index[i]);
            }
        } else {
            Enumeration items = fromModel.elements();
            while (items.hasMoreElements()) {
                toModel.addElement(items.nextElement());
            }
            fromModel.clear();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAllButton;
    private javax.swing.JButton addButton;
    private javax.swing.JLabel availableLabel;
    private javax.swing.JList availableList;
    private javax.swing.JScrollPane availablePane;
    private javax.swing.JComboBox connectionCombo;
    private javax.swing.JLabel connectionLabel;
    private javax.swing.JButton downButton;
    private javax.swing.JLabel includeLabel;
    private javax.swing.JList includeList;
    private javax.swing.JScrollPane includePane;
    private javax.swing.JPanel masterPanel;
    private javax.swing.JButton removeAllButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JComboBox tableCombo;
    private javax.swing.JLabel tableLabel;
    private javax.swing.JButton upButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns component that represents this wizard panel.
     *
     * @return component that represents this wizard panel.
     */
    public Component getComponent() {
        if (masterPanel == null) {
            initGUI();
        }
        return masterPanel;
    }

    /**
     * Returns help context for this wizard panel.
     *
     * @return default help.
     */
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    public void readSettings(Object settings) {
        // not used
    }

    /**
     * Stores settings of this panel.
     *
     * @param settings wizard descriptor to store the settings in.
     */
    public void storeSettings(Object settings) {
        WizardDescriptor wizard = (WizardDescriptor) settings;
        wizard.putProperty("connection", getConnection()); // NOI18N
        wizard.putProperty("master", getTable().getName()); // NOI18N
        wizard.putProperty("masterColumns", getSelectedColumns()); // NOI18N
    }
    
    /**
     * Determines whether we can move to the next panel.
     *
     * @return <code>true</code> if we can proceed to the next
     * panel, returns <code>false</code> otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the valid property.
     *
     * @param valid new value of the valid property.
     */
    void setValid(boolean valid) {
        if (valid == this.valid) return;
        this.valid = valid;
        fireStateChanged();
    }

    /**
     * Adds change listener to this wizard panel.
     *
     * @param listener listener to add.
     */
    public void addChangeListener(ChangeListener listener) {
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Removes change listener from this wizard panel.
     *
     * @param listener listener to remove.
     */
    public void removeChangeListener(ChangeListener listener) {
        if (listenerList != null) {
            listenerList.remove(ChangeListener.class, listener);
        }
    }

    /**
     * Fires change of the valid property.
     */
    private void fireStateChanged() {
        if (listenerList == null) return;

        ChangeEvent e = null;
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        for (int i=listeners.length-1; i>=0; i--) {
            if (e == null) {
                e = new ChangeEvent(this);
            }
            listeners[i].stateChanged(e);
        }
    }
    
}
