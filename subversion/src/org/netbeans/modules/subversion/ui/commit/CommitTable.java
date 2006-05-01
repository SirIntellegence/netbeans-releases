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

package org.netbeans.modules.subversion.ui.commit;

import org.netbeans.modules.versioning.util.FilePathCellRenderer;
import org.netbeans.modules.subversion.util.TableSorter;
import org.netbeans.modules.subversion.util.SvnUtils;
import org.netbeans.modules.subversion.SvnFileNode;
import org.netbeans.modules.subversion.FileInformation;
import org.netbeans.modules.subversion.Subversion;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.util.*;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * {@link #getComponent Table} that displays nodes in the commit dialog.
 * 
 * @author Maros Sandor
 */
public class CommitTable implements AncestorListener, TableModelListener {

    public static String [] COMMIT_COLUMNS = new String [] {
                                            CommitTableModel.COLUMN_NAME_NAME,
                                            CommitTableModel.COLUMN_NAME_STATUS,
                                            CommitTableModel.COLUMN_NAME_ACTION,
                                            CommitTableModel.COLUMN_NAME_PATH
                                        };

    public static String [] IMPORT_COLUMNS = new String [] {
                                            CommitTableModel.COLUMN_NAME_NAME,                                            
                                            CommitTableModel.COLUMN_NAME_ACTION,
                                            CommitTableModel.COLUMN_NAME_PATH
                                        };
    
    private CommitTableModel    tableModel;
    private JTable              table;
    private JComponent          component;
    
    private TableSorter sorter;
    private String[]    columns;

    public CommitTable(JLabel label, String[] columns) {
        tableModel = new CommitTableModel(columns);
        tableModel.addTableModelListener(this);
        sorter = new TableSorter(tableModel);
        table = new JTable(sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(String.class, new CommitStringsCellRenderer());
        table.setDefaultEditor(CommitOptions.class, new CommitOptionsCellEditor());
        table.getTableHeader().setReorderingAllowed(true);
        sorter.setTableHeader(table.getTableHeader());
        table.setRowHeight(table.getRowHeight() * 6 / 5);
        table.addAncestorListener(this);
        component = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        label.setLabelFor(table);
        table.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CommitTable.class, "ACSD_CommitTable"));
    }

    public void ancestorAdded(AncestorEvent event) {
        setDefaultColumnSizes();
    }

    /**
     * Sets sizes of Commit table columns, kind of hardcoded.
     */ 
    private void setDefaultColumnSizes() {
        int width = table.getWidth();
        TableColumnModel columnModel = table.getColumnModel();
        if (columns == null || columnModel == null) return; // unsure when this methed will be called (component realization) 
        if (columnModel.getColumnCount() != columns.length) return; 
        if (columns.length == 4) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                sorter.setSortingStatus(i, TableSorter.NOT_SORTED);
                if (col.equals(CommitTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_STATUS)) {
                    sorter.setColumnComparator(i, new StatusComparator());
                    sorter.setSortingStatus(i, TableSorter.ASCENDING);
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_ACTION)) {
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 40 / 100);
                }
            }
        } else if (columns.length == 5) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                sorter.setSortingStatus(i, TableSorter.NOT_SORTED);
                if (col.equals(CommitTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 25 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_STATUS)) {
                    sorter.setColumnComparator(i, new StatusComparator());
                    sorter.setSortingStatus(i, TableSorter.ASCENDING);
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_ACTION)) {
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                }
            }
        }
    }

    public void ancestorMoved(AncestorEvent event) {
    }

    public void ancestorRemoved(AncestorEvent event) {
    }
    
    void setColumns(String[] cols) {
        if (Arrays.equals(columns, cols)) return;
        columns = cols;
        tableModel.setColumns(cols);
        setDefaultColumnSizes();
    }

    public void setNodes(SvnFileNode[] nodes) {
        tableModel.setNodes(nodes);
    }

    /**
     * @return Map&lt;SvnFileNode, CommitOptions>
     */
    public Map getCommitFiles() {
        return tableModel.getCommitFiles();
    }

    /**
     * @return table in a scrollpane 
     */
    public JComponent getComponent() {
        return component;
    }

    void dataChanged() {
        int idx = table.getSelectedRow();
        tableModel.fireTableDataChanged();
        if (idx != -1) table.getSelectionModel().addSelectionInterval(idx, idx);
    }

    TableModel getTableModel() {
        return tableModel;
    }

    public void tableChanged(TableModelEvent e) {
        // change in commit options may alter name rendering (strikethrough)
        table.repaint();
    }

    public void setRootFile(String repositoryPath, String rootLocalPath) {
        tableModel.setRootFile(repositoryPath, rootLocalPath);
    }
    
    private class CommitOptionsCellEditor extends DefaultCellEditor {

        private final Object[] dirAddOptions = new Object [] {
                CommitOptions.ADD_DIRECTORY,
                CommitOptions.EXCLUDE
            };
        
        private final Object[] addOptions = new Object [] {
                CommitOptions.ADD_TEXT,
                CommitOptions.ADD_BINARY,
                CommitOptions.EXCLUDE
            };
        private final Object[] commitOptions = new Object [] {
                CommitOptions.COMMIT,
                CommitOptions.EXCLUDE
            };

        private final Object[] removeOptions = new Object [] {
                CommitOptions.COMMIT_REMOVE,
                CommitOptions.EXCLUDE
            };

        public CommitOptionsCellEditor() {
            super(new JComboBox());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            FileInformation info = tableModel.getNode(sorter.modelIndex(row)).getInformation();
            int fileStatus = info.getStatus();
            JComboBox combo = (JComboBox) editorComponent;
            if (fileStatus == FileInformation.STATUS_VERSIONED_DELETEDLOCALLY || fileStatus == FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY) {
                combo.setModel(new DefaultComboBoxModel(removeOptions));
            } else if ((fileStatus & FileInformation.STATUS_IN_REPOSITORY) == 0) {
                if (info.isDirectory()) {
                    combo.setModel(new DefaultComboBoxModel(dirAddOptions));
                } else {
                    combo.setModel(new DefaultComboBoxModel(addOptions));
                }
            } else {
                combo.setModel(new DefaultComboBoxModel(commitOptions));
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    private static class CommitStringsCellRenderer extends DefaultTableCellRenderer {

        private FilePathCellRenderer pathRenderer = new FilePathCellRenderer();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int col = table.convertColumnIndexToModel(column);
            if (col == 0) {
                TableSorter sorter = (TableSorter) table.getModel();
                CommitTableModel model = (CommitTableModel) sorter.getTableModel();
                SvnFileNode node = model.getNode(sorter.modelIndex(row));
                CommitOptions options = model.getOptions(sorter.modelIndex(row));
                if (!isSelected) {
                    value = "<html>" + Subversion.getInstance().getAnnotator().annotateNameHtml(  // NOI18N
                            node.getFile().getName(), node.getInformation(), null);
                }
                if (options == CommitOptions.EXCLUDE) {
                    value = "<html><s>" + value + "</s></html>"; // NOI18N
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (col == 3) {
                return pathRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    }
    
    private class StatusComparator extends SvnUtils.ByImportanceComparator {
        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return super.compare(tableModel.getNode(row1.intValue()).getInformation(),
                                 tableModel.getNode(row2.intValue()).getInformation());
        }
    }
    
    private class FileNameComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return tableModel.getNode(row1.intValue()).getName().compareToIgnoreCase(
                    tableModel.getNode(row2.intValue()).getName());
        }
    }


}
